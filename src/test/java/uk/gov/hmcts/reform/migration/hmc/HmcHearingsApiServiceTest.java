package uk.gov.hmcts.reform.migration.hmc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HmcHearingsApiServiceTest {

    @Mock
    private IdamService idamService;

    @Mock
    private HmcHearingsApi hmcHearingsApi;

    @InjectMocks
    private HmcHearingsApiService hmcHearingsApiService;

    @Test
    void shouldReturnResponse() {
        HearingsGetResponse hmcResponse = HearingsGetResponse.builder().build();
        when(idamService.getIdamTokens()).thenReturn(
            IdamTokens.builder().serviceAuthorization("test").idamOauth2Token("test").build());
        when(hmcHearingsApi.getHearingsRequest(any(),any(), any(),any())).thenReturn(hmcResponse);
        HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest("1234", HmcStatus.COMPLETED);
        assert (response).equals(hmcResponse);
    }
}
