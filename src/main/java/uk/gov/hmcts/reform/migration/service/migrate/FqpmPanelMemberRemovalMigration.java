package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

import java.util.Collections;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
public class FqpmPanelMemberRemovalMigration extends CaseMigrationProcessor {
    static final String EVENT_ID = "clearFqpmPanelMember";
    static final String EVENT_SUMMARY = "Cleared FQPM panel member";
    static final String EVENT_DESCRIPTION = "Cleared FQPM panel member";
    private final String encodedDataString;

    public FqpmPanelMemberRemovalMigration(@Value("${migration.fqpmPanelMigration.encoded-data-string}") String encodedDataString) {
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
            var panelMemberComposition = caseData.getPanelMemberComposition();
            if (nonNull(panelMemberComposition)) {
                var panelMemberFqpm = panelMemberComposition.getPanelCompositionDisabilityAndFqMember();
                if (panelMemberFqpm.isEmpty()) {
                    log.info("Skipping case for clearFqpmPanelMember migration. Case id: {} Reason: fqpm is not"
                                 + " 'present on the case",
                             caseId);
                    throw new RuntimeException("Skipping case for clearFqpmPanelMember migration. Reason: clearFqpmPanelMember is not"
                                                   + " 'present on the case'");

                } else {
                    panelMemberComposition.setPanelCompositionDisabilityAndFqMember(Collections.emptyList());
                }
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
