package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

class WaDataMigrationServiceImplTest {

    private WaDataMigrationServiceImpl waDataMigrationService;

    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        waDataMigrationService =
            new WaDataMigrationServiceImpl(null,null);
        caseDetails = CaseDetails.builder().id(1234L).build();
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().id(1234L).build();
        assertTrue(waDataMigrationService.accepts().test(sscsCaseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(waDataMigrationService.accepts().test(null));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        var data = buildCaseDataMap(SscsCaseData.builder().build());
        caseDetails.setData(data);

        waDataMigrationService.migrate(caseDetails);

        assertNotNull(caseDetails.getData());
        assertEquals(data, caseDetails.getData());
        assertNotNull(data.get("preWorkAllocation"));
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        waDataMigrationService.migrate(caseDetails);

        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForWaMigration() {
        assertEquals("waCaseMigration", waDataMigrationService.getEventId());
        assertEquals("Migrate case for WA", waDataMigrationService.getEventDescription());
        assertEquals("Migrate case for WA", waDataMigrationService.getEventSummary());
    }
}
