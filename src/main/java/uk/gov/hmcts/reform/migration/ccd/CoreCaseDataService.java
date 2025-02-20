package uk.gov.hmcts.reform.migration.ccd;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

import java.util.Map;

@Slf4j
@Service
public class CoreCaseDataService {

    @Autowired
    private IdamService idamService;

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Retryable(
        retryFor = FeignException.class,
        maxAttemptsExpression = "${case-migration.retry.max-retries}")
    public CaseDetails update(String caseType,
                              Long caseId,
                              String jurisdiction,
                              DataMigrationService<Map<String, Object>> dataMigrationService) throws Exception {
        var idamTokens = idamService.getIdamTokens();

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
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
            ).data(dataMigrationService.migrate(updatedCaseDetails))
            .build();

        return coreCaseDataApi.submitEventForCaseWorker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            updatedCaseDetails.getJurisdiction(),
            caseType,
            String.valueOf(updatedCaseDetails.getId()),
            true,
            caseDataContent);
    }

    @Retryable(
        retryFor = FeignException.class,
        maxAttemptsExpression = "${case-migration.retry.max-retries}")
    public SearchResult getCases(String caseType, String query) {
        var idamTokens = idamService.getIdamTokens();
        return coreCaseDataApi.searchCases(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(),
                                           caseType, query);
    }
}
