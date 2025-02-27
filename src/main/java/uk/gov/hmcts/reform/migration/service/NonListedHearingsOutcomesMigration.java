package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;

import java.util.List;

import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.COMPLETED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.LISTED;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.nonListedHearingsOutcomes.enabled", havingValue = "true")
public class NonListedHearingsOutcomesMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;

    public NonListedHearingsOutcomesMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.hearingOutcomesMigration.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {

        List<CaseHearing> allhmcHearings = hmcHearingsApiService.getHearingsRequest(caseId, null)
                .getCaseHearings();
        if (allhmcHearings.size() != 1) {
            log.info(SKIPPING_CASE_MSG
                            + " |Case id: {}|No of hearings: {} |Reason: Zero or More than one hearing found",
                    caseId, allhmcHearings.size());
            throw new RuntimeException(SKIPPING_CASE_MSG + ", Zero or More than one hearing found");
        }

        return allhmcHearings.stream()
                .filter(hearing -> !List.of(COMPLETED, LISTED).contains(hearing.getHmcStatus()))
                .toList();
    }
}
