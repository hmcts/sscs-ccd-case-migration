package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hmctsDwpStateMigration.enabled", havingValue = "true")
public class HmctsDwpStateMigrationImpl extends CaseMigrationProcessor {

    static final String EVENT_ID = "clearExpiredFilters";
    static final String EVENT_SUMMARY = "Cleared expired filters";
    static final String EVENT_DESCRIPTION = "Cleared expired filters";

    private final String encodedDataString;

    public HmctsDwpStateMigrationImpl(@Value("${migration.hmctsDwpStateMigration.encoded-data-string}")
                                      String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public void migrate(SscsCaseDetails caseDetails) {
        var caseData = caseDetails.getData();
        if (nonNull(caseData)) {
            String caseId = caseDetails.getId().toString();

            if (caseData.getHmctsDwpState() == null
                || !caseData.getHmctsDwpState().equalsIgnoreCase("failedSendingFurtherEvidence")) {
                log.info("Skipping case for hmctsDwpState migration. Case id: {} Reason: hmctsDwpState is not"
                             + " 'failedSendingFurtherEvidence' it is {}",
                         caseId, caseData.getHmctsDwpState());
                throw new RuntimeException("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                        + " 'failedSendingFurtherEvidence'");
            } else {
                log.info("case {} has hmctsDwpState as failedSendingFurtherEvidence. "
                             + "Removing it and setting it to null", caseId);
                caseData.setHmctsDwpState(null);
            }
        }
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    @Override
    public String getEventId() {
        return EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
