package uk.gov.hmcts.reform.migration.service.migrate;

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
import java.util.Objects;

import static java.lang.Long.parseLong;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.multipleHearingsOutcomes.enabled", havingValue = "true")
public class MultipleHearingsOutcomeMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;
    private Map<Long, Long> caseRefToHearingIdMap;


    public MultipleHearingsOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.multipleHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        var caseAndHearingDetails = new CaseLoader(encodedDataString).findCasesWithHearingID();
        caseRefToHearingIdMap = caseAndHearingDetails.getKey();
        return caseAndHearingDetails.getValue();
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        Long selectedHearingID = caseRefToHearingIdMap.get(parseLong(caseId));
        log.info("Mapping case id {} to selected hearingID {}", caseId, selectedHearingID);

        return hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings()
            .stream()
            .filter(hearing -> Objects.equals(hearing.getHearingId(), selectedHearingID))
            .toList();
    }
}
