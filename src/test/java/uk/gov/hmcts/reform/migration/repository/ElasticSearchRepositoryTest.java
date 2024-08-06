package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import java.util.ArrayList;
import java.util.List;

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

    private static final String USER_TOKEN = "TEST_USER_TOKEN";

    private static final String CASE_TYPE = "CASE_TYPE";

    private static final String AUTH_TOKEN = "Test_Auth_Token";

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
    private static final int CASE_PROCESS_LIMIT = 100;

    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamRepository idamRepository;

    @Mock
    private ElasticSearchQuery elasticSearchQuery;

    @BeforeEach
    public void setUp() {
        elasticSearchRepository = new ElasticSearchRepository(coreCaseDataService,
                                                              authTokenGenerator,
                                                              idamRepository,
                                                              elasticSearchQuery,
                                                              CASE_TYPE,
                                                              QUERY_SIZE,
                                                              CASE_PROCESS_LIMIT);
    }

    @Test
    public void shouldReturnSearchResultsForCaseTypeElasticSearch() {
        SearchResult searchResult = mock(SearchResult.class);
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(coreCaseDataService.getCases(
            USER_TOKEN,
            CASE_TYPE,
            AUTH_TOKEN,
            INITIAL_QUERY
        )).thenReturn(searchResult);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCases();
        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldNotReturnCaseDetailsForCaseTypeWhenSearchResultIsNull() {
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCases();
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
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(coreCaseDataService.getCases(
            USER_TOKEN,
            CASE_TYPE,
            AUTH_TOKEN,
            INITIAL_QUERY
        )).thenReturn(searchResult);

        SearchResult searchAfterResult = mock(SearchResult.class);
        when(coreCaseDataService.getCases(
            USER_TOKEN,
            CASE_TYPE,
            AUTH_TOKEN,
            SEARCH_AFTER_QUERY
        )).thenReturn(searchAfterResult);

        List<CaseDetails> caseDetails1 = new ArrayList<>();
        CaseDetails details1 = mock(CaseDetails.class);
        caseDetails1.add(details1);
        when(searchAfterResult.getCases()).thenReturn(caseDetails1, List.of());

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCases();
        assertNotNull(returnCaseDetails);
        verify(authTokenGenerator, times(1)).generate();
        verify(coreCaseDataService, times(1)).getCases(USER_TOKEN,
                                                      CASE_TYPE,
                                                      AUTH_TOKEN,
                                                      INITIAL_QUERY);
        verify(coreCaseDataService, times(1)).getCases(USER_TOKEN,
                                                      CASE_TYPE,
                                                      AUTH_TOKEN,
                                                      SEARCH_AFTER_QUERY);

        assertEquals(2, returnCaseDetails.size());
    }

    @Test
    public void shouldReturnOnlyInitialCaseDetailsWhenSearchAfterReturnsNullSearchResults() {
        List<CaseDetails> caseDetails = new ArrayList<>();
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        caseDetails.add(details);
        SearchResult searchResult = mock(SearchResult.class);
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(elasticSearchQuery.getQuery(isNull(), anyInt(), eq(true))).thenReturn(INITIAL_QUERY);
        when(elasticSearchQuery.getQuery(anyString(), anyInt(), eq(false))).thenReturn(SEARCH_AFTER_QUERY);
        when(coreCaseDataService.getCases(
            USER_TOKEN,
            CASE_TYPE,
            AUTH_TOKEN,
            INITIAL_QUERY
        )).thenReturn(searchResult);

        when(coreCaseDataService.getCases(
            USER_TOKEN,
            CASE_TYPE,
            AUTH_TOKEN,
            SEARCH_AFTER_QUERY
        )).thenReturn(null);

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCases();
        assertNotNull(returnCaseDetails);

        verify(authTokenGenerator, times(1)).generate();

        verify(coreCaseDataService, times(1)).getCases(USER_TOKEN,
                                                      CASE_TYPE,
                                                      AUTH_TOKEN,
                                                      INITIAL_QUERY);
        verify(coreCaseDataService, times(1)).getCases(USER_TOKEN,
                                                      CASE_TYPE,
                                                      AUTH_TOKEN,
                                                      SEARCH_AFTER_QUERY);

        assertEquals(1, returnCaseDetails.size());
    }
}
