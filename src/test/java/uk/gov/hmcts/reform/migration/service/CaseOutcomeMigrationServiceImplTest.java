package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseOutcomeMigrationServiceImplTest {

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
        assertThat(EVENT_ID).isEqualTo(caseOutcomeMigrationService.getEventId());
        assertThat(EVENT_DESCRIPTION).isEqualTo(caseOutcomeMigrationService.getEventDescription());
        assertThat(EVENT_SUMMARY).isEqualTo(caseOutcomeMigrationService.getEventSummary());
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
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("Hearing outcome already exists");
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
            .build();
        when(hmcHearingsApiService.getHearingsRequest(any(),any()))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of(
                CaseHearing.builder().hearingId(1L).build(),
                CaseHearing.builder().hearingId(2L).build()
            )).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("More than one completed hearing found");
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNoHearings() {
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build())
            .build();
        when(hmcHearingsApiService.getHearingsRequest(any(),any()))
            .thenReturn(HearingsGetResponse.builder().caseHearings(List.of()).build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> caseOutcomeMigrationService.migrate(caseDetails))
            .hasMessageContaining("No completed hearings found");
    }
}
