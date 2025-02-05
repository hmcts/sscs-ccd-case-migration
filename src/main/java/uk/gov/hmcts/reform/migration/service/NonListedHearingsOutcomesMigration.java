package uk.gov.hmcts.reform.migration.service;

import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.COMPLETED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.LISTED;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.nonListedHearingsOutcomes.enabled", havingValue = "true")
public class NonListedHearingsOutcomesMigration extends CaseOutcomeMigration
    implements DataMigrationService<Map<String, Object>> {

    private final HmcHearingsApiService hmcHearingsApiService;

    public NonListedHearingsOutcomesMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService) {
        super(hearingOutcomeService);
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
