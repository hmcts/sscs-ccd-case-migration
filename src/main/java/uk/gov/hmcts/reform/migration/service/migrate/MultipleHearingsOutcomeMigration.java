package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.completedHearingsOutcomes.enabled", havingValue = "true")
public class MultipleHearingsOutcomeMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;

    private final Map<String, String> caseMap = new CaseLoader(encodedDataString).findCasesWithHearingID();

    public MultipleHearingsOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.completedHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.encodedDataString = encodedDataString;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        List<CaseHearing> allhmcHearings = hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings();

        Long jostSelectedHearingID = Long.valueOf(caseMap.get(caseId));

        List<CaseHearing>  jostSelectedHearing = allhmcHearings.stream()
            .filter(hearing -> hearing.getHearingId().equals(jostSelectedHearingID)).toList();

        if (jostSelectedHearing.size() != 1) {
            log.info(SKIPPING_CASE_MSG
                + "Cannot map to hearingID {} selected for case id {}",
               jostSelectedHearingID, caseId);
            throw new RuntimeException(SKIPPING_CASE_MSG + ", Zero or More than one hearing found");
        }
        return jostSelectedHearing;
    };

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return new CaseLoader(encodedDataString).findCases(true); //use findCasesWithHearingID new method
    }
}
