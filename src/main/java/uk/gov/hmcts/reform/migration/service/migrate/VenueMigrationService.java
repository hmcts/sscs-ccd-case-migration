package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.List;

import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.venue.enabled", havingValue = "true")
public class VenueMigrationService extends CaseMigrationProcessor {

    private static final String EVENT_ID = "processingVenueMigration";
    private static final String EVENT_SUMMARY = "Migrate processing venue for Child support cases";
    private static final String EVENT_DESCRIPTION = "Migrate processing venue for Child support cases," +
        "after Airlookup update replaced Bradford, Grimsby, Scarborough and York with Leeds/Hull";

    private final String encodedDataString;
    private final RoboticsJsonMapper roboticsJsonMapper;


    public VenueMigrationService(@Value("${migration.processing-venue.encoded-string}")
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
        var caseData = (SscsCaseData) caseDetails.getData();

        String venue = roboticsJsonMapper.findVenueName(caseData)
            .orElseThrow(() -> new RuntimeException("Could not find Venue"));
        caseData.setProcessingVenue(venue);
        log.info("Setting processing venue to ({})", venue);
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
