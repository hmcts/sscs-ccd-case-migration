package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class ElasticSearchRepository {

    private final CoreCaseDataService coreCaseDataService;
    private final String caseType;

    private final int querySize;

    @Autowired
    public ElasticSearchRepository(CoreCaseDataService coreCaseDataService,
                                   @Value("${migration.caseType}") String caseType,
                                   @Value("${case-migration.elasticsearch.querySize}") int querySize) {
        this.coreCaseDataService = coreCaseDataService;
        this.caseType = caseType;
        this.querySize = querySize;
    }

    public List<CaseDetails> findCases(ElasticSearchQuery elasticSearchQuery) {
        log.info("Processing the Case Migration search for case type {}.", caseType);

        String initialQuery = elasticSearchQuery.getQuery(null, querySize, true);
        SearchResult searchResult =
            coreCaseDataService.getCases(caseType, initialQuery);
        List<CaseDetails> caseDetails = new ArrayList<>();

        if (searchResult != null && searchResult.getTotal() > 0) {
            List<CaseDetails> searchResultCases = searchResult.getCases();
            caseDetails.addAll(searchResultCases);
            String searchAfterValue = searchResultCases.get(searchResultCases.size() - 1).getId().toString();

            boolean keepSearching;
            do {
                String subsequentElasticSearchQuery = elasticSearchQuery.getQuery(searchAfterValue, querySize, false);
                SearchResult subsequentSearchResult =
                    coreCaseDataService.getCases(caseType, subsequentElasticSearchQuery);

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
