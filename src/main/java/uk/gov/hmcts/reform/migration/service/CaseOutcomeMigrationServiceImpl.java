package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hearingOutcomesMigration.enabled", havingValue = "true")
public class CaseOutcomeMigrationServiceImpl  implements DataMigrationService<Map<String, Object>> {
    static final String EVENT_ID = "caseManagementLocationMigration";
    // ASK UMA TO CHECK WHAT EVENT SUMMARY AND EVENT DESCRIPTION SHOULD BE
    static final String EVENT_SUMMARY = "Migrate Case Outcome fields to Hearing Outcome";
    static final String EVENT_DESCRIPTION = "Migrate Case Outcome fields (caseOutcome, didPoAttend) "
        + "to Hearing Outcome with values from hmcHearings and Hearings";

    private final HmcHearingsApiService hmcHearingsApiService;

    public CaseOutcomeMigrationServiceImpl(HmcHearingsApiService hmcHearingsApiService) {
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    public Map<String, Object> migrate(Map<String, Object> data, CaseDetails caseDetails) {
        if (nonNull(data)) {

            SscsCaseData caseData = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build().convertValue(data, SscsCaseData.class);

            String caseId = caseDetails.getId().toString();

            if (caseData.getCaseOutcome().getCaseOutcome() == null) {
                log.info("case outcome is empty for case id {}, continuing to next case", caseId);
                return data;
            } else {
                HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(caseId,HmcStatus.COMPLETED);
                List<CaseHearing> hmcHearings = response.getCaseHearings();

                if (hmcHearings.isEmpty()) {
                    log.info("No completed hearings found for case id {}", caseId);
                    return data;
                }
                if (hmcHearings.size() > 1) {
                    log.info("More than one completed hearing found for case id {}", caseId);
                } else {
                    String hearingID = hmcHearings.get(0).getHearingId().toString();
                    log.info("Completed hearing found for case id {} with hearing id {}", caseId, hearingID);

                    Map<String, Object> hearingOutcomeMap = buildHearingOutcomeMap(caseData, hearingID);

                    data.put("hearingOutcomes", hearingOutcomeMap);

                    log.info("case outcome found with value {} and set to null for case id {}",
                             data.get("caseOutcome"), caseId);
                    data.put("caseOutcome", null);

                    log.info("did Po Attend found with value {} and set to null for case id {}",
                             data.get("didPoAttend"), caseId);
                    data.put("didPoAttend", null);
                }
            }
        }
        return data;
    }

    private static HearingDetails getHearingDetails(SscsCaseData caseData, String hearingID) {
        return caseData.getHearings().stream()
            .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(hearingID))
            .findFirst().orElse(Hearing.builder().build()).getValue();
    }

    private static Map<String, Object> buildHearingOutcomeMap(SscsCaseData caseData, String hearingID) {
        HearingDetails selectedHearingDetails = getHearingDetails(caseData, hearingID);

        HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
            .completedHearingId(hearingID)
            .hearingStartDateTime(selectedHearingDetails.getStart())
            .hearingEndDateTime(selectedHearingDetails.getEnd())
            .hearingOutcomeId(caseData.getCaseOutcome().getCaseOutcome())
            .didPoAttendHearing(caseData.getCaseOutcome().getDidPoAttend())
            .hearingChannelId(selectedHearingDetails.getHearingChannel())
            .venue(selectedHearingDetails.getVenue())
            .epimsId(selectedHearingDetails.getEpimsId())
            .build();

        HearingOutcome hearingOutcome = HearingOutcome.builder().value(hearingOutcomeDetails).build();

        return Map.of("hearingOutcomes", hearingOutcome);
    }

    public String getEventId() {
        return EVENT_ID;
    }

    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
