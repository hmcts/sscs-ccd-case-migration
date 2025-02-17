package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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

    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private ElasticSearchQuery elasticSearchQuery;

    @BeforeEach
    public void setUp() {
        elasticSearchRepository = new ElasticSearchRepository(coreCaseDataService,
                                                              CASE_TYPE,
                                                              QUERY_SIZE);
    }

    @Test
    public void shouldReturnSearchResultsForCaseTypeElasticSearch() {
        SearchResult searchResult = mock(SearchResult.class);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(coreCaseDataService.getCases(
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(searchResult);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery);
        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldNotReturnCaseDetailsForCaseTypeWhenSearchResultIsNull() {
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCases(elasticSearchQuery);
        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldReturnSearchResultsAndCaseDetailsForCaseTypeElasticSearch() {
        List<CaseDetails> caseDetails = new ArrayList<>();
        CaseDetails details = mock(CaseDetails.class);
        caseDetails.add(details);
        SearchResult searchResult = mock(SearchResult.class);
        when(details.getId()).thenReturn(1677777777L);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(coreCaseDataService.getCases(
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(searchResult);

        SearchResult searchAfterResult = mock(SearchResult.class);
        when(coreCaseDataService.getCases(
            CASE_TYPE,
            SEARCH_AFTER_QUERY
        )).thenReturn(searchAfterResult);

        List<CaseDetails> caseDetails1 = new ArrayList<>();
        CaseDetails details1 = mock(CaseDetails.class);
        caseDetails1.add(details1);
        when(searchAfterResult.getCases()).thenReturn(caseDetails1, List.of());

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCases(elasticSearchQuery);
        assertNotNull(returnCaseDetails);
        verify(coreCaseDataService, times(1)).getCases(CASE_TYPE, INITIAL_QUERY);
        verify(coreCaseDataService, times(1)).getCases(CASE_TYPE, SEARCH_AFTER_QUERY);

        assertEquals(2, returnCaseDetails.size());
    }

    @Test
    public void shouldReturnOnlyInitialCaseDetailsWhenSearchAfterReturnsNullSearchResults() {
        List<CaseDetails> caseDetails = new ArrayList<>();
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        caseDetails.add(details);
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(coreCaseDataService.getCases(CASE_TYPE, INITIAL_QUERY)).thenReturn(searchResult);

        when(coreCaseDataService.getCases(CASE_TYPE,SEARCH_AFTER_QUERY)).thenReturn(null);

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCases(elasticSearchQuery);
        assertNotNull(returnCaseDetails);

        verify(coreCaseDataService, times(1)).getCases(CASE_TYPE, INITIAL_QUERY);
        verify(coreCaseDataService, times(1)).getCases(CASE_TYPE, SEARCH_AFTER_QUERY);

        assertEquals(1, returnCaseDetails.size());
    }
}
