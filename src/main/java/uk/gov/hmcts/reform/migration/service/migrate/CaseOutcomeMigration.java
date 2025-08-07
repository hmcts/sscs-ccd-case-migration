package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Slf4j
public abstract class CaseOutcomeMigration extends CaseMigrationProcessor {

    static final String CASE_OUTCOME_MIGRATION_ID = "caseOutcomeMigration";
    static final String CASE_OUTCOME_MIGRATION_SUMMARY = "Hearing outcome linked to hearing date";
    static final String CASE_OUTCOME_MIGRATION_DESCRIPTION = "";
    public static final String CASE_OUTCOME = "caseOutcome";

    private final HearingOutcomeService hearingOutcomeService;
    protected final String encodedDataString;

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
                String reason = "Hearing Route is not " + getMigrationRoute();
                log.info("Skipping case for {} migration | Case ID: {} | Reason: {}",
                         getClass().getSimpleName(), caseId, reason);
                throw new RuntimeException("Skipping case for " + getClass().getSimpleName() + " migration. " + reason);
            }

            if (skipMigration(data)) {
                String reason = "Hearing outcome already exists or " + getOutcomeFieldName() + " is empty";
                log.info("Skipping case for {} migration | Case ID: {} | {}: {} | Reason: {}",
                         getClass().getSimpleName(), caseId, getOutcomeFieldName(),
                         data.get(getOutcomeFieldName()), reason
                );
                throw new RuntimeException("Skipping case for " + getClass().getSimpleName() + " migration, " + reason);
            }

            setHearingOutcome(data, caseId);
            resetOutcomeFields(data, caseId);

            log.info("Completed migration for {} migration. Case id: {}", getClass().getSimpleName(), caseId);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    boolean skipMigration(Map<String, Object> data) {
        return nonNull(data.get("hearingOutcomes")) || isNull(data.get(getOutcomeFieldName()));
    }

    String getMigrationRoute() {
        return HearingRoute.LIST_ASSIST.toString();
    }

    void setHearingOutcome(Map<String, Object> data, String caseId) {
        data.put("hearingOutcomes",
                 hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), data, getOutcomeFieldName())
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

    protected CaseHearing getHmcHearing(String caseId) {
        var hmcHearings = getHearingsFromHmc(caseId);

        if (hmcHearings.size() != 1) {
            String reason = "Zero or More than one hearing found";
            log.info("Skipping case for {} migration|Case id: {}|No of hearings: {}|Reason: {}",
                     getClass().getSimpleName(), caseId, hmcHearings.size(), reason
            );
            throw new RuntimeException("Skipping case for " + getClass().getSimpleName() + " migration, " + reason);
        }
        return hmcHearings.getFirst();
    }

    abstract List<CaseHearing> getHearingsFromHmc(String caseId);

    public String getOutcomeFieldName() {
        return CASE_OUTCOME;
    }

    public void resetOutcomeFields(Map<String, Object> data, String caseId) {
        log.info("{} found with value {} and set to null for case id {}",
                 getOutcomeFieldName(), data.get(getOutcomeFieldName()), caseId);
        data.put(getOutcomeFieldName(), null);

        log.info("did Po Attend found with value {} and set to null for case id {}", data.get("didPoAttend"), caseId);
        data.put("didPoAttend", null);

    }
}
