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
@ConditionalOnProperty(value = "migration.outcomeMigration.enabled", havingValue = "true")
public class JudgeOutcomeMigration extends CaseOutcomeMigration {

    static final String OUTCOME_MIGRATION_SUMMARY = "Migrate Outcome to Hearing Outcome";
    static final String OUTCOME_MIGRATION_DESCRIPTION = "Link Outcome to completed Hearing";

    private final HmcHearingsApiService hmcHearingsApiService;

    public JudgeOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                 HearingOutcomeService hearingOutcomeService,
                                 @Value("${migration.outcomeMigration.encoded-string}")
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
    public String getMigrationName() {
        return "Outcome";
    }

    @Override
    public String getOutcomeFieldName() {
        return "outcome";
    }

    @Override
    public void resetSourceCaseFields(Map<String, Object> data, String caseId) {
        log.info("{} found with value {} and set to null for case id {}", getMigrationName(),
                 data.get(getOutcomeFieldName()), caseId);
        data.put(getOutcomeFieldName(), null);
    }
}
