package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.waFieldsRemoval.enabled", havingValue = "true")
public class WaFieldsRemovalServiceImpl extends CaseMigrationProcessor {

    static final String EVENT_ID = "waCaseMigration";
    static final String EVENT_SUMMARY = "Remove WA fields with incorrect data type";
    static final String EVENT_DESCRIPTION = "Remove WA fields (scannedDocumentTypes, assignedCaseRoles, "
        + "previouslyAssignedCaseRoles) with incorrect data type";

    private final String encodedDataString;

    public WaFieldsRemovalServiceImpl(@Value("${migration.waFieldsRemoval.encoded-data-string}")
                                      String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public void migrate(SscsCaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data) && nonNull(data.getWorkAllocationFields())) {
            var waFields = data.getWorkAllocationFields();
            if (nonNull(waFields.getScannedDocumentTypes())) {
                log.info("Scanned document types found {}", waFields.getScannedDocumentTypes());
                waFields.setScannedDocumentTypes(null);
            }
            if (nonNull(waFields.getAssignedCaseRoles())) {
                log.info("Assigned case roles found {}", waFields.getAssignedCaseRoles());
                waFields.setAssignedCaseRoles(null);
            }
            if (nonNull(waFields.getPreviouslyAssignedCaseRoles())) {
                log.info("Previously assigned case roles found {}", waFields.getPreviouslyAssignedCaseRoles());
                waFields.setPreviouslyAssignedCaseRoles(null);
            }
        }
    }

    @Override
    public List<SscsCaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    public String getEventId() {
        return EVENT_ID;
    }

    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
