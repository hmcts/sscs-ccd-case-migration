package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public abstract class CaseOutcomeMigration extends CaseMigrationProcessor {

    static final String SKIPPING_CASE_MSG = "Skipping case for case outcome migration";
    static final String CASE_OUTCOME_MIGRATION_ID = "caseOutcomeMigration";
    static final String CASE_OUTCOME_MIGRATION_SUMMARY = "Hearing outcome linked to hearing date";
    static final String CASE_OUTCOME_MIGRATION_DESCRIPTION = "";

    private final HearingOutcomeService hearingOutcomeService;
    private final String encodedDataString;

    public CaseOutcomeMigration(HearingOutcomeService hearingOutcomeService,
                                String encodedDataString) {
        this.hearingOutcomeService = hearingOutcomeService;
        this.encodedDataString = encodedDataString;
    }

    @Override
    public void migrate(SscsCaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            final SscsCaseData caseData = caseDetails.getData();
            String caseId = caseDetails.getId().toString();

            var hearingRoute = data.getSchedulingAndListingFields().getHearingRoute();

            if (!hearingRoute.equals(getMigrationRoute())) {
                log.info(SKIPPING_CASE_MSG + " |Case id: {} "
                                + "Reason: Hearing Route is not {} it is {}",
                         caseId, getMigrationRoute(), hearingRoute);
                throw new RuntimeException(SKIPPING_CASE_MSG + ". Hearing Route is not " + getMigrationRoute());
            }

            if (isMigrationNeeded(caseData)) {
                log.info(SKIPPING_CASE_MSG + "|Case id: {}|Case outcome: {}|Hearing outcome: {}|"
                             + "Reason: Hearing outcome already exists or Case outcome is empty",
                         caseId, caseData.getCaseOutcome(), caseData.getHearingOutcomes());
                throw new RuntimeException(SKIPPING_CASE_MSG
                                               + ", Hearing outcome already exists or Case outcome is empty");
            }
            setHearingOutcome(data, caseData, caseId);
            var caseOutcome = data.getCaseOutcome();
            log.info("case outcome found with value {} and set to null for case id {}",
                     caseOutcome.getCaseOutcome(), caseId);
            caseOutcome.setCaseOutcome(null);
            log.info("did Po Attend found with value {} and set to null for case id {}",
                     caseOutcome.getDidPoAttend(), caseId);
            caseOutcome.setDidPoAttend(null);
            log.info("Completed migration for case outcome migration. Case id: {}", caseId);
        }
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    boolean isMigrationNeeded(SscsCaseData caseData) {
        return nonNull(caseData.getHearingOutcomes())
            || isNull(caseData.getCaseOutcome()) || isNull(caseData.getCaseOutcome().getCaseOutcome());
    }

    HearingRoute getMigrationRoute() {
        return HearingRoute.LIST_ASSIST;
    }

    void setHearingOutcome(SscsCaseData data, SscsCaseData caseData, String caseId) {
        data.setHearingOutcomes(hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), caseData));
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
        return hmcHearings.get(0);
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) {
        return List.of();
    }
}
