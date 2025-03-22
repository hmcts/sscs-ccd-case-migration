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
import java.util.Objects;

import static java.lang.Long.parseLong;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.multipleHearingsOutcomes.enabled", havingValue = "true")
public class MultipleHearingsOutcomeMigration extends CaseOutcomeMigration {

    private final HmcHearingsApiService hmcHearingsApiService;

    public MultipleHearingsOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.multipleHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        Map<String, String> caseRefToHearingIdMap = caseLoader.mapCaseRefToHearingId();
        String selectedHearingId = caseRefToHearingIdMap.get(caseId);
        log.info("Mapping case id {} to selected hearingID {}", caseId, selectedHearingId);

        return hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings()
            .stream()
            .filter(hearing -> Objects.equals(hearing.getHearingId(), parseLong(selectedHearingId)))
            .toList();
    }
}
