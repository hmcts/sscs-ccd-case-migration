package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.domain.common.CaseOutcomeMap;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
public class HearingOutcomeServiceTest {

    @Mock
    private VenueService venueService;

    HearingOutcomeService hearingOutcomeService;

    @BeforeEach
    public void setUp() {
        hearingOutcomeService = new HearingOutcomeService(venueService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"caseOutcome", "outcome"})
    public void shouldReturnEmptyHearingOutcome(String outcomeSource) {

        when(venueService.getVenueDetailsForActiveVenueByEpimsId(isNull()))
            .thenReturn(null);
        var hmcHearing = CaseHearing.builder().hearingId(123456789L).build();
        var caseData = SscsCaseData.builder().caseOutcome(CaseOutcome.builder().build()).build();
        var expectedHearingOutcome = HearingOutcome.builder().value(
            HearingOutcomeDetails.builder()
                .completedHearingId("123456789")
                .hearingStartDateTime(null)
                .hearingEndDateTime(null)
                .hearingOutcomeId(null)
                .didPoAttendHearing(null)
                .hearingChannelId(null)
                .venue(Venue.builder().build())
                .epimsId(null)
                .build()
        ).build();

        var hearingOutcomes = hearingOutcomeService.mapHmcHearingToHearingOutcome(
            hmcHearing, buildCaseDataMap(caseData), outcomeSource);

        assertEquals(expectedHearingOutcome, hearingOutcomes.getFirst());
    }

    @Test
    public void shouldReturnDetailedHearingOutcome() {

        when(venueService.getVenueDetailsForActiveVenueByEpimsId(eq("epimsId")))
            .thenReturn(VenueDetails.builder()
                            .venAddressLine1("Windsor Castle")
                            .venAddressTown("Windsor")
                            .epimsId("epimsId")
                            .build());
        var hmcHearing = CaseHearing.builder()
            .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
            .hearingDaySchedule(
                List.of(HearingDaySchedule.builder()
                            .hearingStartDateTime(LocalDateTime.parse("2024-06-30T10:00:00"))
                            .hearingEndDateTime(LocalDateTime.parse("2024-06-30T11:00:00"))
                            .hearingVenueEpimsId("epimsId").build()))
            .hearingId(123456789L).build();
        var caseData = SscsCaseData.builder()
            .caseOutcome(CaseOutcome.builder().caseOutcome("outcome").didPoAttend(YesNo.YES).build())
            .build();
        var expectedHearingOutcome = HearingOutcome.builder().value(
            HearingOutcomeDetails.builder()
                .completedHearingId("123456789")
                .hearingStartDateTime(LocalDateTime.parse("2024-06-30T11:00:00"))
                .hearingEndDateTime(LocalDateTime.parse("2024-06-30T12:00:00"))
                .hearingOutcomeId("outcome")
                .didPoAttendHearing(YesNo.YES)
                .hearingChannelId(HearingChannel.FACE_TO_FACE)
                .venue(Venue.builder().address(
                    Address.builder().line1("Windsor Castle").town("Windsor").build()
                ).build())
                .epimsId("epimsId")
                .build()
        ).build();

        var hearingOutcomes = hearingOutcomeService.mapHmcHearingToHearingOutcome(
            hmcHearing, buildCaseDataMap(caseData), "caseOutcome");

        assertEquals(expectedHearingOutcome, hearingOutcomes.getFirst());
    }

    @Test
    @DisplayName("Should create a Hearing Outcome and map the correct Outcome value to Case Outcome value")
    void shouldReturnNewHearingOutcomeWithCorrectOutcomeMapping() {
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(eq("epimsId")))
            .thenReturn(VenueDetails.builder()
                            .venAddressLine1("Windsor Castle")
                            .venAddressTown("Windsor")
                            .epimsId("epimsId")
                            .build());
        var hmcHearing = CaseHearing.builder()
            .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
            .hearingDaySchedule(
                List.of(HearingDaySchedule.builder()
                            .hearingStartDateTime(LocalDateTime.parse("2024-06-30T10:00:00"))
                            .hearingEndDateTime(LocalDateTime.parse("2024-06-30T11:00:00"))
                            .hearingVenueEpimsId("epimsId").build()))
            .hearingId(123456789L).build();
        var caseData = SscsCaseData.builder()
            .outcome(CaseOutcomeMap.DECISION_UPHELD.getOutcomeKey())
            .build();
        var expectedHearingOutcome = HearingOutcome.builder().value(
            HearingOutcomeDetails.builder()
                .completedHearingId("123456789")
                .hearingStartDateTime(LocalDateTime.parse("2024-06-30T11:00:00"))
                .hearingEndDateTime(LocalDateTime.parse("2024-06-30T12:00:00"))
                .hearingOutcomeId(CaseOutcomeMap.DECISION_UPHELD.getCaseOutcomeCode())
                .hearingChannelId(HearingChannel.FACE_TO_FACE)
                .venue(Venue.builder().address(
                    Address.builder().line1("Windsor Castle").town("Windsor").build()
                ).build())
                .epimsId("epimsId")
                .build()
        ).build();

        var hearingOutcomes = hearingOutcomeService.mapHmcHearingToHearingOutcome(
            hmcHearing, buildCaseDataMap(caseData), "outcome");

        assertEquals(expectedHearingOutcome, hearingOutcomes.getFirst());
    }

    @Test
    @DisplayName("Should throw exception when Outcome value cannot be mapped to Case outcome")
    void shouldThrowWhenOutcomeNotMapped() {
        var hmcHearing = CaseHearing.builder()
            .hearingChannels(List.of(HearingChannel.FACE_TO_FACE))
            .hearingDaySchedule(
                List.of(HearingDaySchedule.builder()
                            .hearingStartDateTime(LocalDateTime.parse("2024-06-30T10:00:00"))
                            .hearingEndDateTime(LocalDateTime.parse("2024-06-30T11:00:00"))
                            .hearingVenueEpimsId("epimsId").build()))
            .hearingId(123456789L).build();
        var caseData = SscsCaseData.builder()
            .outcome("outcome")
            .build();
        assertThatThrownBy(() -> hearingOutcomeService.mapHmcHearingToHearingOutcome(
            hmcHearing, buildCaseDataMap(caseData), "outcome"))
            .hasMessageContaining("No match found for outcome value");
    }
}
