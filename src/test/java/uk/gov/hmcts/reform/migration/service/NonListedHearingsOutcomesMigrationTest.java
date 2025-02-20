package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDeprecatedFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.AWAITING_LISTING;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.HEARING_REQUESTED;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigration.CASE_OUTCOME_MIGRATION_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigration.CASE_OUTCOME_MIGRATION_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigration.CASE_OUTCOME_MIGRATION_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
public class NonListedHearingsOutcomesMigrationTest {

    @Mock
    private HearingOutcomeService hearingOutcomeService;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    private final Venue venue = Venue.builder().name("venue 1 name").build();
    private final String epims = "123456";
    private final String hearingOutcomeId = "2208";
    private final LocalDateTime start = LocalDateTime.of(2024, 6, 30, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2024, 6, 30, 13, 0);

    private final CaseDetails caseDetails = CaseDetails.builder().id(1234L).build();

    NonListedHearingsOutcomesMigration caseOutcomeMigrationService;

    @BeforeEach
    public void setUp() {
        caseOutcomeMigrationService =
            new NonListedHearingsOutcomesMigration(hmcHearingsApiService, hearingOutcomeService, null, null);
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertThat(caseOutcomeMigrationService.accepts().test(caseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(caseOutcomeMigrationService.accepts().test(null)).isFalse();
    }

    @Test
    void shouldSkipWhenDataIsNull() throws Exception {
        Map<String, Object> result = caseOutcomeMigrationService.migrate(caseDetails);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertThat(CASE_OUTCOME_MIGRATION_ID).isEqualTo(caseOutcomeMigrationService.getEventId());
        assertThat(CASE_OUTCOME_MIGRATION_DESCRIPTION).isEqualTo(caseOutcomeMigrationService.getEventDescription());
        assertThat(CASE_OUTCOME_MIGRATION_SUMMARY).isEqualTo(caseOutcomeMigrationService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() throws Exception {
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .jointParty(JointParty.builder().build())
            .sscsDeprecatedFields(SscsDeprecatedFields.builder().build())
            .pipSscsCaseData(SscsPipCaseData.builder().build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder().build())
            .build();

        var caseHearing = CaseHearing.builder().hearingId(1L)
            .hearingDaySchedule(List.of(
                HearingDaySchedule.builder().hearingVenueEpimsId(epims)
                    .hearingStartDateTime(start.minusHours(1))
                    .hearingEndDateTime(end.minusHours(1))
                    .build())
            )
            .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
            .hmcStatus(AWAITING_LISTING)
            .build();
        when(hmcHearingsApiService.getHearingsRequest(eq(caseDetails.getId().toString()), isNull()))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of(caseHearing)).build());
        HearingOutcome hearingOutcome = HearingOutcome.builder()
            .value(HearingOutcomeDetails.builder()
                       .completedHearingId("1")
                       .didPoAttendHearing(YesNo.YES)
                       .hearingOutcomeId(hearingOutcomeId)
                       .epimsId(epims)
                       .venue(venue)
                       .hearingChannelId(HearingChannel.FACE_TO_FACE)
                       .hearingStartDateTime(start)
                       .hearingEndDateTime(end)
                       .build())
            .build();
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(eq(caseHearing), eq(caseData)))
            .thenReturn(Map.of("hearingOutcomes", hearingOutcome));
        caseDetails.setData(buildCaseDataMap(caseData));

        Map<String, Object> result = caseOutcomeMigrationService.migrate(caseDetails);

        assertThat(result).isNotNull();
        assertThat(result.get("hearingOutcomes")).isEqualTo(Map.of("hearingOutcomes", hearingOutcome));
        assertThat(result.get("caseOutcome")).isNull();
        assertThat(result.get("didPoAttend")).isNull();
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithHearingOutcomeInData() {
        SscsCaseData caseData = buildCaseData();
        caseData.setHearingOutcomes(new ArrayList<>());
        caseData.getHearingOutcomes().add(HearingOutcome.builder().value(
            HearingOutcomeDetails.builder().completedHearingId("1").build()).build());
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoCaseOutcomeInData() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Case outcome is empty");

    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithMultipleHearings() {
        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(caseOutcome)
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .build();

        when(hmcHearingsApiService.getHearingsRequest(any(), any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(
                CaseHearing.builder().hearingId(1L).hmcStatus(HEARING_REQUESTED).build(),
                CaseHearing.builder().hearingId(2L).hmcStatus(AWAITING_LISTING).build()
            )).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Zero or More than one hearing found");

    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoHearings() {
        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(caseOutcome)
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .build();

        when(hmcHearingsApiService.getHearingsRequest(any(), any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of()).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration, Zero or More than one hearing found");
    }
}
