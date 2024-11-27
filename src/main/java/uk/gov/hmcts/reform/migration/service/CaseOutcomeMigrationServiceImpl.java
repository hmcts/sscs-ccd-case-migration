package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    static final String EVENT_ID = "hearingOutcomeMigration";
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

    public Map<String, Object> migrate(Map<String, Object> data) {
        if (nonNull(data)) {

            try {
                SscsCaseData caseData = new ObjectMapper().registerModule(new JavaTimeModule())
                    .convertValue(data, SscsCaseData.class);
                String caseId = caseData.getCaseReference();

                if (caseData.getCaseOutcome().getCaseOutcome() == null) {
                    log.info("case outcome is empty for case {}, continuing to next case", caseId);
                    return data;
                }
                else {
                    HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(caseId, HmcStatus.COMPLETED);
                    List<CaseHearing> hmcHearings = response.getCaseHearings();

                    if (hmcHearings.isEmpty()) {
                        log.info("No completed hearings found for case id {}", caseId);
                        return data;
                    }
                    if (hmcHearings.size() > 1) {
                        log.info("More than one completed hearing found for case id {}", caseId);
                    }
                    else {
                        String hearingID = hmcHearings.get(0).getHearingId().toString();
                        log.info("Completed hearing found for case id {} with hearing id {}", caseId, hearingID);

                        HearingOutcome hearingOutcome = buildHearingOutcome(caseData, hearingID);
                        data.put("hearingOutcomes", hearingOutcome);
    //                    check whether you can add another hearing outcome onto migrated case
                        log.info("case outcome found with value {} and set to null for case {}", data.get("caseOutcome"), caseId);
                        data.put("caseOutcome", null);
                        log.info("did Po Attend found with value {} and set to null for case {}", data.get("didPoAttend"), caseId);
                        data.put("didPoAttend", null);
                    }
                }
            } catch (Exception e) {
                log.error("Error migrating case outcome for case {}", data.get("caseReference"), e);
            }
        }
        return data;
    }

    private static HearingDetails getHearingDetails(SscsCaseData caseData, String hearingID) {
        return caseData.getHearings().stream()
            .filter(hearing -> hearing.getValue().getHearingId().equalsIgnoreCase(hearingID))
            .findFirst().orElse(Hearing.builder().build()).getValue();
    }

    private static HearingOutcome buildHearingOutcome(SscsCaseData caseData, String hearingID) {
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

        return hearingOutcome;
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
