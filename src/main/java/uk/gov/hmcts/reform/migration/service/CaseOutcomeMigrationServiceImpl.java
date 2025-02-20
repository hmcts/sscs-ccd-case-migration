package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.resetCaseOutcome;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hearingOutcomesMigration.enabled", havingValue = "true")
public class CaseOutcomeMigrationServiceImpl extends CaseMigrationProcessor {

    static final String CASE_OUTCOME_MIGRATION_ID = "caseOutcomeMigration";
    static final String CASE_OUTCOME_MIGRATION_SUMMARY = "Hearing outcome linked to hearing date";
    static final String CASE_OUTCOME_MIGRATION_DESCRIPTION = "";

    private final HmcHearingsApiService hmcHearingsApiService;
    private final String encodedDataString;

    public CaseOutcomeMigrationServiceImpl(CoreCaseDataService coreCaseDataService,
                                           HmcHearingsApiService hmcHearingsApiService,
                                           @Value("${migration.hearingOutcomesMigration.encoded-data-string}")
                                           String encodedDataString) {
        super(coreCaseDataService);
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.encodedDataString = encodedDataString;
    }

    public Map<String, Object> migrate(CaseDetails caseDetails) throws Exception {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            SscsCaseData caseData = getSscsCaseDataFrom(data);
            String caseId = caseDetails.getId().toString();

            if (caseData.getHearingOutcomes() != null) {
                log.info("Skipping case for case outcome migration. Case id: {} Reason: Hearing outcome already exists",
                        caseId);
                throw new Exception("Skipping case for case outcome migration. Hearing outcome already exists");
            }

            if (caseData.getCaseOutcome().getCaseOutcome() == null) {

                log.info("Skipping case for case outcome migration. Case id: {} Reason: Case outcome is empty", caseId);
                throw new Exception("Skipping case for case outcome migration. Case outcome is empty");

            } else {
                HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(caseId,HmcStatus.COMPLETED);
                List<CaseHearing> hmcHearings = response.getCaseHearings();

                if (hmcHearings.isEmpty()) {
                    log.info("Skipping case for case outcome migration. Case id: {} "
                            + "Reason: No completed hearings found", caseId);
                    throw new Exception("Skipping case for case outcome migration. No completed hearings found");
                }
                if (hmcHearings.size() > 1) {
                    log.info("Skipping case for case outcome migration. Case id: {} "
                            + "Reason: More than one completed hearing found", caseId);
                    throw new Exception("Skipping case for case outcome migration. "
                            + "More than one completed hearing found");
                } else {
                    String hearingID = hmcHearings.get(0).getHearingId().toString();
                    log.info("Completed hearing found for case id {} with hearing id {}", caseId, hearingID);
                    Map<String, Object> hearingOutcomeMap = buildHearingOutcomeMap(caseData, hearingID);
                    data.put("hearingOutcomes", hearingOutcomeMap);
                    resetCaseOutcome(data, caseId);
                }
            }
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    @Override
    public String getEventId() {
        return CASE_OUTCOME_MIGRATION_ID;
    }

    @Override
    public String getEventDescription() {
        return CASE_OUTCOME_MIGRATION_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return CASE_OUTCOME_MIGRATION_SUMMARY;
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
}
