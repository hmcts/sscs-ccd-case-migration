package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.multipleHearingsOutcomes.enabled", havingValue = "true")
public class MultipleHearingsOutcomeMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;

    private final Map<String, String> caseMap = new CaseLoader(encodedDataString).findCasesWithHearingID();

    public MultipleHearingsOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.multipleHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.encodedDataString = encodedDataString;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        List<CaseHearing> allhmcHearings = hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings();

        Long selectedHearingID = Long.valueOf(caseMap.get(caseId));
        log.info("Mapping case id {} to selected hearingID {}", caseId, selectedHearingID);

        List<CaseHearing>  selectedHearing = allhmcHearings.stream()
            .filter(hearing -> hearing.getHearingId().equals(selectedHearingID)).toList();

        if (selectedHearing.size() != 1) {
            log.info(SKIPPING_CASE_MSG
                + " | Case id: {} | Hearing id: {} |Reason: Hearing with selected hearing id not found on case",
                     caseId, selectedHearingID);
            throw new RuntimeException(SKIPPING_CASE_MSG + ", Hearing with selected hearing id not found on case");
        }
        return selectedHearing;
    }
}
