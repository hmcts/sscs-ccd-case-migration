package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VOID_STATE;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.confidentialityFlag.enabled", havingValue = "true")
public class ConfidentialityFlagMigration extends CaseMigrationProcessor {

    static final String CONFIDENTIALITY_FLAG_MIGRATION_EVENT_ID = "caseMigrated";
    static final String CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY
        = "Confidentiality fields migrated successfully";
    static final String CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION = "";
    static final String STATE_FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect state: (%s)";
    static final String DATE_FAILURE_MESSAGE = "Skipping Case (%s) for migration due to appeal being dormant for over 6 months.";
    static final String NO_CONFIDENTIALITY_MESSAGE = "Skipping Case (%s) for migration due to no confidentiality fields.";
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
        if (!Objects.equals(caseDetails.getState(), VOID_STATE.toString())) {
            String skipMsg = format(STATE_FAILURE_MSG, caseDetails.getId(), caseDetails.getState());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        } else if (Objects.equals(caseDetails.getState(), DORMANT_APPEAL_STATE.toString())
            && dormantCutOffDate.isAfter(caseDetails.getLastModified())) {
            String skipMsg = format(DATE_FAILURE_MESSAGE, caseDetails.getId());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);

        }
        SscsCaseData sscsCaseData = convertToSscsCaseData(caseDetails.getData());
        YesNo isConfidentialCase = sscsCaseData.getIsConfidentialCase();
        YesNo appellantConfidentialityRequired = sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired();
        List<CcdValue<OtherParty>> otherPartiesConfidential =  sscsCaseData.getOtherParties().stream()
            .filter(otherParty -> nonNull(otherParty.getValue().getConfidentialityRequired())).toList();

        if (isNull(isConfidentialCase) && isNull(appellantConfidentialityRequired)
            && otherPartiesConfidential.isEmpty()) {
            String skipMsg = format(NO_CONFIDENTIALITY_MESSAGE, caseDetails.getId());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);

        }

        if (!otherPartiesConfidential.isEmpty()) {
            List<CcdValue<OtherParty>> otherPartiesUpdated = sscsCaseData.getOtherParties().stream().map(party -> {
                if (nonNull(party.getValue().getConfidentialityRequired())) {
                    party.getValue().setConfidentialityRequired(null);
                }
                return party;
            }).toList();
            sscsCaseData.setOtherParties(otherPartiesUpdated);
        }
        if (nonNull(isConfidentialCase)) {
            sscsCaseData.setIsConfidentialCase(null);
        }
        if (nonNull(appellantConfidentialityRequired)) {
            sscsCaseData.getAppeal().getAppellant().setConfidentialityRequired(null);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());

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
