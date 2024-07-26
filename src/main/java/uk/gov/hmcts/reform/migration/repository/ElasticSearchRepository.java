package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
@ConditionalOnProperty(value = "migration.elastic.enabled", havingValue = "true")
public class ElasticSearchRepository implements CcdRepository {

    private final CoreCaseDataService coreCaseDataService;

    private final AuthTokenGenerator authTokenGenerator;

    private final int querySize;

    private final int caseProcessLimit;

    private ElasticSearchQuery elasticSearchQuery;

    @Autowired
    public ElasticSearchRepository(CoreCaseDataService coreCaseDataService,
                                   AuthTokenGenerator authTokenGenerator,
                                   ElasticSearchQuery elasticSearchQuery,
                                   @Value("${case-migration.elasticsearch.querySize}") int querySize,
                                   @Value("${case-migration.processing.limit}") int caseProcessLimit) {
        this.coreCaseDataService = coreCaseDataService;
        this.authTokenGenerator = authTokenGenerator;
        this.querySize = querySize;
        this.caseProcessLimit = caseProcessLimit;
        this.elasticSearchQuery = elasticSearchQuery;
    }

    public List<CaseDetails> findCaseByCaseType(String userToken, String caseType) {
        log.info("Processing the Case Migration search for case type {}.", caseType);
        String authToken = authTokenGenerator.generate();

        String initialQuery = elasticSearchQuery.getQuery(null, querySize, true);
        SearchResult searchResult =
            coreCaseDataService.getCases(userToken, caseType, authToken, initialQuery);
        List<CaseDetails> caseDetails = new ArrayList<>();

        if (searchResult != null && searchResult.getTotal() > 0) {
            List<CaseDetails> searchResultCases = searchResult.getCases();
            caseDetails.addAll(searchResultCases);
            String searchAfterValue = searchResultCases.get(searchResultCases.size() - 1).getId().toString();

            boolean keepSearching;
            do {
                String subsequentElasticSearchQuery = elasticSearchQuery.getQuery(searchAfterValue, querySize, false);
                SearchResult subsequentSearchResult =
                    coreCaseDataService.getCases(userToken, caseType, authToken, subsequentElasticSearchQuery);

                keepSearching = false;
                if (subsequentSearchResult != null) {
                    caseDetails.addAll(subsequentSearchResult.getCases());
                    keepSearching = !subsequentSearchResult.getCases().isEmpty();
                    if (keepSearching) {
                        searchAfterValue = caseDetails.get(caseDetails.size() - 1).getId().toString();
                    }
                }
            } while (keepSearching);
        }
        log.info("The Case Migration has processed caseDetails {}.", caseDetails.size());
        return caseDetails;
    }
}
