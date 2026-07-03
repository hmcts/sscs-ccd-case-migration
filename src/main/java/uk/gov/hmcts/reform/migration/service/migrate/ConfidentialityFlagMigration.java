package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.isNull;
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
        = "Confidentiality fields migrated successfully";
    static final String CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION = "";
    static final String STATE_FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect state: (%s)";
    static final String DATE_FAILURE_MESSAGE
        = "Skipping Case (%s) for migration due to appeal being dormant for over 6 months.";
    static final String NO_CONFIDENTIALITY_MESSAGE
        = "Skipping Case (%s) for migration due to no confidentiality fields.";
    static final LocalDateTime dormantCutOffDate = LocalDateTime.now().minusMonths(6);


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
        if (Objects.equals(caseDetails.getState(), VOID_STATE.toString())
            || Objects.equals(caseDetails.getState(), DRAFT_ARCHIVED.toString())) {
            String skipMsg = format(STATE_FAILURE_MSG, caseId, caseDetails.getState());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        } else if (Objects.equals(caseDetails.getState(), DORMANT_APPEAL_STATE.toString())
            && dormantCutOffDate.isAfter(caseDetails.getLastModified())) {
            String skipMsg = format(DATE_FAILURE_MESSAGE, caseId);
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);

        }
        Map<String, Object> data = caseDetails.getData();
        Boolean appellantUpdated = updateAppeallant(data, caseId);
        Boolean otherPartiesUpdated = updateOtherParties(data, caseId);
        Boolean hasMissingConfidentiality = checkForMissingConfidentiality(data);

        if (hasMissingConfidentiality) {
            log.info("Setting hasUndeterminedPartyConfidentiality for case {}", caseId);
            data.put("hasUndeterminedPartyConfidentiality", "Yes");
        }

        if (!appellantUpdated && !otherPartiesUpdated && !hasMissingConfidentiality) {
            String skipMsg = format(NO_CONFIDENTIALITY_MESSAGE, caseDetails.getId());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    private Boolean checkForMissingConfidentiality(Map<String, Object> data) {
        Boolean confidentialityMissing = false;
        Map<String, Object> appeal = (Map<String, Object>) data.get("appeal");
        if (nonNull(appeal)) {
            Map<String, Object> appellant = (Map<String, Object>) appeal.get("appellant");
            if (nonNull(appellant)) {
                Object confidentialityRequired = appellant.get("confidentialityRequired");
                if (isNull(confidentialityRequired)) {
                    confidentialityMissing = true;
                }
            }
        }
        if (data.containsKey("otherParties")) {
            List<Map<String, Object>> otherParties = (List<Map<String, Object>>) data.get("otherParties");
            for (Map<String, Object> op : otherParties) {
                Map<String, Object> value = (Map<String, Object>) op.get("value");
                if (nonNull(value) && isNull(value.get("confidentialityRequired"))) {
                    confidentialityMissing = true;
                }
            }
        }
        return confidentialityMissing;
    }

    private Boolean updateOtherParties(Map<String, Object> data, Long caseId) {
        Boolean updateOtherParties = false;
        if (data.containsKey("otherParties")) {
            List<Map<String, Object>> otherParties = (List<Map<String, Object>>) data.get("otherParties");
            for (Map<String, Object> op : otherParties) {
                if (op.containsKey("value") && updateOtherParty((Map<String, Object>) op.get("value"))) {
                    log.info("Updating other party confidentiality for case {}", caseId);
                    updateOtherParties = true;
                }
            }
        }
        return updateOtherParties;
    }

    private Boolean updateOtherParty(Map<String, Object> op) {
        Boolean otherPartyUpdated = false;
        Object confidentialityRequired = op.get("confidentialityRequired");
        if (nonNull(confidentialityRequired)) {
            op.put("confidentialityRequirement", confidentialityRequired.toString());
            op.remove("confidentialityRequired");
            otherPartyUpdated = true;
        }
        return otherPartyUpdated;
    }


    private Boolean updateAppeallant(Map<String, Object> data, Long caseId) {
        Boolean appellantUpdated = false;
        Map<String, Object> appeal = (Map<String, Object>) data.get("appeal");
        if (nonNull(appeal)) {
            Map<String, Object> appellant = (Map<String, Object>) appeal.get("appellant");
            if (nonNull(appellant)) {
                Object confidentialityRequired = appellant.get("confidentialityRequired");
                if (nonNull(confidentialityRequired)) {
                    appellant.put("confidentialityRequirement", confidentialityRequired.toString());
                    appellant.remove("confidentialityRequired");
                    log.info("Updating Appellant confidentiality for case {}", caseId);
                    appellantUpdated = true;
                }
            }
        }
        return appellantUpdated;
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
