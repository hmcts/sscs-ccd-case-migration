package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.COMPLETED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.LISTED;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.nonListedHearingsOutcomes.enabled", havingValue = "true")
public class NonListedHearingsOutcomesMigration extends CaseOutcomeMigration
    implements DataMigrationService<Map<String, Object>> {

    private final HmcHearingsApiService hmcHearingsApiService;

    public NonListedHearingsOutcomesMigration(JsonMapper jsonMapper,
                                              HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService) {
        super(hearingOutcomeService, jsonMapper);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings()
            .stream()
            .filter(hearing -> !List.of(COMPLETED, LISTED).contains(hearing.getHmcStatus()))
            .toList();
    }
}
