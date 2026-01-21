package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.AmendReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.UPDATE_REQUESTED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.UPDATE_SUBMITTED;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.venueHearing.enabled", havingValue = "true")
public class VenueHearingMigrationService extends CaseMigrationProcessor {

    static final String VENUE_MIGRATION_EVENT_ID = "updateListingRequirements";
    static final String VENUE_MIGRATION_EVENT_SUMMARY = "Migrate processing venue";
    static final String VENUE_MIGRATION_EVENT_DESCRIPTION = "";
    static final String PROCESSING_VENUE_FIELD = "processingVenue";
    static final String FAILURE_MSG = "Skipping Case (%s) for migration because no venue was found";

    private final String encodedDataString;
    private final RoboticsJsonMapper roboticsJsonMapper;
    private final HmcHearingsApiService hmcHearingsApiService;



    public VenueHearingMigrationService(@Value("${migration.venueHearing.encoded-string}")
                                 String encodedDataString,
                                        RoboticsJsonMapper roboticsJsonMapper,
                                        HmcHearingsApiService hmcHearingsApiService) {
        this.encodedDataString = encodedDataString;
        this.roboticsJsonMapper = roboticsJsonMapper;
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        String venue = roboticsJsonMapper.findVenueName(convertToSscsCaseData(caseDetails.getData()))
            .orElseThrow(() -> {
                String failureMsg = format(FAILURE_MSG, caseDetails.getId());
                log.error(failureMsg);
                return new RuntimeException(failureMsg);
            });
        caseDetails.getData().put(PROCESSING_VENUE_FIELD, venue);
        log.info("Setting processing venue to ({})", venue);
        String caseId = caseDetails.getId().toString();
        List<CaseHearing> hearingsList =
            hmcHearingsApiService.getHearingsRequest(caseId, null).getCaseHearings();

        var hearingAwaitingListing = hearingsList.stream()
            .filter(hearing -> List.of(AWAITING_LISTING, UPDATE_REQUESTED, UPDATE_SUBMITTED)
                .contains(hearing.getHmcStatus()))
            .findAny().orElse(null);
        if (nonNull(hearingAwaitingListing)) {
            caseDetails.getData().put("amendReasons", List.of(AmendReason.ADMIN_REQUEST));
            return new UpdateResult(getEventSummary(), getEventDescription());
        } else {
            log.info("Skipping case for VenueHearing migration."
                         + " Case id: {} Reason: no valid hearing found on case", caseId);
            throw new RuntimeException("Skipping case for VenueHearing migration."
                                           + "Reason: no valid hearing found on case");
        }

    }

    @Override
    public String getEventId() {
        return VENUE_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return VENUE_MIGRATION_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return VENUE_MIGRATION_EVENT_SUMMARY;
    }
}
