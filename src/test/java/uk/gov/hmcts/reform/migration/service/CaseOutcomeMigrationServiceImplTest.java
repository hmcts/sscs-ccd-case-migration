package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_SUMMARY;


@Slf4j
public class CaseOutcomeMigrationServiceImplTest {

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
        new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1234L)
            .build();
        assertTrue(caseOutcomeMigrationService.accepts().test(caseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(caseOutcomeMigrationService.accepts().test(null));
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        Map<String, Object> result = caseOutcomeMigrationService.migrate(null);
        assertNull(result);
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertEquals(EVENT_ID, caseOutcomeMigrationService.getEventId());
        assertEquals(EVENT_DESCRIPTION, caseOutcomeMigrationService.getEventDescription());
        assertEquals(EVENT_SUMMARY, caseOutcomeMigrationService.getEventSummary());
    }

    @ParameterizedTest
    @MethodSource("getDataValues")
    void shouldReturnPassedDataWhenMigrateCalled(String key, List<String> value) {
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(data);
        assertNotNull(result);
        assertNull(result.get(key));
    }
    // need to add hearing details to the test data
    // need to add extra tests (skip over empty case outcome, logging exceptions)

    private static Stream<Arguments> getDataValues() {
        return Stream.of(
            Arguments.of("caseOutcome",
                         List.of("1", "2", null)),
            Arguments.of("didPoAttend",
                         List.of("yes", "no", null))
        );
    }
}
