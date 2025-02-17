package uk.gov.hmcts.reform.migration.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;

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

    public Map<String, Object> migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            if (data.containsKey("scannedDocumentTypes")) {
                log.info("Scanned document types found {}", data.get("scannedDocumentTypes"));
                data.put("scannedDocumentTypes", null);
            }
            if (data.containsKey("assignedCaseRoles")) {
                log.info("Assigned case roles found {}", data.get("assignedCaseRoles"));
                data.put("assignedCaseRoles", null);
            }
            if (data.containsKey("previouslyAssignedCaseRoles")) {
                log.info("Previously assigned case roles found {}", data.get("previouslyAssignedCaseRoles"));
                data.put("previouslyAssignedCaseRoles", null);
            }
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
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
