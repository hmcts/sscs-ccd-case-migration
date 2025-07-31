package uk.gov.hmcts.reform.migration.service.migrate;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.JudgeOutcomeMigration.OUTCOME_MIGRATION_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.JudgeOutcomeMigration.OUTCOME_MIGRATION_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JudgeOutcomeMigrationTest {

    @Mock
    private HearingOutcomeService hearingOutcomeService;
    @Mock
    private HmcHearingsApiService hearingsApiService;

    private final Venue venue = Venue.builder().name("venue 1 name").build();
    private final LocalDateTime start = LocalDateTime.of(2024, 6, 30, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2024,6,30,13,0);

    private CaseDetails caseDetails;
    private SscsCaseData caseData;
    private JudgeOutcomeMigration judgeOutcomeMigration;

    @BeforeEach
    void setUp() {
        judgeOutcomeMigration = new JudgeOutcomeMigration(hearingsApiService, hearingOutcomeService, ENCODED_STRING);
        caseData = buildCaseData();
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(caseData)).id(1234L).build();
    }

    @Test
    void shouldMigrate() {
        String epims = "123456";
        caseData.setOutcome("decisionUpheld");
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build());
        caseData.setHearings(List.of(Hearing.builder().value(
                HearingDetails.builder()
                    .hearingId("1").epimsId(epims).venue(venue)
                    .hearingChannel(HearingChannel.FACE_TO_FACE).start(start).end(end)
                    .build()).build()));
        caseDetails.setData(buildCaseDataMap(caseData));

        var caseHearing = CaseHearing.builder().hearingId(1L)
            .hearingDaySchedule(List.of(
                HearingDaySchedule.builder().hearingVenueEpimsId(epims)
                    .hearingStartDateTime(start.minusHours(1))
                    .hearingEndDateTime(end.minusHours(1))
                    .build())
            )
            .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
            .build();
        when(hearingsApiService.getCompletedHearings(eq(caseDetails.getId().toString())))
            .thenReturn(List.of(caseHearing));

        HearingOutcome hearingOutcome = HearingOutcome.builder()
            .value(HearingOutcomeDetails.builder()
                       .completedHearingId("1")
                       .hearingOutcomeId("2208")
                       .epimsId(epims)
                       .venue(venue)
                       .hearingChannelId(HearingChannel.FACE_TO_FACE)
                       .hearingStartDateTime(start)
                       .hearingEndDateTime(end)
                       .build())
            .build();
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(
            eq(caseHearing), eq(buildCaseDataMap(caseData)), eq("outcome")))
            .thenReturn(List.of(hearingOutcome));

        judgeOutcomeMigration.migrate(caseDetails);

        assertThat(caseDetails.getData()).isNotNull();
        assertThat(List.of(hearingOutcome)).isEqualTo(caseDetails.getData().get("hearingOutcomes"));
        assertThat(caseDetails.getData().get("outcome")).isNull();
    }

    @Test
    @DisplayName("Should throw exception when hearingRoute is not listAssist")
    void shouldThrowErrorWhenHearingRouteIsGaps() {
        caseData.getSchedulingAndListingFields().setHearingRoute(GAPS);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> judgeOutcomeMigration.migrate(caseDetails))
            .hasMessageContaining("Hearing Route is not listAssist");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoOutcome() {
        caseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> judgeOutcomeMigration.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists or Outcome is empty");
    }

    @Test
    void shouldThrowWhenHearingOutcomeExists() {
        caseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        caseData.setOutcome("someOutcome");
        caseData.setHearingOutcomes(List.of(HearingOutcome.builder().build()));
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> judgeOutcomeMigration.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists or Outcome is empty");
    }

    @Test
    @DisplayName("Event details should be correct")
    void shouldReturnCorrectEventDetails() {
        assertThat(judgeOutcomeMigration.getEventDescription()).isEqualTo(OUTCOME_MIGRATION_DESCRIPTION);
        assertThat(judgeOutcomeMigration.getEventSummary()).isEqualTo(OUTCOME_MIGRATION_SUMMARY);
    }

    @Test
    @DisplayName("Source case field name and migration name should be correct")
    void shouldReturnCorrectSourceFieldAndMigrationName() {
        assertThat(judgeOutcomeMigration.getMigrationName()).isEqualTo("Outcome");
        assertThat(judgeOutcomeMigration.getOutcomeFieldName()).isEqualTo("outcome");
    }

    @Test
    @DisplayName("Source case fields should be reset to null")
    void shouldResetSourceCaseFields() {
        caseData.setOutcome("someOutcome");
        caseDetails.setData(buildCaseDataMap(caseData));

        judgeOutcomeMigration.resetSourceCaseFields(caseDetails.getData(), caseDetails.getId().toString());

        assertThat(caseDetails.getData().get("outcome")).isNull();
    }
}
