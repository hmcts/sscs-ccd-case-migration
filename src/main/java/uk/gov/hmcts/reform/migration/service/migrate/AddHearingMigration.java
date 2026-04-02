package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToHearingId;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hmctsDwpStateMigration.enabled", havingValue = "true")
public class AddHearingMigration extends CaseMigrationProcessor {

    static final String EVENT_ID = "addHearing";
    static final String EVENT_SUMMARY = "addHearing";
    static final String EVENT_DESCRIPTION = "addHearing";
    private final ObjectMapper objectMapper;
    private final Map<String, String> caseRefToHearingIdMap;

    private final String encodedDataString;

    public AddHearingMigration(@Value("${migration.hmctsDwpStateMigration.encoded-data-string}")
                                      String encodedDataString) {
        this.encodedDataString = encodedDataString;
        this.caseRefToHearingIdMap = mapCaseRefToHearingId(encodedDataString);
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        Map<String, Object> data = caseDetails.getData();

        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            SscsCaseData sscsCaseData = objectMapper.convertValue(data, SscsCaseData.class);

            String selectedHearingId = caseRefToHearingIdMap.get(caseId);
            HearingDetails newHearing = HearingDetails.builder()
                .hearingId(selectedHearingId)
                .hearingDate("hearingDate")
                .time("time")
                .venue(Venue.builder().name("London Tribunals").build())
                .venueId("1269")
                .epimsId("369230")

                .build();
            log.info("Adding hearing with hearingId {} to case id {}", selectedHearingId, caseId);
            sscsCaseData.getHearings().add(Hearing.builder().value(newHearing).build());
        }
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
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
