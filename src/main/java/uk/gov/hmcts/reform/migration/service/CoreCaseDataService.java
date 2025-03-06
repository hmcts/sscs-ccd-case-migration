package uk.gov.hmcts.reform.migration.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SearchCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.function.Function;

@Service
@Slf4j
public abstract class CoreCaseDataService {

    private final IdamService idamService;
    private final CcdClient ccdClient;
    private final SearchCcdCaseService ccdSearchService;

    public CoreCaseDataService(IdamService idamService, CcdClient ccdClient, SearchCcdCaseService ccdSearchService) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.ccdSearchService = ccdSearchService;
    }

    public void applyUpdatesInCcd(Long caseId, String eventType, Function<CaseDetails, UpdateResult> mutator) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        log.info("UpdateCaseV2 for caseId {} and eventType {}", caseId, eventType);
        StartEventResponse startEventResponse = ccdClient.startEvent(idamTokens, caseId, eventType);
        var caseDetails = startEventResponse.getCaseDetails();
        var result = mutator.apply(caseDetails);
        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(result.summary())
                    .description(result.description())
                    .build()
            ).data(caseDetails.getData())
            .build();
        ccdClient.submitEventForCaseworker(idamTokens, caseId, caseDataContent);
    }

    public List<SscsCaseDetails> searchForCases(String elasticSearchQuery, boolean onlySubmittedCases) {
        if (onlySubmittedCases) {
            return ccdSearchService.findSubmittedCasesBySearchCriteria(elasticSearchQuery, idamService.getIdamTokens());
        }
        return ccdSearchService.findAllCasesBySearchCriteria(elasticSearchQuery, idamService.getIdamTokens());
    }
}
