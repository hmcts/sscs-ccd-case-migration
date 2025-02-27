package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.migration.service.WaFieldsRemovalServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.WaFieldsRemovalServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.WaFieldsRemovalServiceImpl.EVENT_SUMMARY;

@Slf4j
public class WaFieldsRemovalServiceImplTest {

    WaFieldsRemovalServiceImpl waFieldsRemovalService;
    private SscsCaseDetails caseDetails;

    @BeforeEach
    public void setUp() {
        waFieldsRemovalService = new WaFieldsRemovalServiceImpl(null);
        caseDetails = SscsCaseDetails.builder().id(1234L).build();
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertTrue(waFieldsRemovalService.accepts().test(caseDetails));
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
        caseDetails.setData(
            SscsCaseData.builder()
                .workAllocationFields(
                    WorkAllocationFields.builder()
                        .scannedDocumentTypes(
                            List.of("appellantEvidence", "representativeEvidence", "Other document", "dl16")
                        )
                        .assignedCaseRoles(List.of("hearing-judge"))
                        .previouslyAssignedCaseRoles(List.of("[CREATOR]", "hearing-judge"))
                        .build())
                .build());

        waFieldsRemovalService.migrate(caseDetails);

        assertNotNull(caseDetails.getData());
        assertNull(caseDetails.getData().getWorkAllocationFields().getScannedDocumentTypes());
        assertNull(caseDetails.getData().getWorkAllocationFields().getAssignedCaseRoles());
        assertNull(caseDetails.getData().getWorkAllocationFields().getPreviouslyAssignedCaseRoles());
    }
}
