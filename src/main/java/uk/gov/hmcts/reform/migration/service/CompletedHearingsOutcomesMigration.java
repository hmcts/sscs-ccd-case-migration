package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.completedHearingsOutcomes.enabled", havingValue = "true")
public class CompletedHearingsOutcomesMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;

    public CompletedHearingsOutcomesMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              CoreCaseDataService coreCaseDataService,
                                              @Value("${migration.completedHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(coreCaseDataService, hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return hmcHearingsApiService.getHearingsRequest(caseId, HmcStatus.COMPLETED).getCaseHearings();
    }
}
