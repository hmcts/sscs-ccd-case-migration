package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.AmendReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.UPDATE_REQUESTED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.UPDATE_SUBMITTED;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.defaultPanelComposition.enabled", havingValue = "true")
public class DefaultPanelCompositionMigration extends CaseMigrationProcessor {

    static final String UPDATE_LISTING_REQUIREMENTS_ID = "updateListingRequirements";
    static final String UPDATE_LISTING_REQUIREMENTS_SUMMARY = "Automated update to listing requirements";
    static final String UPDATE_LISTING_REQUIREMENTS_DESCRIPTION = "Panel Member Composition updated and sent "
        + "to ListAssist";

    private final DefaultPanelCompositionQuery searchQuery;
    private final ElasticSearchRepository repository;
    private final HmcHearingsApiService hmcHearingsApiService;
    private final PanelCompositionService panelCompositionService;
    private final String encodedDataString;
    private final String exclusionListEncodedString;
    private final boolean usePreFetchedCaseList;

    public DefaultPanelCompositionMigration(DefaultPanelCompositionQuery searchQuery,
                                            ElasticSearchRepository repository,
                                            HmcHearingsApiService hmcHearingsApiService,
                                            PanelCompositionService panelCompositionService,
                                            @Value("${migration.defaultPanelComposition.use-pre-fetched-case-list}")
                                            boolean usePreFetchedCaseList,
                                            @Value("${migration.defaultPanelComposition.encoded-data-string}")
                                            String encodedDataString,
                                            @Value("${migration.defaultPanelComposition.exclusion-list-encoded-string}")
                                            String exclusionListEncodedString) {
        this.searchQuery = searchQuery;
        this.repository = repository;
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.panelCompositionService = panelCompositionService;
        this.encodedDataString = encodedDataString;
        this.exclusionListEncodedString = exclusionListEncodedString;
        this.usePreFetchedCaseList = usePreFetchedCaseList;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        if (usePreFetchedCaseList) {
            return findCases(encodedDataString);
        } else {
            List<Long> exclusionList = findCases(exclusionListEncodedString).stream()
                .map(SscsCaseDetails::getId).toList();
            return repository.findCases(searchQuery, true)
                .stream()
                .filter(sscsCaseDetails -> !exclusionList.contains(sscsCaseDetails.getId()))
                .filter(caseDetails -> {
                    var hearingRoute = caseDetails.getData().getSchedulingAndListingFields().getHearingRoute();
                    var panelComposition = caseDetails.getData().getPanelMemberComposition();
                    boolean caseValid = READY_TO_LIST.toString().equals(caseDetails.getState())
                        && LIST_ASSIST.equals(hearingRoute)
                        && (isNull(panelComposition) || panelComposition.isEmpty());
                    if (!caseValid) {
                        String errorMessage = !READY_TO_LIST.toString().equals(caseDetails.getState())
                            ? "due to incorrect state"
                            : !LIST_ASSIST.equals(hearingRoute) ? "due to incorrect hearing route"
                            : "due to invalid PanelMemberComposition";
                        log.error("Skipping Case {} for migration {} State: {} PanelMemberComposition: {}",
                                  caseDetails.getId(), errorMessage, caseDetails.getState(), panelComposition);
                    }
                    return caseValid;
                }).toList();
        }
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        skipNonReadyToList(caseDetails);

        var caseData = convertToSscsCaseData(caseDetails.getData());
        skipNonListAssist(caseData);
        skipEmptyJohTiers(caseData);
        skipInvalidHmcStatus(caseDetails);

        log.info("{} for Case: {}", getEventSummary(), caseDetails.getId());
        caseDetails.getData().put("amendReasons", List.of(AmendReason.ADMIN_REQUEST));

        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    private void skipNonReadyToList(CaseDetails caseDetails) {
        if (!READY_TO_LIST.toString().equals(caseDetails.getState())) {
            skipCase("State has changed (%s)", caseDetails.getId().toString(), caseDetails.getState());
        }
    }

    private void skipEmptyJohTiers(SscsCaseData caseData) {
        if (panelCompositionService.getDefaultPanelComposition(caseData).getJohTiers().isEmpty()) {
            skipCase("DefaultPanelComposition has no JOH tiers ", caseData.getCcdCaseId());
        }
    }

    private void skipNonListAssist(SscsCaseData caseData) {
        HearingRoute hearingRoute = caseData.getSchedulingAndListingFields().getHearingRoute();
        if (!LIST_ASSIST.equals(hearingRoute)) {
            skipCase("hearingRoute is not list assist", caseData.getCcdCaseId());
        }
    }

    private void skipInvalidHmcStatus(CaseDetails caseDetails) {
        String caseId = caseDetails.getId().toString();
        List<CaseHearing> hearingsList =
            hmcHearingsApiService.getHearingsRequest(caseId, null).getCaseHearings();

        var hearingAwaitingListing = hearingsList.stream()
            .filter(hearing -> List.of(AWAITING_LISTING, UPDATE_REQUESTED, UPDATE_SUBMITTED)
                .contains(hearing.getHmcStatus()))
            .findAny().orElse(null);

        if (nonNull(hearingAwaitingListing)) {
            log.info("{} for Case: {} with hearing ID: {} and hmc status: {}", getEventSummary(), caseId,
                     hearingAwaitingListing.getHearingId(), hearingAwaitingListing.getHmcStatus());
        } else {
            skipCase("HMC status is not valid for ULR. HMC Status:(%s)",
                     caseId, hearingsList.stream().map(CaseHearing::getHmcStatus).toList());
        }
    }

    private void skipCase(String rootCause, Object... msgArgs) {
        String failureMsg = format("Skipping Case (%s) for migration because " + rootCause, msgArgs);
        log.error(failureMsg);
        throw new RuntimeException(failureMsg);
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
