package uk.gov.hmcts.reform.migration.service.migrate;

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
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDeprecatedFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.CANCELLED;
import static uk.gov.hmcts.reform.domain.hmc.HmcStatus.HEARING_REQUESTED;
import static uk.gov.hmcts.reform.migration.service.migrate.CaseOutcomeMigration.CASE_OUTCOME_MIGRATION_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.CaseOutcomeMigration.CASE_OUTCOME_MIGRATION_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.NonListedHearingsOutcomesMigration.NON_LISTED_OUTCOME_TAB_SUMMARY;
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
    private final String hearingOutcomeId = "2208";
    private final LocalDateTime start = LocalDateTime.of(2024, 6, 30, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2024, 6, 30, 13, 0);

    private final CaseDetails caseDetails = CaseDetails.builder().id(1234L).build();

    NonListedHearingsOutcomesMigration caseOutcomeMigrationService;

    @BeforeEach
    public void setUp() {
        caseOutcomeMigrationService =
            new NonListedHearingsOutcomesMigration(hmcHearingsApiService, hearingOutcomeService, null);
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().build();
        assertThat(caseOutcomeMigrationService.accepts().test(sscsCaseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(caseOutcomeMigrationService.accepts().test(null)).isFalse();
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        caseOutcomeMigrationService.migrate(caseDetails);
        assertThat(caseDetails.getData()).isNull();
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertThat(CASE_OUTCOME_MIGRATION_ID).isEqualTo(caseOutcomeMigrationService.getEventId());
        assertThat(CASE_OUTCOME_MIGRATION_DESCRIPTION).isEqualTo(caseOutcomeMigrationService.getEventDescription());
        assertThat(NON_LISTED_OUTCOME_TAB_SUMMARY).isEqualTo(caseOutcomeMigrationService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        String epims = "123456";
        var caseHearing = CaseHearing.builder().hearingId(1L)
                .hearingDaySchedule(List.of(
                        HearingDaySchedule.builder().hearingVenueEpimsId(epims)
                                .hearingStartDateTime(start.minusHours(1))
                                .hearingEndDateTime(end.minusHours(1))
                                .build())
                )
                .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
                .hmcStatus(CANCELLED)
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
        var data = buildCaseDataMap(SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .jointParty(JointParty.builder().build())
            .sscsDeprecatedFields(SscsDeprecatedFields.builder().build())
            .pipSscsCaseData(SscsPipCaseData.builder().build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder().build())
            .build());
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(eq(caseHearing), eq(data), eq("caseOutcome")))
            .thenReturn(List.of(hearingOutcome));
        caseDetails.setData(data);

        caseOutcomeMigrationService.migrate(caseDetails);

        assertNotNull(caseDetails.getData());
        assertThat(data.get("hearingOutcomes")).isEqualTo(List.of(hearingOutcome));
        assertNull(data.get("caseOutcome"));
        assertNull(data.get("didPoAttend"));
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
                CaseHearing.builder().hearingId(2L).hmcStatus(HEARING_REQUESTED).build()
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
            .hasMessageContaining("Skipping case for Case outcome migration, Zero or More than one hearing found");
    }
}
