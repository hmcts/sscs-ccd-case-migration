package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;

import java.util.List;

import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.COMPLETED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.UPDATE_SUBMITTED;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.nonListedHearingsOutcomes.enabled", havingValue = "true")
public class NonListedHearingsOutcomesMigration extends CaseOutcomeMigration {

    static final String NON_LISTED_OUTCOME_TAB_SUMMARY = "migrate case - link outcome to hearing";

    private final HmcHearingsApiService hmcHearingsApiService;

    public NonListedHearingsOutcomesMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.nonListedHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings()
            .stream()
            .filter(hearing ->
                        !List.of(COMPLETED, AWAITING_LISTING, UPDATE_SUBMITTED).contains(hearing.getHmcStatus()))
            .toList();
    }

    @Override
    public String getEventSummary() {
        return NON_LISTED_OUTCOME_TAB_SUMMARY;
    }
}
