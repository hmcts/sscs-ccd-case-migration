package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

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
    private final String encodedDataString;

    public CaseOutcomeMigration(CoreCaseDataService coreCaseDataService,
                                HearingOutcomeService hearingOutcomeService,
                                String encodedDataString) {
        super(coreCaseDataService);
        this.hearingOutcomeService = hearingOutcomeService;
        this.encodedDataString = encodedDataString;
    }


    public Map<String, Object> migrate(CaseDetails caseDetails) throws Exception {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            final SscsCaseData caseData = getSscsCaseDataFrom(data);
            String caseId = caseDetails.getId().toString();
            String hearingRoute = caseDetails.getData().get("hearingRoute").toString();

            if (!hearingRoute.equalsIgnoreCase(getMigrationRoute())) {
                log.info(SKIPPING_CASE_MSG + " |Case id: {} "
                                + "Reason: Hearing Route is not {} it is {}",
                        caseId, getMigrationRoute(), hearingRoute);
                throw new Exception(SKIPPING_CASE_MSG + ". Hearing Route is not " + getMigrationRoute());
            }

            if (isMigrationNeeded(caseData)) {
                log.info(SKIPPING_CASE_MSG + "|Case id: {}|Case outcome: {}|Hearing outcome: {}|"
                                + "Reason: Hearing outcome already exists or Case outcome is empty",
                        caseId, caseData.getCaseOutcome(), caseData.getHearingOutcomes());
                throw new Exception(SKIPPING_CASE_MSG + ", Hearing outcome already exists or Case outcome is empty");
            }
            setHearingOutcome(data, caseData, caseId);
            log.info("case outcome found with value {} and set to null for case id {}",
                    data.get("caseOutcome"), caseId);
            data.put("caseOutcome", null);
            log.info("did Po Attend found with value {} and set to null for case id {}",
                    data.get("didPoAttend"), caseId);
            data.put("didPoAttend", null);
            log.info("Completed migration for case outcome migration. Case id: {}", caseId);
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    boolean isMigrationNeeded(SscsCaseData caseData) {
        return nonNull(caseData.getHearingOutcomes())
                || isNull(caseData.getCaseOutcome()) || isNull(caseData.getCaseOutcome().getCaseOutcome());
    }

    String getMigrationRoute() {
        return HearingRoute.LIST_ASSIST.toString();
    }

    void setHearingOutcome(Map<String, Object> data, SscsCaseData caseData, String caseId) throws Exception {
        data.put(
                "hearingOutcomes", hearingOutcomeService.mapHmcHearingToHearingOutcome(getHmcHearing(caseId), caseData)
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

    private CaseHearing getHmcHearing(String caseId) throws Exception {
        var hmcHearings = getHearingsFromHmc(caseId);

        if (hmcHearings.size() != 1) {
            log.info(SKIPPING_CASE_MSG
                            + " |Case id: {}|No of hearings: {} |Reason: Zero or More than one hearing found",
                    caseId, hmcHearings.size()
            );
            throw new Exception(SKIPPING_CASE_MSG + ", Zero or More than one hearing found");
        }
        return hmcHearings.get(0);
    }

    List<CaseHearing> getHearingsFromHmc(String caseId) throws Exception {
        return List.of();
    }
}
