package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.FinalDecisionOutcomeMigration.OUTCOME_MIGRATION_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.FinalDecisionOutcomeMigration.OUTCOME_MIGRATION_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FinalDecisionOutcomeMigrationTest {

    @Mock
    private HearingOutcomeService hearingOutcomeService;
    @Mock
    private HmcHearingsApiService hearingsApiService;

    private CaseDetails caseDetails;
    private SscsCaseData caseData;
    private FinalDecisionOutcomeMigration finalDecisionOutcomeMigration;

    @BeforeEach
    void setUp() {
        finalDecisionOutcomeMigration = new FinalDecisionOutcomeMigration(hearingsApiService,
                                                                          hearingOutcomeService,
                                                                          ENCODED_STRING);
        caseData = buildCaseData();
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(caseData)).id(1234L).build();
    }

    @Test
    @DisplayName("Should migrate when case doesn't have hearing outcomes")
    void shouldMigrate() {
        caseData.setOutcome("decisionUpheld");
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build());
        caseData.setHearings(List.of(Hearing.builder()
                                         .value(HearingDetails.builder().hearingId("1").build())
                                         .build()));
        caseDetails.setData(buildCaseDataMap(caseData));

        var caseHearing = CaseHearing.builder().hearingId(1L).build();
        when(hearingsApiService.getCompletedHearings(eq(caseDetails.getId().toString())))
            .thenReturn(List.of(caseHearing));

        HearingOutcome hearingOutcome = HearingOutcome.builder()
            .value(HearingOutcomeDetails.builder()
                       .completedHearingId("1")
                       .build())
            .build();
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(
            eq(caseHearing), eq(buildCaseDataMap(caseData)), eq("outcome")))
            .thenReturn(List.of(hearingOutcome));

        finalDecisionOutcomeMigration.migrate(caseDetails);

        assertThat(caseDetails.getData()).isNotNull();
        assertThat(caseDetails.getData().get("hearingOutcomes")).isNotNull();
        assertThat(caseDetails.getData().get("outcome")).isNull();
    }

    @Test
    @DisplayName("Should migrate when case already has hearing outcomes")
    void shouldMigrateWithHearingOutcomes() {
        String selectedHearingId = "333";
        caseData.setOutcome("decisionUpheld");
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build());
        caseData.setHearings(List.of(Hearing.builder()
                                         .value(HearingDetails.builder().hearingId(selectedHearingId).build())
                                         .build()));
        caseDetails.setData(buildCaseDataMap(caseData));

        var caseHearing = CaseHearing.builder().hearingId(1L).build();
        when(hearingsApiService.getCompletedHearings(eq(caseDetails.getId().toString())))
            .thenReturn(List.of(caseHearing));

        HearingOutcome hearingOutcome = HearingOutcome.builder()
            .value(HearingOutcomeDetails.builder()
                       .completedHearingId("1")
                       .build())
            .build();
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(
            eq(caseHearing), eq(buildCaseDataMap(caseData)), eq("outcome")))
            .thenReturn(List.of(hearingOutcome));

        finalDecisionOutcomeMigration.migrate(caseDetails);

        assertThat(caseDetails.getData()).isNotNull();
        assertThat(caseDetails.getData().get("hearingOutcomes")).isNotNull();
        assertThat(caseDetails.getData().get("outcome")).isNull();
    }

    @Test
    @DisplayName("Should throw exception when hearingRoute is not listAssist")
    void shouldThrowErrorWhenHearingRouteIsGaps() {
        caseData.getSchedulingAndListingFields().setHearingRoute(GAPS);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> finalDecisionOutcomeMigration.migrate(caseDetails))
            .hasMessageContaining("Hearing Route is not listAssist");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoOutcome() {
        caseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> finalDecisionOutcomeMigration.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists or outcome is empty");
    }

    @Test
    @DisplayName("Event details should be correct")
    void shouldReturnCorrectEventDetails() {
        assertThat(finalDecisionOutcomeMigration.getEventDescription()).isEqualTo(OUTCOME_MIGRATION_DESCRIPTION);
        assertThat(finalDecisionOutcomeMigration.getEventSummary()).isEqualTo(OUTCOME_MIGRATION_SUMMARY);
    }

    @Test
    @DisplayName("Source case field name and migration name should be correct")
    void shouldReturnCorrectSourceFieldAndMigrationName() {
        assertThat(finalDecisionOutcomeMigration.getOutcomeFieldName()).isEqualTo("outcome");
    }

    @Test
    @DisplayName("Source case fields should be reset to null")
    void shouldResetOutcomeFields() {
        caseData.setOutcome("someOutcome");
        caseDetails.setData(buildCaseDataMap(caseData));

        finalDecisionOutcomeMigration.resetOutcomeFields(caseDetails.getData(), caseDetails.getId().toString());

        assertThat(caseDetails.getData().get("outcome")).isNull();
    }
}
