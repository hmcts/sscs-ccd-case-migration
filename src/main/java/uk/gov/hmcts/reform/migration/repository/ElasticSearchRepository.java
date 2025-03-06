package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Repository
@Slf4j
public class ElasticSearchRepository {

    CoreCaseDataService ccdSearchService;

    private final int querySize;

    @Autowired
    public ElasticSearchRepository(CoreCaseDataService ccdSearchService,
                                   @Value("${case-migration.elasticsearch.querySize}") int querySize) {
        this.ccdSearchService = ccdSearchService;
        this.querySize = querySize;
    }

    public List<SscsCaseDetails> findCases(ElasticSearchQuery elasticSearchQuery, boolean onlySubmittedCases) {
        log.info("Processing the Case Migration search.");

        String initialQuery = elasticSearchQuery.getQuery(null, querySize, true);
        var searchResultCases = ccdSearchService.searchForCases(initialQuery, onlySubmittedCases);
        List<SscsCaseDetails> caseDetails = new ArrayList<>();

        if (nonNull(searchResultCases) && !searchResultCases.isEmpty()) {
            caseDetails.addAll(searchResultCases);
            String searchAfterValue = searchResultCases.get(searchResultCases.size() - 1).getId().toString();

            boolean keepSearching;
            do {
                String subsequentSearchQuery =
                    elasticSearchQuery.getQuery(searchAfterValue, querySize, false);
                var subsequentSearchCases = ccdSearchService.searchForCases(subsequentSearchQuery, onlySubmittedCases);

                keepSearching = false;
                if (nonNull(subsequentSearchCases)) {
                    caseDetails.addAll(subsequentSearchCases);
                    keepSearching = !subsequentSearchCases.isEmpty();
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
