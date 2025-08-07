package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.finalDecisionOutcomeMigration.enabled", havingValue = "true")
public class FinalDecisionOutcomeMigration extends CaseOutcomeMigration {

    static final String OUTCOME_MIGRATION_SUMMARY = "Migrate Outcome to Hearing Outcome";
    static final String OUTCOME_MIGRATION_DESCRIPTION = "Link Outcome to completed Hearing";
    public static final String OUTCOME = "outcome";

    private final HmcHearingsApiService hmcHearingsApiService;

    public FinalDecisionOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                         HearingOutcomeService hearingOutcomeService,
                                         @Value("${migration.finalDecisionOutcomeMigration.encoded-string}")
                                 String outcomeEncodedDataString) {
        super(hearingOutcomeService, outcomeEncodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
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

    @Override
    public String getOutcomeFieldName() {
        return OUTCOME;
    }

    @Override
    public void resetOutcomeFields(Map<String, Object> data, String caseId) {
        log.info("{} found with value {} and set to null for case id {}", getClass().getSimpleName(),
                 data.get(getOutcomeFieldName()), caseId);
        data.put(getOutcomeFieldName(), null);
    }
}
