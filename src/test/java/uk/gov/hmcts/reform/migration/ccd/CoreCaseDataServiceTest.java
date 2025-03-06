package uk.gov.hmcts.reform.migration.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SearchCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreCaseDataServiceTest {

    @Mock
    private IdamService idamService;
    @Mock
    private CcdClient ccdClient;
    @Mock
    private SearchCcdCaseService ccdSearchService;

    @InjectMocks
    private CoreCaseDataService coreCaseDataService;

    private final IdamTokens idamTokens = IdamTokens.builder().build();

    @BeforeEach
    void setUp() {
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    void applyUpdatesInCcd() {
        Long caseId = 12345L;
        String eventType = "updateEvent";
        var data = new HashMap<String, Object>();
        data.put("hearingRoute", "gaps");
        var updateResult = new UpdateResult("summary", "description");
        Function<CaseDetails, UpdateResult> mutator = caseDetails -> {
            caseDetails.getData().put("hearingRoute", "listAssist");
            return updateResult;
        };
        CaseDetails caseDetails = CaseDetails.builder().data(data).build();
        StartEventResponse startEventResponse = StartEventResponse.builder()
            .caseDetails(caseDetails).token("token").eventId("eventId").build();
        when(ccdClient.startEvent(any(), eq(caseId), eq(eventType))).thenReturn(startEventResponse);

        coreCaseDataService.applyUpdatesInCcd(caseId, eventType, mutator);

        assertEquals("listAssist", data.get("hearingRoute"));
        verify(ccdClient).startEvent(eq(idamTokens), eq(caseId), eq(eventType));
        verify(ccdClient).submitEventForCaseworker(eq(idamTokens), eq(caseId), any());
    }

    @Test
    void shouldSearchForSubmittedCases() {
        String elasticSearchQuery = "query";
        List<SscsCaseDetails> expectedCases = List.of(SscsCaseDetails.builder().build());

        when(ccdSearchService.findSubmittedCasesBySearchCriteria(eq(elasticSearchQuery), eq(idamTokens)))
            .thenReturn(expectedCases);

        var actualCases = coreCaseDataService.searchForCases(elasticSearchQuery, true);

        assertEquals(expectedCases, actualCases);
    }

    @Test
    void shouldSearchForAllCases() {
        String elasticSearchQuery = "query";
        List<SscsCaseDetails> expectedCases = List.of(SscsCaseDetails.builder().build());
        when(ccdSearchService.findAllCasesBySearchCriteria(eq(elasticSearchQuery), eq(idamTokens)))
            .thenReturn(expectedCases);

        List<SscsCaseDetails> actualCases = coreCaseDataService.searchForCases(elasticSearchQuery, false);

        assertEquals(expectedCases, actualCases);
    }
}
