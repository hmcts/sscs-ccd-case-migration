package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hmctsDwpStateMigration.enabled", havingValue = "true")
public class HmctsDwpStateMigrationImpl extends CaseMigrationProcessor {

    static final String EVENT_ID = "clearExpiredFilters";
    static final String EVENT_SUMMARY = "Cleared expired filters";
    static final String EVENT_DESCRIPTION = "Cleared expired filters";

    private static final String HMCTS_DWP_STATE = "hmctsDwpState";

    private final CaseLoader caseLoader;

    public HmctsDwpStateMigrationImpl(@Value("${migration.hmctsDwpStateMigration.encoded-data-string}")
                                      String encodedDataString) {
        this.caseLoader = new CaseLoader(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            if (data.get(HMCTS_DWP_STATE) == null
                || !data.get(HMCTS_DWP_STATE).toString().equalsIgnoreCase("failedSendingFurtherEvidence")) {
                log.info("Skipping case for hmctsDwpState migration. Case id: {} Reason: hmctsDwpState is not"
                             + " 'failedSendingFurtherEvidence' it is {}",
                         caseId, data.get(HMCTS_DWP_STATE));
                throw new RuntimeException("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                        + " 'failedSendingFurtherEvidence'");
            } else {
                log.info("case {} has hmctsDwpState as failedSendingFurtherEvidence. "
                             + "Removing it and setting it to null", caseId);
                data.put(HMCTS_DWP_STATE, null);
            }
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return caseLoader.findCases();
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
