package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

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
    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private CoreCaseDataService ccdSearchService;
    @Mock
    private ElasticSearchQuery elasticSearchQuery;

    @BeforeEach
    public void setUp() {
        elasticSearchRepository = new ElasticSearchRepository(ccdSearchService, QUERY_SIZE);
    }

    @Test
    public void shouldReturnSearchResultsForCaseTypeElasticSearch() {
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(ccdSearchService.searchForCases(INITIAL_QUERY, true)).thenReturn(List.of());

        List<SscsCaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery, true);

        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldNotReturnCaseDetailsForCaseTypeWhenSearchResultIsNull() {
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);

        List<SscsCaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery, true);

        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldReturnSearchResultsAndCaseDetailsForCaseTypeElasticSearch() {
        List<SscsCaseDetails> searchResults = List.of(SscsCaseDetails.builder().id(1677777777L).build());
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(ccdSearchService.searchForCases(INITIAL_QUERY, false)).thenReturn(searchResults);
        List<SscsCaseDetails> subsequentSearchResults = List.of(SscsCaseDetails.builder().id(1777777777L).build());
        when(ccdSearchService.searchForCases(SEARCH_AFTER_QUERY, false))
            .thenReturn(subsequentSearchResults)
            .thenReturn(null);

        List<SscsCaseDetails> actualSearchResults = elasticSearchRepository.findCases(elasticSearchQuery, false);

        assertNotNull(actualSearchResults);
        verify(ccdSearchService).searchForCases(INITIAL_QUERY, false);
        verify(ccdSearchService, times(2))
            .searchForCases(SEARCH_AFTER_QUERY, false);
        assertEquals(2, actualSearchResults.size());
    }

    @Test
    public void shouldReturnOnlyInitialCaseDetailsWhenSearchAfterReturnsNullSearchResults() {
        List<SscsCaseDetails> searchResults = List.of(SscsCaseDetails.builder().id(1677777777L).build());
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(ccdSearchService.searchForCases(INITIAL_QUERY, true)).thenReturn(searchResults);
        when(ccdSearchService.searchForCases(SEARCH_AFTER_QUERY, true)).thenReturn(null);

        List<SscsCaseDetails> actualSearchResults = elasticSearchRepository.findCases(elasticSearchQuery, true);

        assertNotNull(actualSearchResults);
        verify(ccdSearchService).searchForCases(INITIAL_QUERY, true);
        verify(ccdSearchService).searchForCases(SEARCH_AFTER_QUERY, true);
        assertEquals(1, actualSearchResults.size());
    }
}
