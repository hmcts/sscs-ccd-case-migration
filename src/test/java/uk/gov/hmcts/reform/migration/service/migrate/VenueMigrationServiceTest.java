package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
class VenueMigrationServiceTest {

    @Mock
    private RoboticsJsonMapper roboticsJsonMapper;
    @Mock
    private SscsCcdConvertService ccdConvertService;

    private VenueMigrationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new VenueMigrationService(ENCODED_STRING, ccdConvertService, roboticsJsonMapper);
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
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)"Bradford"));
        var caseDetails = CaseDetails.builder().data(caseData).build();
        var sscsCaseData = SscsCaseData.builder().processingVenue("Bradford").build();
        when(ccdConvertService.getCaseData(eq(caseData))).thenReturn(sscsCaseData);
        when(roboticsJsonMapper.findVenueName(eq(sscsCaseData))).thenReturn(Optional.of("Leeds"));

        underTest.migrate(caseDetails);

        assertThat(caseData.get("processingVenue")).isEqualTo("Leeds");
    }

    @Test
    void shouldThrowException() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)"Bradford"));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var sscsCaseData = SscsCaseData.builder().processingVenue("Bradford").build();
        when(ccdConvertService.getCaseData(eq(caseData))).thenReturn(sscsCaseData);
        when(roboticsJsonMapper.findVenueName(eq(sscsCaseData))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldGetEventId() {
        assertThat(underTest.getEventId()).isEqualTo(VENUE_MIGRATION_EVENT_ID);
    }

    @Test
    void shouldGetEventDescription() {
        assertThat(underTest.getEventDescription()).isEqualTo(VENUE_MIGRATION_EVENT_DESCRIPTION);
    }

    @Test
    void shouldGetEventSummary() {
        assertThat(underTest.getEventSummary()).isEqualTo(VENUE_MIGRATION_EVENT_SUMMARY);
    }
}
