package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.Map;

import static java.util.Objects.isNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.caseOutcomeGapsMigration.enabled", havingValue = "true")
public class CaseOutcomeGapsMigrationServiceImpl extends CaseOutcomeMigration {

    static final String REMOVE_GAPS_OUTCOME_TAB_ID = "removeGapsOutcomeTab";
    static final String REMOVE_GAPS_OUTCOME_TAB_SUMMARY = "Outcome tab removed as it is Gaps case";
    static final String REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION = "Outcome tab removed as it is Gaps case";

    public CaseOutcomeGapsMigrationServiceImpl(CoreCaseDataService coreCaseDataService,
                                               @Value("${migration.caseOutcomeGapsMigration.encoded-data-string}")
                                               String encodedDataString) {
        super(coreCaseDataService, null, encodedDataString);
    }

    @Override
    public String getMigrationRoute() {
        return HearingRoute.GAPS.toString();
    }

    boolean isMigrationNeeded(SscsCaseData caseData) {
        return isNull(caseData.getCaseOutcome().getCaseOutcome());
    }

    @Override
    void setHearingOutcome(Map<String, Object> data, SscsCaseData caseData, String caseId) {
        // do nothing
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
