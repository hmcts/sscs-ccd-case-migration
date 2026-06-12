package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INCOMPLETE_APPLICATION;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.awaitingConfidentialityReq.enabled", havingValue = "true")
public class AwaitingConfidentialityReqMigration extends CaseMigrationProcessor {

    static final String AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_ID = "adminSendToAwaitConfidentialityRequirements";
    static final String AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_SUMMARY
        = "Case state moved to Awaiting confidentiality requirements";
    static final String AWAITING_CONFIDENTIALITY_EVENT_DESCRIPTION = "";
    static final String STATE_FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect state: (%s)";
    static final String BENEFIT_FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect benefit type: (%s)";
    static final String OTHER_PARTY_FAILURE_MSG = "Skipping Case (%s) for migration due to invalid other party data";

    private final String encodedDataString;

    public AwaitingConfidentialityReqMigration(@Value("${migration.awaitingConfidentialityReq.encoded-data-string}")
                                 String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        if (!Objects.equals(caseDetails.getState(), INCOMPLETE_APPLICATION.toString())) {
            String skipMsg = format(STATE_FAILURE_MSG, caseDetails.getId(), caseDetails.getState());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        SscsCaseData sscsCaseData = convertToSscsCaseData(caseDetails.getData());
        if (!sscsCaseData.isBenefitType(Benefit.CHILD_SUPPORT)) {
            String skipMsg = format(BENEFIT_FAILURE_MSG, caseDetails.getId(),
                                    sscsCaseData.getBenefitType().orElse(null));
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        if (!isNotEmpty(sscsCaseData.getOtherParties())) {
            String skipMsg = format(OTHER_PARTY_FAILURE_MSG, caseDetails.getId());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());

    }

    @Override
    public String getEventId() {
        return AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return AWAITING_CONFIDENTIALITY_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_SUMMARY;
    }

}
