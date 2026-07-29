package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.Party;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DRAFT_ARCHIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VOID_STATE;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.confidentialityFlag.enabled", havingValue = "true")
public class ConfidentialityFlagMigration extends CaseMigrationProcessor {

    static final String CONFIDENTIALITY_FLAG_MIGRATION_EVENT_ID = "migrateCase";
    static final String CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY
        = "Appeal migrated following confidentiality release";
    static final String CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION = "Appeal migrated following confidentiality release";
    static final String STATE_FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect state: (%s)";
    static final String DATE_FAILURE_MESSAGE
        = "Skipping Case (%s) for migration due to appeal being dormant for over 6 months.";
    static final String NO_CONFIDENTIALITY_MESSAGE
        = "Skipping Case (%s) for migration due to no confidentiality fields.";
    static final LocalDateTime dormantCutOffDate = now(systemDefault()).minusMonths(6);
    private static final String CONFIDENTIALITY_REQUIREMENT = "confidentialityRequirement";


    private final String encodedDataString;

    public ConfidentialityFlagMigration(@Value("${migration.confidentialityFlag.encoded-data-string}")
                                 String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        Long caseId = caseDetails.getId();
        final Map<String, Object> data = caseDetails.getData();

        if (Objects.equals(caseDetails.getState(), VOID_STATE.toString())
            || Objects.equals(caseDetails.getState(), DRAFT_ARCHIVED.toString())) {
            String skipMsg = format(STATE_FAILURE_MSG, caseId, caseDetails.getState());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        } else if (!isChildSupport(data) && Objects.equals(caseDetails.getState(), DORMANT_APPEAL_STATE.toString())
            && dormantCutOffDate.isAfter(caseDetails.getLastModified())) {
            String skipMsg = format(DATE_FAILURE_MESSAGE, caseId);
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }

        boolean appellantUpdated = updateAppellant(data, caseId);
        boolean otherPartiesUpdated = updateOtherParties(data, caseId);

        if (!appellantUpdated && !otherPartiesUpdated) {
            String skipMsg = format(NO_CONFIDENTIALITY_MESSAGE, caseDetails.getId());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        var caseData = convertToSscsCaseData(data);
        YesNo undeterminedConfidentiality = caseData.hasUndeterminedPartyConfidentiality();
        String confidentialityTab = caseData.getConfidentialityTab();
        YesNo isConfidentialValue = isConfidential(caseData);
        if (nonNull(isConfidentialValue)) {
            data.put("isConfidentialCase", isConfidentialValue.getValue());
        }
        if (nonNull(undeterminedConfidentiality)) {
            data.put("hasUndeterminedPartyConfidentiality", undeterminedConfidentiality.getValue());
        }
        if (nonNull(confidentialityTab)) {
            data.put("confidentialityTab", confidentialityTab);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    private YesNo isConfidential(SscsCaseData caseData) {
        Optional<YesNo> appellantConfidentiality = caseData.getAppellant().map(Party::getConfidentialityRequired);
        List<YesNo> otherPartiesConfidentiality = Optional.ofNullable(caseData.getOtherParties())
            .orElse(emptyList())
            .stream()
            .map(op -> op.getValue().getConfidentialityRequired())
            .toList();

        if (appellantConfidentiality.filter(YesNo.YES::equals).isPresent()
            || otherPartiesConfidentiality.contains(YesNo.YES)) {
            return YesNo.YES;
        }

        if (appellantConfidentiality.filter(YesNo.NO::equals).isPresent()
            && otherPartiesConfidentiality.stream().allMatch(YesNo.NO::equals)) {
            return YesNo.NO;
        }

        return null;
    }

    private Boolean updateOtherParties(Map<String, Object> data, Long caseId) {
        boolean updateOtherParties = false;
        if (data.containsKey("otherParties")) {
            List<Map<String, Object>> otherParties = (List<Map<String, Object>>) data.get("otherParties");
            for (Map<String, Object> op : otherParties) {
                if (op.containsKey("value") && updateOtherParty((Map<String, Object>) op.get("value"), caseId)) {
                    log.info("Updating other party confidentiality for case {}", caseId);
                    updateOtherParties = true;
                }
            }
        }
        return updateOtherParties;
    }

    private Boolean updateOtherParty(Map<String, Object> op, Long caseId) {
        Boolean otherPartyUpdated = false;
        Object confidentialityRequired = op.get("confidentialityRequired");
        if (nonNull(confidentialityRequired)) {
            if (!op.containsKey(CONFIDENTIALITY_REQUIREMENT)) {
                op.put(CONFIDENTIALITY_REQUIREMENT, confidentialityRequired.toString());
                return true;
            } else {
                log.info("New confidentiality field is present for other party on case {}", caseId);
                return false;
            }
        }
        return otherPartyUpdated;
    }


    private boolean updateAppellant(Map<String, Object> data, Long caseId) {
        Map<String, Object> appeal = (Map<String, Object>) data.get("appeal");
        if (nonNull(appeal)) {
            Map<String, Object> appellant = (Map<String, Object>) appeal.get("appellant");
            if (nonNull(appellant)) {
                Object confidentialityRequired = appellant.get("confidentialityRequired");
                if (nonNull(confidentialityRequired)) {
                    if (!appellant.containsKey(CONFIDENTIALITY_REQUIREMENT)) {
                        appellant.put(CONFIDENTIALITY_REQUIREMENT, confidentialityRequired.toString());
                        log.info("Updating Appellant confidentiality for case {}", caseId);
                        return true;
                    } else {
                        log.info("New confidentiality field for appellant is present on case {}", caseId);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private boolean isChildSupport(Map<String, Object> data) {
        String code = getNestedValue(data, "appeal", "benefitType", "code");
        return Benefit.CHILD_SUPPORT.getShortName().equalsIgnoreCase(code);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getNestedValue(Map<String, Object> data, String... keys) {
        Object current = data;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return (T) current;
    }

    @Override
    public String getEventId() {
        return CONFIDENTIALITY_FLAG_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY;
    }

}
