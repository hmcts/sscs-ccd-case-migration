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
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

class DwpDataMigrationServiceImplTest {

    private DwpDataMigrationServiceImpl dwpDataMigrationService;

    private CaseDetails caseDetails;

    @BeforeEach
    public void setup() {
        dwpDataMigrationService =
            new DwpDataMigrationServiceImpl(null,null);
        caseDetails = CaseDetails.builder().id(1234L).build();
    }

    @Test
    void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().id(1234L).build();
        assertTrue(dwpDataMigrationService.accepts().test(sscsCaseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(dwpDataMigrationService.accepts().test(null));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        var data = buildCaseDataMap(SscsCaseData.builder().build());
        caseDetails.setData(data);

        dwpDataMigrationService.migrate(caseDetails);

        assertNotNull(caseDetails.getData());
        assertEquals(data, caseDetails.getData());
    }

    @Test
    void shouldNotChangeExistingFields() {
        var data = SscsCaseData.builder()
            .poAttendanceConfirmed(YES)
            .dwpIsOfficerAttending(YES.getValue())
            .tribunalDirectPoToAttend(YES)
            .build();
        caseDetails.setData(buildCaseDataMap(data));

        dwpDataMigrationService.migrate(caseDetails);

        assertNotNull(data);
        assertEquals(YES, data.getPoAttendanceConfirmed());
        assertEquals(YES.toString(), data.getDwpIsOfficerAttending());
        assertEquals(YES, data.getTribunalDirectPoToAttend());
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        dwpDataMigrationService.migrate(caseDetails);
        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForDwpMigration() {
        assertEquals("dwpCaseMigration", dwpDataMigrationService.getEventId());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventDescription());
        assertEquals("Migrate case for DWP Enhancements", dwpDataMigrationService.getEventSummary());
    }
}
