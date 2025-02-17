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

class WaDataMigrationServiceImplTest {

    private final WaDataMigrationServiceImpl waDataMigrationService =
        new WaDataMigrationServiceImpl(null, null);

    private final CaseDetails caseDetails = CaseDetails.builder()
        .id(1234L)
        .build();

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertTrue(waDataMigrationService.accepts().test(caseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(waDataMigrationService.accepts().test(null));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        Map<String, Object> data = new HashMap<>();
        caseDetails.setData(data);
        Map<String, Object> result = waDataMigrationService.migrate(caseDetails);
        assertNotNull(result);
        assertEquals(data, result);
        assertTrue(data.containsKey("preWorkAllocation"));
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = waDataMigrationService.migrate(null);
        assertNull(result);
    }

    @Test
    void shouldReturnCorrectValuesForWaMigration() {
        assertEquals("waCaseMigration", waDataMigrationService.getEventId());
        assertEquals("Migrate case for WA", waDataMigrationService.getEventDescription());
        assertEquals("Migrate case for WA", waDataMigrationService.getEventSummary());
    }
}
