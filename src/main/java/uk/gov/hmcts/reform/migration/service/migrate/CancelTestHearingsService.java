package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HearingsUpdateResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.query.CancelTestHearingsSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.cancel-test-hearing.enabled", havingValue = "true")
public class CancelTestHearingsService extends CaseMigrationProcessor {

    static final String EVENT_ID = "updateListingRequirements";
    static final String EVENT_SUMMARY = "Send cancellation request for test hearing";
    static final String EVENT_DESCRIPTION = "Send cancellation request for test hearing";
    private final ElasticSearchRepository repository;
    private final CancelTestHearingsSearchQuery searchQuery;
    private final HmcHearingsApiService hmcHearingsApiService;


    public CancelTestHearingsService(ElasticSearchRepository repository, CancelTestHearingsSearchQuery searchQuery,
                                     HmcHearingsApiService hmcHearingsApiService) {
        this.repository = repository;
        this.searchQuery = searchQuery;
        this.hmcHearingsApiService = hmcHearingsApiService;
    }


    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return repository.findCases(searchQuery, true);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        HearingsGetResponse hmcResponse = hmcHearingsApiService
            .getHearingsRequest(caseDetails.getId().toString(), HmcStatus.AWAITING_LISTING);
        if (hmcResponse.getCaseHearings() != null && !hmcResponse.getCaseHearings().isEmpty()) {
            for (CaseHearing caseHearing : hmcResponse.getCaseHearings()) {
                HearingCancelRequestPayload cancelRequestPayload = HearingCancelRequestPayload
                    .builder().cancellationReasonCodes(Collections.singletonList(CancellationReason.OTHER)).build();
                HearingsUpdateResponse response = hmcHearingsApiService
                    .sendCancelHearingRequest(cancelRequestPayload, caseHearing.getHearingId().toString());
                log.debug("Received Cancel Hearing Request Response for Case ID {}:\n{}",
                          caseDetails.getId(),
                          response);
            }
        } else {
            log.info("Skipping case for cancel hearings job, "
                         + "no valid hearings found for CaseId {}", caseDetails.getId());
            throw new RuntimeException("No hearings awaiting listing found for case ID " + caseDetails.getId());
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public String getEventId() {
        return EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
