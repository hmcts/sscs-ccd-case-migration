package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.REMOVE_GAPS_OUTCOME_TAB_SUMMARY;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseOutcomeGapsMigrationServiceImplTest {

    private SscsCaseDetails caseDetails;
    private CaseOutcomeGapsMigrationServiceImpl caseOutcomeGapsMigrationService;

    @BeforeEach
    public void setUp() {
        SscsCaseData caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build())
            .build();
        caseDetails = SscsCaseDetails.builder().id(1234L).data(caseData).build();
        caseOutcomeGapsMigrationService = new CaseOutcomeGapsMigrationServiceImpl(null);
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertThat(caseOutcomeGapsMigrationService.accepts().test(caseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(caseOutcomeGapsMigrationService.accepts().test(null)).isFalse();
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
        caseData.setCaseOutcome(CaseOutcome.builder().caseOutcome("1234").didPoAttend(YesNo.YES).build());
        caseDetails.setData(caseData);

        caseOutcomeGapsMigrationService.migrate(caseDetails);

        assertNotNull(caseData);
        assertNull(caseData.getCaseOutcome().getCaseOutcome());
        assertNull(caseData.getCaseOutcome().getDidPoAttend());
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNonGapsCase() {
        var caseData = caseDetails.getData();
        caseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        caseDetails.setData(caseData);

        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration. Hearing Route is not gaps");

    }

    @Test
    void shouldThrowErrorWhenMigrateCalledForGapsCaseWithNoCaseOutcome() {
        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration, "
                                      + "Hearing outcome already exists or Case outcome is empty");
    }
}
