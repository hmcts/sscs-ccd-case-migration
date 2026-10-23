package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.INVALID_STATE_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.STATES_TO_SKIP;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUES_TO_MIGRATE;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.VenueMigrationService.VENUE_MIGRATION_EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
class VenueMigrationServiceTest {

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

    @MethodSource("getVenuesToMigrate")
    @ParameterizedTest
    void shouldUpdateVenue(String venueToMigrate) {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)venueToMigrate));
        var caseDetails = CaseDetails.builder().data(caseData).build();
        when(roboticsJsonMapper.findVenueName(any(SscsCaseData.class))).thenReturn(Optional.of("Leeds"));

        var result = underTest.migrate(caseDetails);

        assertThat(caseData.get("processingVenue")).isEqualTo("Leeds");
        assertThat(result.summary()).isEqualTo(String.format(VENUE_MIGRATION_EVENT_SUMMARY, "Leeds"));
        assertThat(result.description()).isEqualTo(String.format(VENUE_MIGRATION_EVENT_DESCRIPTION, "Leeds"));
    }

    @MethodSource("getVenuesToMigrate")
    @ParameterizedTest
    void shouldThrowException(String venueToMigrate) {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object)venueToMigrate));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var sscsCaseData = SscsCaseData.builder().processingVenue(venueToMigrate).build();
        when(roboticsJsonMapper.findVenueName(eq(sscsCaseData))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }


    @MethodSource("getStatesToSkip")
    @ParameterizedTest
    void shouldSkipCaseInInvalidState(String state) {
        var caseData = new HashMap<>(Map.of("processingVenue", (Object) VENUES_TO_MIGRATE.getFirst()));
        var caseDetails = CaseDetails.builder().data(caseData).state(state).id(1234L).build();
        var exception = assertThrows(IllegalStateException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(INVALID_STATE_FAILURE_MSG, 1234L, state));
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

    static Stream<String> getVenuesToMigrate() {
        return VENUES_TO_MIGRATE.stream();
    }

    static Stream<String> getStatesToSkip() {
        return STATES_TO_SKIP.stream();
    }
}
