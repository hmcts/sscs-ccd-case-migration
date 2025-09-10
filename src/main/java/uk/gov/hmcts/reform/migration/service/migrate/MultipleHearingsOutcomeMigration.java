package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Long.parseLong;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToHearingId;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.multipleHearingsOutcomes.enabled", havingValue = "true")
public class MultipleHearingsOutcomeMigration extends CaseOutcomeMigration {

    static final String OUTCOME_MIGRATION_MULTIPLE_HEARINGS_SUMMARY = "Migrate Outcome to Hearing Outcome";
    static final String OUTCOME_MIGRATION_MULTIPLE_HEARINGS_DESCRIPTION = "Link Outcome to completed Hearing";
    public static final String OUTCOME = "outcome";

    private final HmcHearingsApiService hmcHearingsApiService;
    private final HearingOutcomeService hearingOutcomeService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> caseRefToHearingIdMap;

    public MultipleHearingsOutcomeMigration(HmcHearingsApiService hmcHearingsApiService,
                                              HearingOutcomeService hearingOutcomeService,
                                              @Value("${migration.multipleHearingsOutcomes.encoded-data-string}")
                                              String encodedDataString) {
        super(hearingOutcomeService, encodedDataString);
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.hearingOutcomeService = hearingOutcomeService;
        this.caseRefToHearingIdMap = mapCaseRefToHearingId(encodedDataString);
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        String selectedHearingId = caseRefToHearingIdMap.get(caseId);
        log.info("Mapping case id {} to selected hearingID {}", caseId, selectedHearingId);

        return hmcHearingsApiService.getHearingsRequest(caseId, null)
            .getCaseHearings()
            .stream()
            .filter(hearing -> Objects.equals(hearing.getHearingId(), parseLong(selectedHearingId)))
            .toList();
    }

    @Override
    boolean skipMigration(Map<String, Object> data) {
        SscsCaseData sscsCaseData = objectMapper.convertValue(data, SscsCaseData.class);

        boolean isSelectedHearingUsed = Optional.ofNullable(sscsCaseData.getHearingOutcomes())
            .orElse(Collections.emptyList())
            .stream()
            .map(HearingOutcome::getValue)
            .map(HearingOutcomeDetails::getCompletedHearingId)
            .anyMatch(caseRefToHearingIdMap::containsValue);

        return isNull(data.get(getOutcomeFieldName())) || isSelectedHearingUsed;
    }

    @Override
    void setHearingOutcome(Map<String, Object> data, String caseId) {
        SscsCaseData sscsCaseData = objectMapper.convertValue(data, SscsCaseData.class);
        List<HearingOutcome> existingHearingOutcomes = Optional.ofNullable(sscsCaseData.getHearingOutcomes())
            .orElse(new ArrayList<>());

        List<HearingOutcome> mappedHearingOutcomes =
            hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), data, getOutcomeFieldName());
        existingHearingOutcomes.addAll(mappedHearingOutcomes);
        data.put("hearingOutcomes", existingHearingOutcomes);
    }

    @Override
    public String getOutcomeFieldName() {
        return OUTCOME;
    }

    @Override
    public String getEventDescription() {
        return OUTCOME_MIGRATION_MULTIPLE_HEARINGS_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return OUTCOME_MIGRATION_MULTIPLE_HEARINGS_SUMMARY;
    }
}
