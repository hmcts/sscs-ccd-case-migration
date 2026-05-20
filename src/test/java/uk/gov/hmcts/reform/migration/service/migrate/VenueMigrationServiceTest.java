package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.INVALID_PROCESSING_VENUE_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.INVALID_STATE_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.STATES_TO_SKIP;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_TO_MIGRATE;

@ExtendWith(MockitoExtension.class)
class VenueMigrationServiceTest {

    private String venueAfterMigration = "Glasgow";

    @Mock
    private RoboticsJsonMapper roboticsJsonMapper;

    private VenueMigrationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new VenueMigrationService(ENCODED_STRING, roboticsJsonMapper);
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
        var caseData = new HashMap<>(Map.of("processingVenue", (Object) VENUE_TO_MIGRATE));
        var caseDetails = CaseDetails.builder().data(caseData).build();
        when(roboticsJsonMapper.findVenueName(any(SscsCaseData.class))).thenReturn(Optional.of(venueAfterMigration));
        underTest.migrate(caseDetails);
        assertThat(caseData.get("processingVenue")).isEqualTo(venueAfterMigration);
    }

    @Test
    void shouldThrowException() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)VENUE_TO_MIGRATE));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(FAILURE_MSG, 1234L));
    }

    @Test
    void shouldSkipCaseWithInvalidProcessingVenue() {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object) "invalidVenue"));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(IllegalStateException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(INVALID_PROCESSING_VENUE_FAILURE_MSG, 1234L, VENUE_TO_MIGRATE));
    }

    @Disabled("Disabled as we are not currently skipping any states")
    @ParameterizedTest
    @MethodSource("getStatesToSkip")
    void shouldSkipCaseInInvalidState(String state) {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object) VENUE_TO_MIGRATE));
        var caseDetails = CaseDetails.builder().data(caseData).state(state).id(1234L).build();
        var exception = assertThrows(IllegalStateException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(INVALID_STATE_FAILURE_MSG, 1234L, state));
    }

    static Stream<String> getStatesToSkip() {
        return STATES_TO_SKIP.stream();
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
