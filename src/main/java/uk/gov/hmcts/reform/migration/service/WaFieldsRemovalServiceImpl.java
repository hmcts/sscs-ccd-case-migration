package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.waFieldsRemoval.enabled", havingValue = "true")
public class WaFieldsRemovalServiceImpl  implements DataMigrationService<Map<String, Object>> {
    static final String EVENT_ID = "waCaseMigration";
    static final String EVENT_SUMMARY = "Remove WA fields with incorrect data type";
    static final String EVENT_DESCRIPTION = "Remove WA fields (scannedDocumentTypes, assignedCaseRoles, "
        + "previouslyAssignedCaseRoles) with incorrect data type";

    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    public Map<String, Object> migrate(Map<String, Object> data) {
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
