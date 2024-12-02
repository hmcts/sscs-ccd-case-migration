package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeMigrationServiceImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseOutcomeMigrationServiceImplTest {

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;



    private final Venue venue = Venue.builder().name("venue 1 name").build();
    private final String epims = "123456";
    private final String hearingOutcomeId = "2208";
    private final LocalDateTime start = LocalDateTime.of(2024,6,30,10,00);
    private final LocalDateTime end = LocalDateTime.of(2024,6,30,13,00);

    private final  CaseDetails caseDetails = CaseDetails.builder()
        .id(1234L)
        .build();

    CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
        new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertThat(caseOutcomeMigrationService.accepts().test(caseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(caseOutcomeMigrationService.accepts().test(null)).isFalse();
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        Map<String, Object> result = caseOutcomeMigrationService.migrate(null, null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertThat(EVENT_ID).isEqualTo(caseOutcomeMigrationService.getEventId());
        assertThat(EVENT_DESCRIPTION).isEqualTo(caseOutcomeMigrationService.getEventDescription());
        assertThat(EVENT_SUMMARY).isEqualTo(caseOutcomeMigrationService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {

        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        List<Hearing> hearings = new ArrayList<>();
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
                                                       .hearingId("1")
                                                       .epimsId(epims)
                                                       .venue(venue)
                                                       .hearingChannel(HearingChannel.FACE_TO_FACE)
                                                       .start(start)
                                                       .end(end)
                                                       .build()).build();
        hearings.add(hearing1);

        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(caseOutcome)
            .hearings(hearings)
            .build();

        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
            new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(data, caseDetails);
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
    void shouldSkipWhenMigrateCalledWithNoCaseOutcomeInData() {
        SscsCaseData caseData = buildCaseData();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
            new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(data, caseDetails);
        assertThat(result).isNotNull();
        assertThat(result.get("hearingOutcomes")).isNull();
        assertThat(result.get("caseOutcome")).isNull();
        assertThat(result.get("didPoAttend")).isNull();

    }

    @Test
    void shouldSkipWhenMigrateCalledWithMultipleHearings() {
        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(caseOutcome)
            .build();

        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of(
                CaseHearing.builder().hearingId(1L).build(),
                CaseHearing.builder().hearingId(2L).build()
            )).build());

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
            new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(data, caseDetails);
        assertThat(result).isNotNull();
        assertThat(result.get("hearingOutcomes")).isNull();
        assertThat(result.get("caseOutcome")).isEqualTo(hearingOutcomeId);
        assertThat(result.get("didPoAttend")).isEqualTo(YES);
    }

    @Test
    void shouldSkipWhenMigrateCalledWithNoHearings() {
        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome(hearingOutcomeId).didPoAttend(YesNo.YES).build();
        SscsCaseData caseData = SscsCaseData.builder()
            .caseOutcome(caseOutcome)
            .build();

        when(hmcHearingsApiService.getHearingsRequest(any(),any())).thenReturn(
            HearingsGetResponse.builder().caseHearings(List.of()).build());

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeMigrationServiceImpl caseOutcomeMigrationService =
            new CaseOutcomeMigrationServiceImpl(hmcHearingsApiService);
        Map<String, Object> result = caseOutcomeMigrationService.migrate(data, caseDetails);
        assertThat(result).isNotNull();
        assertThat(result.get("hearingOutcomes")).isNull();
        assertThat(result.get("caseOutcome")).isEqualTo(hearingOutcomeId);
        assertThat(result.get("didPoAttend")).isEqualTo(YES);
    }
}
