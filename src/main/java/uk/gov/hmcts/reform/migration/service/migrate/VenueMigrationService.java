package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.List;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.processingVenue.enabled", havingValue = "true")
public class VenueMigrationService extends CaseMigrationProcessor {

    static final String VENUE_MIGRATION_EVENT_ID = "migrateCase";
    static final String VENUE_MIGRATION_EVENT_SUMMARY = "Migrate to update processing venue for Child support cases";
    static final String VENUE_MIGRATION_EVENT_DESCRIPTION = "";
    static final String PROCESSING_VENUE_FIELD = "processingVenue";
    static final String FAILURE_MSG = "Skipping Case (%s) for migration because no venue was found";

    private final String encodedDataString;
    private final RoboticsJsonMapper roboticsJsonMapper;
    private final SscsCcdConvertService sscsCcdConvertService;


    public VenueMigrationService(@Value("${migration.processingVenue.encoded-string}")
                                 String encodedDataString,
                                 SscsCcdConvertService sscsCcdConvertService,
                                 RoboticsJsonMapper roboticsJsonMapper) {
        this.encodedDataString = encodedDataString;
        this.roboticsJsonMapper = roboticsJsonMapper;
        this.sscsCcdConvertService = sscsCcdConvertService;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        var sscsCaseData = sscsCcdConvertService.getCaseData(caseDetails.getData());

        String venue = roboticsJsonMapper.findVenueName(sscsCaseData)
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
}
