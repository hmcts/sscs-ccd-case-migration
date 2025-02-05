package uk.gov.hmcts.reform.migration.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hearingOutcomesMigration.enabled", havingValue = "true")
public abstract class CaseOutcomeMigration implements DataMigrationService<Map<String, Object>> {

    static final String SKIPPING_CASE_MSG = "Skipping case for case outcome migration";
    static final String EVENT_ID = "caseOutcomeMigration";
    static final String EVENT_SUMMARY = "Hearing outcome linked to hearing date";
    static final String EVENT_DESCRIPTION = "";

    private final HearingOutcomeService hearingOutcomeService;

    public CaseOutcomeMigration(HearingOutcomeService hearingOutcomeService) {
        this.hearingOutcomeService = hearingOutcomeService;
    }



    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    public Map<String, Object> migrate(Map<String, Object> data, CaseDetails caseDetails) throws Exception {
        if (nonNull(data)) {
            final SscsCaseData caseData = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build().convertValue(data, SscsCaseData.class);

            String caseId = caseDetails.getId().toString();

            if (nonNull(caseData.getHearingOutcomes()) ||
                isNull(caseData.getCaseOutcome()) || isNull(caseData.getCaseOutcome().getCaseOutcome())) {
                log.info(SKIPPING_CASE_MSG + "|Case id: {}|Case outcome: {}|Hearing outcome: {}|" +
                             "Reason: Hearing outcome already exists or Case outcome is empty",
                         caseId, caseData.getCaseOutcome(), caseData.getHearingOutcomes()
                );
                throw new Exception(SKIPPING_CASE_MSG + ", Hearing outcome already exists or Case outcome is empty");
            }

            data.put(
                "hearingOutcomes", hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), caseData)
            );

            log.info("Case outcome reset from {} to null for case id {}", data.get("caseOutcome"), caseId);
            data.put("caseOutcome", null);

            log.info("'Did Po Attend found' reset from {} to null for case id {}", data.get("didPoAttend"), caseId);
            data.put("didPoAttend", null);

            log.info("Completed case outcome migration. Case id: {}", caseId);
        }
        return data;
    }

    private CaseHearing getHmcHearing(String caseId) throws Exception {
        var hmcHearings = getHearingsFromHmc(caseId);

        if (hmcHearings.size() != 1) {
            log.info(SKIPPING_CASE_MSG + "|Case id: {}|No of hearings: {}|Reason: Zero or More than one hearing found",
                     caseId, hmcHearings.size()
            );
            throw new Exception(SKIPPING_CASE_MSG + ", Zero or More than one hearing found");
        }
        return hmcHearings.get(0);
    }

    abstract List<CaseHearing> getHearingsFromHmc(String caseId);

    public String getEventId() {
        return EVENT_ID;
    }

    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
