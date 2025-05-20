package uk.gov.hmcts.reform.migration.hmc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HearingsUpdateResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@RequiredArgsConstructor
@Service
public class HmcHearingsApiService {

    private final HmcHearingsApi hmcHearingsApi;
    private final IdamService idamService;

    public HearingsGetResponse getHearingsRequest(String caseId, HmcStatus hmcStatus) {
        log.debug("Sending Get Hearings Request for Case ID {}", caseId);
        return hmcHearingsApi.getHearingsRequest(
            getIdamTokens().getIdamOauth2Token(),
            getIdamTokens().getServiceAuthorization(),
            caseId,
            hmcStatus);
    }

    public HearingsUpdateResponse sendCancelHearingRequest(
        HearingCancelRequestPayload hearingPayload, String hearingId) {
        log.info("Sending Cancel Hearing Request for Hearing ID {} and request:\n{}",
                 hearingId,
                 hearingPayload);
        return hmcHearingsApi.cancelHearingRequest(
            getIdamTokens().getIdamOauth2Token(),
            getIdamTokens().getServiceAuthorization(),
            hearingId,
            hearingPayload);
    }

    private IdamTokens getIdamTokens() {
        return idamService.getIdamTokens();
    }
}
