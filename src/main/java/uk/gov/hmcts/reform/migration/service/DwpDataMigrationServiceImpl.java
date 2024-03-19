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
@ConditionalOnProperty(value = "migration.dwp-enhancements.enabled", havingValue = "true")
public class DwpDataMigrationServiceImpl implements DataMigrationService<Map<String, Object>> {

    private static final String EVENT_ID = "dwpCaseMigration";
    private static final String EVENT_SUMMARY = "Migrate case for DWP Enhancements";
    private static final String EVENT_DESCRIPTION = "Migrate case for DWP Enhancements";

    @Override
    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> data) {
        if (nonNull(data)) {
            if (!data.containsKey("poAttendanceConfirmed")) {
                data.put("poAttendanceConfirmed", "No");
            }
            if (!data.containsKey("dwpIsOfficerAttending")) {
                data.put("dwpIsOfficerAttending", "No");
            }
            if (!data.containsKey("tribunalDirectPoToAttend")) {
                data.put("tribunalDirectPoToAttend", "No");
            }
        }
        return data;
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
