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
@ConditionalOnProperty(value = "migration.sscs12552.enabled", havingValue = "true")
public class WaFieldsRemovalServiceImpl  implements DataMigrationService<Map<String, Object>> {
    private static final String EVENT_ID = "waCaseMigration";
    private static final String EVENT_SUMMARY = "Migrate case for WA";
    private static final String EVENT_DESCRIPTION = "Migrate case for WA";

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
