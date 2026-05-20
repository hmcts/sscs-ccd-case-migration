package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.processingVenue.enabled", havingValue = "true")
public class VenueMigrationService extends CaseMigrationProcessor {

    static final String VENUE_MIGRATION_EVENT_ID = "migrateCase";
    static final String VENUE_MIGRATION_EVENT_SUMMARY = "Glasgow Migration";
    static final String VENUE_MIGRATION_EVENT_DESCRIPTION = "Glasgow Migration";
    static final String PROCESSING_VENUE_FIELD = "processingVenue";
    static final String VENUE_TO_MIGRATE = "Stirling";
    static final List<String> STATES_TO_SKIP = List.of(State.DORMANT_APPEAL_STATE.getId(), State.DRAFT_ARCHIVED.getId(),
                                                       State.HEARING.getId(), State.VOID_STATE.getId(),
                                                       State.WITH_UT.getId());
    static final String FAILURE_MSG = "Skipping Case (%s) for migration because no venue was found";
    static final String INVALID_PROCESSING_VENUE_FAILURE_MSG = "Skipping Case %s for migration because the processing "
        + "venue is not %s";
    static final String INVALID_STATE_FAILURE_MSG = "Skipping Case %s for migration because it is in state %s";

    private final String encodedDataString;
    private final RoboticsJsonMapper roboticsJsonMapper;


    public VenueMigrationService(@Value("${migration.processingVenue.encoded-string}")
                                 String encodedDataString,
                                 RoboticsJsonMapper roboticsJsonMapper) {
        this.encodedDataString = encodedDataString;
        this.roboticsJsonMapper = roboticsJsonMapper;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {

        validateCase(caseDetails);

        String venue = roboticsJsonMapper.findVenueName(convertToSscsCaseData(caseDetails.getData()))
            .orElseThrow(() -> {
                String failureMsg = format(FAILURE_MSG, caseDetails.getId());
                log.error(failureMsg);
                return new RuntimeException(failureMsg);
            });
        caseDetails.getData().put(PROCESSING_VENUE_FIELD, venue);
        log.info("Setting processing venue to ({})", venue);
        return new UpdateResult(getEventSummary(), getEventDescription());
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

    private void validateCase(CaseDetails caseDetails) {
        validateCaseProcessingVenue(caseDetails);
        validateCaseState(caseDetails);
    }

    private void validateCaseProcessingVenue(CaseDetails caseDetails) {
        if (nonNull(caseDetails.getData().get(PROCESSING_VENUE_FIELD))
            && !caseDetails.getData().get(PROCESSING_VENUE_FIELD).equals(VENUE_TO_MIGRATE)) {
            String skipMsg = format(INVALID_PROCESSING_VENUE_FAILURE_MSG, caseDetails.getId(),VENUE_TO_MIGRATE);
            log.info(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
    }

    private void validateCaseState(CaseDetails caseDetails) {
        if (nonNull(caseDetails.getState()) && STATES_TO_SKIP.contains(caseDetails.getState())) {
            String skipMsg = format(INVALID_STATE_FAILURE_MSG, caseDetails.getId(),
                                    caseDetails.getState());
            log.info(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
    }
}
