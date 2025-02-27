package uk.gov.hmcts.reform.migration.ccd;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

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
    public SearchResult getCases(String caseType, String query) {
        var idamTokens = idamService.getIdamTokens();
        return coreCaseDataApi.searchCases(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(),
                                           caseType, query);
    }
}
