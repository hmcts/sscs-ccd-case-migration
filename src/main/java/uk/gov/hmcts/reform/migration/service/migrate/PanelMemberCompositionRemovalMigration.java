package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
public class PanelMemberCompositionRemovalMigration extends CaseMigrationProcessor {
    static final String EVENT_ID = "migrateCase";
    static final String EVENT_SUMMARY = "Clear panel member composition";
    static final String EVENT_DESCRIPTION = "Cleared panel member composition";
    private final String encodedDataString;

    public PanelMemberCompositionRemovalMigration(
        @Value("${migration.panelMemberCompositionMigration.encoded-data-string}") String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            var caseData = convertToSscsCaseData(caseDetails.getData());
            if (nonNull(caseData.getPanelMemberComposition()) && !caseData.getPanelMemberComposition().isEmpty()) {
                log.info("Setting Panel Member Composition to null for case id {}", caseId);
                data.put("panelMemberComposition", null);
            } else {
                log.info("Skipping case for clearPanelMemberComposition migration. Case id: {} "
                             + "Reason: Panel member composition is null or empty", caseId);
                throw new RuntimeException("Skipping case for clearPanelMemberComposition migration. "
                                               + "Reason: Panel member composition is null or empty");
            }
        }
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

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
