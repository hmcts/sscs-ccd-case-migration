package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.caseOutcomeGapsMigration.enabled", havingValue = "true")
public class CaseOutcomeGapsMigrationServiceImpl extends CaseMigrationProcessor {

    static final String REMOVE_GAPS_OUTCOME_TAB_ID = "removeGapsOutcomeTab";
    static final String REMOVE_GAPS_OUTCOME_TAB_SUMMARY = "Outcome tab removed as it is Gaps case";
    static final String REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION = "Outcome tab removed as it is Gaps case";

    private final String encodedDataString;

    public CaseOutcomeGapsMigrationServiceImpl(CoreCaseDataService coreCaseDataService,
                                               @Value("${migration.caseOutcomeGapsMigration.encoded-data-string}")
                                               String encodedDataString) {
        super(coreCaseDataService);
        this.encodedDataString = encodedDataString;
    }

    @Override
    public Map<String, Object> migrate(CaseDetails caseDetails) throws Exception {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            SscsCaseData caseData = getSscsCaseDataFrom(data);
            String caseId = caseDetails.getId().toString();
            String hearingRoute = caseDetails.getData().get("hearingRoute").toString();

            if (!hearingRoute.equalsIgnoreCase("gaps")) {
                log.info("Skipping case for case outcome migration. Case id: {} "
                             + "Reason: Hearing Route is not gaps it is {}",
                         caseId, hearingRoute);
                throw new Exception("Skipping case for case outcome migration. Hearing Route is not gaps");
            }

            if (caseData.getCaseOutcome().getCaseOutcome() == null) {
                log.info("Skipping case for case outcome migration. Case id: {} Reason: Case outcome is empty", caseId);
                throw new Exception("Skipping case for case outcome migration. Case outcome is empty");
            } else {
                resetCaseOutcome(data, caseId);
            }
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    public static void resetCaseOutcome(Map<String, Object> caseData, String caseId) {
        log.info("case outcome found with value {} and set to null for case id {}",
                 caseData.get("caseOutcome"), caseId);
        caseData.put("caseOutcome", null);
        log.info("did Po Attend found with value {} and set to null for case id {}",
                 caseData.get("didPoAttend"), caseId);
        caseData.put("didPoAttend", null);
        log.info("Completed migration for case outcome gaps migration. Case id: {}", caseId);
    }

    @Override
    public String getEventId() {
        return REMOVE_GAPS_OUTCOME_TAB_ID;
    }

    @Override
    public String getEventDescription() {
        return REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return REMOVE_GAPS_OUTCOME_TAB_SUMMARY;
    }
}
