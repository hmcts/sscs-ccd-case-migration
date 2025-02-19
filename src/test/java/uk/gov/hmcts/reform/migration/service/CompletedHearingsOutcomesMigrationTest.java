package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.CompletedHearingsOutcomesMigration.CASE_OUTCOME_MIGRATION_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CompletedHearingsOutcomesMigration.CASE_OUTCOME_MIGRATION_ID;
import static uk.gov.hmcts.reform.migration.service.CompletedHearingsOutcomesMigration.CASE_OUTCOME_MIGRATION_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CompletedHearingsOutcomesMigrationTest {

    @Mock
    private HearingOutcomeService hearingOutcomeService;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    private final Venue venue = Venue.builder().name("venue 1 name").build();
    private final String hearingOutcomeId = "2208";
    private final LocalDateTime start = LocalDateTime.of(2024,6,30,10,0);
    private final LocalDateTime end = LocalDateTime.of(2024,6,30,13,0);

    private final  CaseDetails caseDetails = CaseDetails.builder().id(1234L).build();

    CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService;

    @BeforeEach
    public void setUp() {
        caseOutcomeMigrationService =
            new CaseOutcomeMigrationServiceImpl(null, hmcHearingsApiService, null);
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
        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        String epims = "123456";
        List<Hearing> hearings = List.of(
            Hearing.builder().value(HearingDetails.builder()
                                        .hearingId("1")
                                        .epimsId(epims)
                                        .venue(venue)
                                        .hearingChannel(HearingChannel.FACE_TO_FACE)
                                        .start(start)
                                        .end(end)
                                        .build()).build()
        );
        SscsCaseData caseData = SscsCaseData.builder().caseOutcome(caseOutcome).hearings(hearings).build();
        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        Map<String, Object> result = caseOutcomeMigrationService.migrate(caseDetails);

        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .jointParty(JointParty.builder().build())
            .sscsDeprecatedFields(SscsDeprecatedFields.builder().build())
            .pipSscsCaseData(SscsPipCaseData.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
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
            .build();
        when(hmcHearingsApiService.getHearingsRequest(eq(caseDetails.getId().toString()),eq(HmcStatus.COMPLETED)))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of(caseHearing)).build());

        assertThat(result).isNotNull();
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

        caseDetails.setData(data);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(caseDetails);

        assertThat(result).isNotNull();
        assertThat(result.get("hearingOutcomes")).isEqualTo(Map.of("hearingOutcomes", hearingOutcome));
        assertThat(result.get("caseOutcome")).isNull();
        assertThat(result.get("didPoAttend")).isNull();
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithHearingOutcomeInData() {
        SscsCaseData caseData = buildCaseData();
        caseData.setHearingOutcomes(List.of(
            HearingOutcome.builder().value(
                HearingOutcomeDetails.builder().completedHearingId("1").build()).build()
        ));
        caseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists");
    }

    @Test
    void shouldThrowErrorWhenGapsCaseIsProcessed() {
        SscsCaseData caseData = buildCaseData();
        caseData.getSchedulingAndListingFields().setHearingRoute(GAPS);

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        caseDetails.setData(data);

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(data, caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration. Hearing Route is not list assist");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoCaseOutcomeInData() {
        caseDetails.setData(buildCaseDataMap(buildCaseData()));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Case outcome is empty");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithMultipleHearings() {
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .build();
        when(hmcHearingsApiService.getHearingsRequest(any(),any()))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of(
                CaseHearing.builder().hearingId(1L).build(),
                CaseHearing.builder().hearingId(2L).build()
            )).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Zero or More than one hearing found");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoHearings() {
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(LIST_ASSIST).build())
            .build();
        when(hmcHearingsApiService.getHearingsRequest(any(),any()))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of()).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration, Zero or More than one hearing found");
    }
}
