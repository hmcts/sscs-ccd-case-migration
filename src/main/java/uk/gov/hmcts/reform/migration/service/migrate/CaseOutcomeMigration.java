package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public abstract class CaseOutcomeMigration extends CaseMigrationProcessor {

    static final String SKIPPING_CASE_MSG = "Skipping case for case outcome migration";
    static final String CASE_OUTCOME_MIGRATION_ID = "caseOutcomeMigration";
    static final String CASE_OUTCOME_MIGRATION_SUMMARY = "Hearing outcome linked to hearing date";
    static final String CASE_OUTCOME_MIGRATION_DESCRIPTION = "";

    private final HearingOutcomeService hearingOutcomeService;
    protected String encodedDataString;

    public CaseOutcomeMigration(HearingOutcomeService hearingOutcomeService,
                                String encodedDataString) {
        this.hearingOutcomeService = hearingOutcomeService;
        this.encodedDataString = encodedDataString;
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        Map<String, Object> data = caseDetails.getData();
        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            String hearingRoute = caseDetails.getData().get("hearingRoute").toString();

            if (!hearingRoute.equalsIgnoreCase(getMigrationRoute())) {
                log.info(SKIPPING_CASE_MSG + " |Case id: {} "
                             + "Reason: Hearing Route is not {} it is {}",
                         caseId, getMigrationRoute(), hearingRoute);
                throw new RuntimeException(SKIPPING_CASE_MSG + ". Hearing Route is not " + getMigrationRoute());
            }

            if (skipMigration(data)) {
                log.info(SKIPPING_CASE_MSG + "|Case id: {}|Case outcome: {}|Hearing outcome: {}|"
                             + "Reason: Hearing outcome already exists or Case outcome is empty",
                         caseId, data.get("caseOutcome"), data.get("hearingOutcomes"));
                throw new RuntimeException(SKIPPING_CASE_MSG
                                               + ", Hearing outcome already exists or Case outcome is empty");
            }

            setHearingOutcome(data, caseId);
            log.info("case outcome found with value {} and set to null for case id {}",
                     data.get("caseOutcome"), caseId);
            data.put("caseOutcome", null);
            log.info("did Po Attend found with value {} and set to null for case id {}",
                     data.get("didPoAttend"), caseId);
            data.put("didPoAttend", null);
            log.info("Completed migration for case outcome migration. Case id: {}", caseId);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return new CaseLoader(encodedDataString).findCases();
    }

    boolean skipMigration(Map<String, Object> data) {
        return nonNull(data.get("hearingOutcomes")) || isNull(data.get("caseOutcome"));
    }

    String getMigrationRoute() {
        return HearingRoute.LIST_ASSIST.toString();
    }

    void setHearingOutcome(Map<String, Object> data, String caseId) {
        data.put(
            "hearingOutcomes", hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), data)
        );
    }

    @Override
    public String getEventId() {
        return CASE_OUTCOME_MIGRATION_ID;
    }

    @Override
    public String getEventDescription() {
        return CASE_OUTCOME_MIGRATION_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return CASE_OUTCOME_MIGRATION_SUMMARY;
    }

    private CaseHearing getHmcHearing(String caseId) {
        var hmcHearings = getHearingsFromHmc(caseId);

        if (hmcHearings.size() != 1) {
            log.info(SKIPPING_CASE_MSG + "|Case id: {}|No of hearings: {}|Reason: Zero or More than one hearing found",
                     caseId, hmcHearings.size()
            );
            throw new RuntimeException(SKIPPING_CASE_MSG + ", Zero or More than one hearing found");
        }
        return hmcHearings.getFirst();
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return List.of();
    }
}
