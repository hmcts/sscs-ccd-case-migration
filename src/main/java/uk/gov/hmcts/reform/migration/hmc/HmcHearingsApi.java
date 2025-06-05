package uk.gov.hmcts.reform.migration.hmc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.domain.hmc.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HearingsUpdateResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


@SuppressWarnings({"PMD.UseObjectForClearerAPI"})
@FeignClient(name = "hmc-hearing", url = "${hmc.url}")
public interface HmcHearingsApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String HEARINGS_ENDPOINT = "/hearings";
    String HEARING_ENDPOINT = "/hearing";
    String ID = "id";

    @GetMapping(HEARINGS_ENDPOINT + "/{caseId}")
    HearingsGetResponse getHearingsRequest(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @PathVariable String caseId,
        @RequestParam(name = "status", required = false) HmcStatus hmcStatus
    );

    @DeleteMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    HearingsUpdateResponse cancelHearingRequest(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @PathVariable String id,
        @RequestBody HearingCancelRequestPayload hearingDeletePayload
    );

}
