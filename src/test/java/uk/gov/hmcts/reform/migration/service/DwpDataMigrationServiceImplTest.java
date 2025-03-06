package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DwpDataMigrationServiceImplTest {

    private DwpDataMigrationServiceImpl dwpDataMigrationService;

    private CaseDetails caseDetails;

    @BeforeEach
    public void setup() {
        dwpDataMigrationService =
            new DwpDataMigrationServiceImpl(null,null, null);
        caseDetails = CaseDetails.builder().id(1234L).build();
    }

    @Test
    void shouldReturnTrueForCaseDetailsPassed() {
        assertTrue(dwpDataMigrationService.accepts().test(caseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(dwpDataMigrationService.accepts().test(null));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        Map<String, Object> data = new HashMap<>();
        caseDetails.setData(data);

        Map<String, Object> result = dwpDataMigrationService.migrate(caseDetails);

        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    void shouldNotChangeExistingFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("poAttendanceConfirmed", "Yes");
        data.put("dwpIsOfficerAttending", "Yes");
        data.put("tribunalDirectPoToAttend", "Yes");
        caseDetails.setData(data);

        Map<String, Object> result = dwpDataMigrationService.migrate(caseDetails);

        assertNotNull(result);
        assertEquals("Yes", data.get("poAttendanceConfirmed"));
        assertEquals("Yes", data.get("dwpIsOfficerAttending"));
        assertEquals("Yes", data.get("tribunalDirectPoToAttend"));
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = dwpDataMigrationService.migrate(caseDetails);

        assertNull(result);
    }

    @Test
    void shouldReturnCorrectValuesForDwpMigration() {
        assertEquals("dwpCaseMigration", dwpDataMigrationService.getEventId());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventDescription());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventSummary());
    }
}
