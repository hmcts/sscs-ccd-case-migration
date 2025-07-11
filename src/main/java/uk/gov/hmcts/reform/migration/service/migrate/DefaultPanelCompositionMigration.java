package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.AmendReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.defaultPanelComposition.enabled", havingValue = "true")
public class DefaultPanelCompositionMigration extends CaseMigrationProcessor {

    static final String UPDATE_LISTING_REQUIREMENTS_ID = "updateListingRequirements";
    static final String UPDATE_LISTING_REQUIREMENTS_SUMMARY = "Migration: Set default Panel Composition";
    static final String UPDATE_LISTING_REQUIREMENTS_DESCRIPTION = "Migration: Set default Panel Composition";

    private final DefaultPanelCompositionQuery searchQuery;
    private final ElasticSearchRepository repository;
    private final HmcHearingsApiService hmcHearingsApiService;
    private final String encodedDataString;
    private final boolean usePreFetchedCaseList;

    public DefaultPanelCompositionMigration(DefaultPanelCompositionQuery searchQuery,
                                            ElasticSearchRepository repository,
                                            HmcHearingsApiService hmcHearingsApiService,
                                            @Value("${migration.defaultPanelComposition.use-pre-fetched-case-list}")
                                            boolean usePreFetchedCaseList,
                                            @Value("${migration.defaultPanelComposition.encoded-data-string}")
                                            String encodedDataString) {
        this.searchQuery = searchQuery;
        this.repository = repository;
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.encodedDataString = encodedDataString;
        this.usePreFetchedCaseList = usePreFetchedCaseList;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        if (usePreFetchedCaseList) {
            return findCases(encodedDataString);
        } else {
            return repository.findCases(searchQuery, true)
                .stream()
                .filter(caseDetails -> READY_TO_LIST.toString().equals(caseDetails.getState())
                    && caseDetails.getData().getSchedulingAndListingFields().getHearingRoute()
                    .equals(HearingRoute.LIST_ASSIST))
                .toList();
        }
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        if (caseDetails.getState().equals(READY_TO_LIST.toString())) {

            String caseId = caseDetails.getId().toString();
            log.info(getEventSummary() + " for Case: {}", caseId);

            Optional<CaseHearing> hearingInAwaitingListingListAssistState =
                hmcHearingsApiService.getHearingsRequest(caseId, null)
                .getCaseHearings()
                .stream()
                .filter(hearing -> List.of(HmcStatus.AWAITING_LISTING, HmcStatus.UPDATE_REQUESTED,
                                           HmcStatus.UPDATE_SUBMITTED).contains(hearing.getHmcStatus()))
                .findFirst();

            if (hearingInAwaitingListingListAssistState.isPresent()) {
                log.info(getEventSummary() + " for Case: {} with hearing ID: {} and hmc status: {}",
                         caseId,
                         hearingInAwaitingListingListAssistState.get().getHearingId(),
                         hearingInAwaitingListingListAssistState.get().getHmcStatus());

                log.info("Setting Amend Reasons to Admin Request for Case: {}", caseId);
                caseDetails.getData().put("amendReasons", List.of(AmendReason.ADMIN_REQUEST));

                return new UpdateResult(getEventSummary(), getEventDescription());
            } else {
                String failureMsg = String.format("Skipping Case (%s) for migration because hmc status is not "
                                                      + "Awaiting Listing, Update Requested or Update Submitted",
                                                  caseId);
                log.error(failureMsg);
                throw new RuntimeException(failureMsg);
            }
        } else {
            String failureMsg = String.format("Skipping Case (%s) for migration because state has changed (%s)",
                                              caseDetails.getId(), caseDetails.getState());
            log.error(failureMsg);
            throw new RuntimeException(failureMsg);
        }
    }

    public String getEventId() {
        return UPDATE_LISTING_REQUIREMENTS_ID;
    }

    public String getEventDescription() {
        return UPDATE_LISTING_REQUIREMENTS_DESCRIPTION;
    }

    public String getEventSummary() {
        return UPDATE_LISTING_REQUIREMENTS_SUMMARY;
    }
}
