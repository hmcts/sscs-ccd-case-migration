package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.migration.service.migrate.WaFieldsRemovalServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.WaFieldsRemovalServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.WaFieldsRemovalServiceImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
public class WaFieldsRemovalServiceImplTest {

    WaFieldsRemovalServiceImpl waFieldsRemovalService;
    private CaseDetails caseDetails;

    @BeforeEach
    public void setUp() {
        waFieldsRemovalService = new WaFieldsRemovalServiceImpl(null);
        caseDetails = CaseDetails.builder().id(1234L).build();
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().id(1234L).build();
        assertTrue(waFieldsRemovalService.accepts().test(sscsCaseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(waFieldsRemovalService.accepts().test(null));
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        waFieldsRemovalService.migrate(caseDetails);
        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForWaMigration() {
        assertEquals(EVENT_ID, waFieldsRemovalService.getEventId());
        assertEquals(EVENT_DESCRIPTION, waFieldsRemovalService.getEventDescription());
        assertEquals(EVENT_SUMMARY, waFieldsRemovalService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        var caseData = SscsCaseData.builder()
            .workAllocationFields(
                WorkAllocationFields.builder()
                    .scannedDocumentTypes(
                        List.of("appellantEvidence", "representativeEvidence", "Other document", "dl16")
                    )
                    .assignedCaseRoles(List.of("hearing-judge"))
                    .previouslyAssignedCaseRoles(List.of("[CREATOR]", "hearing-judge"))
                    .build())
            .build();
        caseDetails.setData(buildCaseDataMap(caseData));

        waFieldsRemovalService.migrate(caseDetails);

        assertNotNull(caseDetails.getData());
        assertNull(caseDetails.getData().get("scannedDocumentTypes"));
        assertNull(caseDetails.getData().get("assignedCaseRoles"));
        assertNull(caseDetails.getData().get("previouslyAssignedCaseRoles"));
    }
}
