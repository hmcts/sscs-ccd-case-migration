package uk.gov.hmcts.reform.migration.service.migrate;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.outcomeMigration.enabled", havingValue = "true")
public class JudgeOutcomeMigration extends CaseOutcomeMigration {

    static final String SKIPP_CASE_MSG = "Skipping case for outcome migration";
    static final String OUTCOME_MIGRATION_SUMMARY = "Migrate Outcome to Hearing Outcome";
    static final String OUTCOME_MIGRATION_DESCRIPTION = "Link Outcome to completed Hearing";

    private final HmcHearingsApiService hmcHearingsApiService;
    private final HearingOutcomeService hearingOutcomeService;

    public JudgeOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                 HearingOutcomeService hearingOutcomeService,
                                 @Value("${migration.outcomeMigration.encoded-string}")
                                 String outcomeEncodedDataString) {
        super(hearingOutcomeService, outcomeEncodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.hearingOutcomeService = hearingOutcomeService;
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        Map<String, Object> data = caseDetails.getData();
        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            String hearingRoute = caseDetails.getData().get("hearingRoute").toString();

            if (!hearingRoute.equalsIgnoreCase(getMigrationRoute())) {
                log.info(SKIPP_CASE_MSG + " |Case id: {} Reason: Hearing Route is {}", caseId, hearingRoute);
                throw new RuntimeException(SKIPP_CASE_MSG + ". Hearing Route is not " + getMigrationRoute());
            }

            if (skipMigration(data)) {
                log.info(SKIPP_CASE_MSG + "|Case id: {}|Outcome: {}|Hearing outcome: {}|Reason: Hearing outcome already"
                             + " exists or Outcome is empty", caseId, data.get("outcome"), data.get("hearingOutcomes"));
                throw new RuntimeException(SKIPP_CASE_MSG+ ", Hearing outcome already exists or outcome is empty");
            }

            setHearingOutcome(data, caseId);

            log.info("outcome found with value {} and set to null for case id {}", data.get("outcome"), caseId);
            data.put("outcome", null);

            log.info("Completed migration for outcome migration. Case id: {}", caseId);
        }
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    boolean skipMigration(Map<String, Object> data) {
        return nonNull(data.get("hearingOutcomes")) || isNull(data.get("outcome"));
    }

    @Override
    void setHearingOutcome(Map<String, Object> data, String caseId) {
        data.put("hearingOutcomes",
                 hearingOutcomeService.mapHmcHearingToHearingOutcomeUsingOutcome(getHmcHearing(caseId), data));
    }

    @Override
    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return hmcHearingsApiService.getCompletedHearings(caseId);
    }

    @Override
    public String getEventDescription() {
        return OUTCOME_MIGRATION_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return OUTCOME_MIGRATION_SUMMARY;
    }
}
