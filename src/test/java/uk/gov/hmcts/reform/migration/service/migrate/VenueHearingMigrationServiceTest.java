package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueHearingMigrationService.VENUE_MIGRATION_EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
public class VenueHearingMigrationServiceTest {

    @Mock
    private RoboticsJsonMapper roboticsJsonMapper;

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    private VenueHearingMigrationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new VenueHearingMigrationService(ENCODED_STRING, roboticsJsonMapper, hmcHearingsApiService);
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    void shouldUpdateVenue() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)"Poole"));
        var caseDetails = CaseDetails.builder().id(1L).data(caseData).build();
        when(roboticsJsonMapper.findVenueName(any(SscsCaseData.class))).thenReturn(Optional.of("Bournemouth"));
        CaseHearing hearing = CaseHearing.builder().hmcStatus(HmcStatus.AWAITING_LISTING).build();
        HearingsGetResponse hmcResponse = HearingsGetResponse.builder().caseHearings(List.of(hearing)).build();
        when(hmcHearingsApiService.getHearingsRequest(any(), any())).thenReturn(hmcResponse);
        UpdateResult updateResult = underTest.migrate(caseDetails);
        assertThat(updateResult.summary()).isEqualTo(VENUE_MIGRATION_EVENT_SUMMARY);
        assertThat(caseData.get("processingVenue")).isEqualTo("Bournemouth");
    }

    @Test
    void shouldThrowExceptionWhenVenueNotFound() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)"Poole"));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var sscsCaseData = SscsCaseData.builder().processingVenue("Poole").build();
        when(roboticsJsonMapper.findVenueName(eq(sscsCaseData))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldThrowExceptionWhenInvalidHearingFound() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)"Poole"));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        when(roboticsJsonMapper.findVenueName(any(SscsCaseData.class))).thenReturn(Optional.of("Bournemouth"));
        CaseHearing hearing = CaseHearing.builder().hmcStatus(HmcStatus.ADJOURNED).build();
        HearingsGetResponse hmcResponse = HearingsGetResponse.builder().caseHearings(List.of(hearing)).build();
        when(hmcHearingsApiService.getHearingsRequest(any(), any())).thenReturn(hmcResponse);

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldReturnEventId() {
        assertThat(underTest.getEventId()).isEqualTo("venueHearingMigration");
    }

}
