package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DwpDataMigrationServiceImplTest {

    private final DwpDataMigrationServiceImpl dwpDataMigrationService = new DwpDataMigrationServiceImpl();

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1234L)
            .build();
        assertTrue(dwpDataMigrationService.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(dwpDataMigrationService.accepts().test(null));
    }

    @Test
    public void shouldReturnPassedDataWhenMigrateCalled() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> result = dwpDataMigrationService.migrate(data);
        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    public void shouldNotChangeExistingFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("poAttendanceConfirmed", "Yes");
        data.put("dwpIsOfficerAttending", "Yes");
        data.put("tribunalDirectPoToAttend", "Yes");
        Map<String, Object> result = dwpDataMigrationService.migrate(data);
        assertNotNull(result);
        assertEquals("Yes", data.get("poAttendanceConfirmed"));
        assertEquals("Yes", data.get("dwpIsOfficerAttending"));
        assertEquals("Yes", data.get("tribunalDirectPoToAttend"));
    }

    @Test
    public void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = dwpDataMigrationService.migrate(null);
        assertNull(result);
    }

    @Test
    public void shouldReturnCorrectValuesForDwpMigration() {
        assertEquals("dwpCaseMigration", dwpDataMigrationService.getEventId());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventDescription());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventSummary());
    }
}
