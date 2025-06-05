package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HearingsUpdateResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
class CancelTestHearingsServiceTest {

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    private CancelTestHearingsService cancelTestHearingsService;

    static final String EVENT_SUMMARY = "Send cancellation request for test hearing";
    static final String EVENT_DESCRIPTION = "Send cancellation request for test hearing";
    static final String EVENT_ID = "updateListingRequirements";

    @BeforeEach
    public void setUp() {
        cancelTestHearingsService = new CancelTestHearingsService(null, hmcHearingsApiService);
    }

    @Test
    void testCancelHearingsServiceShouldSendCancelRequest() {
        var data = buildCaseDataMap(buildCaseData());
        var caseDetails = CaseDetails.builder().id(1L).data(data).build();
        when(hmcHearingsApiService.getHearingsRequest(eq("1"), eq(AWAITING_LISTING))).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());
        when(hmcHearingsApiService.sendCancelHearingRequest(any(), eq("1")))
            .thenReturn(HearingsUpdateResponse.builder().build());
        var response = cancelTestHearingsService.migrate(caseDetails);
        verify(hmcHearingsApiService, times(1)).sendCancelHearingRequest(any(), eq("1"));
        assertThat(response.description()).isEqualTo(EVENT_DESCRIPTION);
        assertThat(response.summary()).isEqualTo(EVENT_SUMMARY);
    }

    @Test
    void testCancelHearingsServiceShouldSkipCaseWithNoHearingsFromHmc() {
        var data = buildCaseDataMap(buildCaseData());
        var caseDetails = CaseDetails.builder().id(1L).data(data).build();
        when(hmcHearingsApiService.getHearingsRequest(eq("1"), eq(AWAITING_LISTING))).thenReturn(
            HearingsGetResponse.builder().build());
        assertThrows(RuntimeException.class, () -> cancelTestHearingsService.migrate(caseDetails));
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(cancelTestHearingsService.getEventDescription()).isEqualTo(EVENT_DESCRIPTION);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(cancelTestHearingsService.getEventSummary()).isEqualTo(EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventId() {
        assertThat(cancelTestHearingsService.getEventId()).isEqualTo(EVENT_ID);
    }
}
