package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.migration.service.migrate.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseOutcomeGapsMigrationServiceImplTest {

    private CaseDetails caseDetails;
    private CaseOutcomeGapsMigrationServiceImpl caseOutcomeGapsMigrationService;

    @BeforeEach
    public void setUp() {
        SscsCaseData caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build())
            .build();
        caseDetails = CaseDetails.builder().id(1234L).data(buildCaseDataMap(caseData)).build();
        caseOutcomeGapsMigrationService = new CaseOutcomeGapsMigrationServiceImpl(null);
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().build();
        assertTrue(caseOutcomeGapsMigrationService.accepts().test(sscsCaseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(caseOutcomeGapsMigrationService.accepts().test(null));
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        caseDetails.setData(null);
        caseOutcomeGapsMigrationService.migrate(caseDetails);
        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeGapsMigration() {
        assertThat(REMOVE_GAPS_OUTCOME_TAB_ID).isEqualTo(caseOutcomeGapsMigrationService.getEventId());
        assertThat(REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION)
            .isEqualTo(caseOutcomeGapsMigrationService.getEventDescription());
        assertThat(REMOVE_GAPS_OUTCOME_TAB_SUMMARY).isEqualTo(caseOutcomeGapsMigrationService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        var caseData = caseDetails.getData();
        caseData.put("caseOutcome", "1234");
        caseData.put("didPoAttend", "Yes");
        caseDetails.setData(caseData);

        caseOutcomeGapsMigrationService.migrate(caseDetails);

        assertNotNull(caseData);
        assertNull(caseData.get("caseOutcome"));
        assertNull(caseData.get("didPoAttend"));
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNonGapsCase() {
        var caseData = buildCaseData();
        caseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for CaseOutcomeGapsMigrationServiceImpl migration. "
                                      + "Hearing Route is not gaps");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledForGapsCaseWithNoCaseOutcome() {
        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for CaseOutcomeGapsMigrationServiceImpl migration, "
                                      + "Hearing outcome already exists or caseOutcome is empty");
    }

    @Test
    void shouldReturnEmptyHearingList() {
        List<CaseHearing> result = caseOutcomeGapsMigrationService.getHearingsFromHmc("someCaseId");

        assertThat(result).isEmpty();
    }
}
