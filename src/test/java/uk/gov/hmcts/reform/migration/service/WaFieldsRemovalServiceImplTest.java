package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    WaFieldsRemovalServiceImpl waFieldsRemovalService = new WaFieldsRemovalServiceImpl();

    private final CaseDetails caseDetails = CaseDetails.builder()
        .id(1234L)
        .build();

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
        Map<String, Object> result = waFieldsRemovalService.migrate(null, null);
        assertNull(result);
    }

    @Test
    void shouldReturnCorrectValuesForWaMigration() {
        assertEquals(EVENT_ID, waFieldsRemovalService.getEventId());
        assertEquals(EVENT_DESCRIPTION, waFieldsRemovalService.getEventDescription());
        assertEquals(EVENT_SUMMARY, waFieldsRemovalService.getEventSummary());
    }

    @ParameterizedTest
    @MethodSource("getDataValues")
    void shouldReturnPassedDataWhenMigrateCalled(String key, List<String> value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        Map<String, Object> result = waFieldsRemovalService.migrate(data, caseDetails);
        assertNotNull(result);
        assertNull(result.get(key));
    }

    private static Stream<Arguments> getDataValues() {
        return Stream.of(
            Arguments.of("scannedDocumentTypes",
                         List.of("appellantEvidence", "representativeEvidence", "Other document", "dl16")),
            Arguments.of("assignedCaseRoles", List.of("hearing-judge")),
            Arguments.of("previouslyAssignedCaseRoles", List.of("[CREATOR]", "hearing-judge"))
        );
    }
}
