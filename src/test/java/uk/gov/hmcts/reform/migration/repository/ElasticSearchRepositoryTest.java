package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SearchCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchRepositoryTest {

    private static final String CASE_TYPE = "CASE_TYPE";

    private static final String INITIAL_QUERY = """
            {
              "query": {
                "bool": {
                    "must_not": [
                        {
                            "exists": {
                                "field": "data.preWorkAllocation"
                             }
                            },
                            {
                                "match": {
                                    "state": "draft"
                            }
                        }
                    ]
                }
              },
              "_source": [
                "reference"
              ],
              "size": 100,
              "sort": [
                {
                  "reference.keyword": "asc"
                }
              ]

            }""";

    private static final String SEARCH_AFTER_QUERY = """
            {
              "query": {
                "bool": {
                    "must_not": [
                        {
                            "exists": {
                                "field": "data.preWorkAllocation"
                             }
                            },
                            {
                                "match": {
                                    "state": "draft"
                            }
                        }
                    ]
                }
              },
              "_source": [
                "reference"
              ],
              "size": 100,
              "sort": [
                {
                  "reference.keyword": "asc"
                }
              ]
            ,"search_after": [1677777777]
            }""";

    private static final int QUERY_SIZE = 100;
    private final IdamTokens idamTokens = IdamTokens.builder().build();
    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private SearchCcdCaseService ccdSearchService;
    @Mock
    private IdamService idamService;

    @Mock
    private ElasticSearchQuery elasticSearchQuery;

    @BeforeEach
    public void setUp() {
        elasticSearchRepository = new ElasticSearchRepository(ccdSearchService, idamService, CASE_TYPE, QUERY_SIZE);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void shouldReturnSearchResultsForCaseTypeElasticSearch() {
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(ccdSearchService.findCaseBySearchCriteria(INITIAL_QUERY, idamTokens)).thenReturn(List.of());

        List<SscsCaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery);

        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldNotReturnCaseDetailsForCaseTypeWhenSearchResultIsNull() {
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);

        List<SscsCaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery);

        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldReturnSearchResultsAndCaseDetailsForCaseTypeElasticSearch() {
        List<SscsCaseDetails> searchResults = List.of(SscsCaseDetails.builder().id(1677777777L).build());
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(ccdSearchService.findCaseBySearchCriteria(INITIAL_QUERY, idamTokens)).thenReturn(searchResults);
        List<SscsCaseDetails> subsequentSearchResults = List.of(SscsCaseDetails.builder().id(1777777777L).build());
        when(ccdSearchService.findCaseBySearchCriteria(SEARCH_AFTER_QUERY, idamTokens))
            .thenReturn(subsequentSearchResults)
            .thenReturn(null);

        List<SscsCaseDetails> actualSearchResults = elasticSearchRepository.findCases(elasticSearchQuery);

        assertNotNull(actualSearchResults);
        verify(ccdSearchService).findCaseBySearchCriteria(INITIAL_QUERY, idamTokens);
        verify(ccdSearchService, times(2))
            .findCaseBySearchCriteria(SEARCH_AFTER_QUERY, idamTokens);
        assertEquals(2, actualSearchResults.size());
    }

    @Test
    public void shouldReturnOnlyInitialCaseDetailsWhenSearchAfterReturnsNullSearchResults() {
        List<SscsCaseDetails> searchResults = List.of(SscsCaseDetails.builder().id(1677777777L).build());
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(ccdSearchService.findCaseBySearchCriteria(INITIAL_QUERY, idamTokens)).thenReturn(searchResults);
        when(ccdSearchService.findCaseBySearchCriteria(SEARCH_AFTER_QUERY, idamTokens)).thenReturn(null);

        List<SscsCaseDetails> actualSearchResults = elasticSearchRepository.findCases(elasticSearchQuery);

        assertNotNull(actualSearchResults);
        verify(ccdSearchService).findCaseBySearchCriteria(INITIAL_QUERY, idamTokens);
        verify(ccdSearchService).findCaseBySearchCriteria(SEARCH_AFTER_QUERY, idamTokens);
        assertEquals(1, actualSearchResults.size());
    }
}
