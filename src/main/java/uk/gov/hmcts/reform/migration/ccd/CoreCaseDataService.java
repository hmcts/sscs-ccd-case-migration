package uk.gov.hmcts.reform.migration.ccd;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.Map;

@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;
    @Autowired
    private DataMigrationService<Map<String, Object>> dataMigrationService;

    @Retryable(
        retryFor = FeignException.class,
        maxAttemptsExpression = "${case-migration.retry.max-retries}")
    public CaseDetails update(String authorisation,
                              String caseType,
                              Long caseId,
                              String jurisdiction) {
        UserDetails userDetails = idamClient.getUserDetails(AuthUtil.getBearerToken(authorisation));

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            jurisdiction,
            caseType,
            String.valueOf(caseId),
            dataMigrationService.getEventId());

        CaseDetails updatedCaseDetails = startEventResponse.getCaseDetails();

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(dataMigrationService.getEventSummary())
                    .description(dataMigrationService.getEventDescription())
                    .build()
            ).data(dataMigrationService.migrate(updatedCaseDetails.getData()))
            .build();

        return coreCaseDataApi.submitEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            updatedCaseDetails.getJurisdiction(),
            caseType,
            String.valueOf(updatedCaseDetails.getId()),
            true,
            caseDataContent);
    }

    @Retryable(
        retryFor = FeignException.class,
        maxAttemptsExpression = "${case-migration.retry.max-retries}")
    public SearchResult getCases(String userToken, String caseType, String authToken, String initialQuery) {
        return coreCaseDataApi.searchCases(userToken, authToken, caseType, initialQuery);
    }
}
