package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.service.migrate.ReadyToListMigration.CALLBACK_WARNING_FIELD;
import static uk.gov.hmcts.reform.migration.service.migrate.ReadyToListMigration.READY_TO_LIST_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.ReadyToListMigration.READY_TO_LIST_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.ReadyToListMigration.READY_TO_LIST_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;

@ExtendWith(MockitoExtension.class)
class ReadyToListMigrationTest {

    private ReadyToListMigration underTest;
    private static final ZoneId LONDON = ZoneId.of("Europe/London");


    @BeforeEach
    void setUp() {
        underTest = new ReadyToListMigration(
            "encodedA",
            "encodedB",
            "encodedC",
            "encodedD",
            "encodedE",
            "encodedF",
            "encodedG",
            "encodedH",
            "encodedI",
            "encodedJ"
        );
    }

    @Test
    void shouldSetIgnoreCallbackWarningsWhenStateIsResponseReceived() {
        var data = new HashMap<String, Object>();
        var caseDetails = CaseDetails.builder()
            .id(1234L)
            .state(RESPONSE_RECEIVED.toString())
            .data(data)
            .build();

        var result = underTest.migrate(caseDetails);

        assertThat(caseDetails.getData().get(CALLBACK_WARNING_FIELD)).isEqualTo("Yes");
        assertThat(result.summary()).isEqualTo(READY_TO_LIST_MIGRATION_EVENT_SUMMARY);
        assertThat(result.description()).isEqualTo(READY_TO_LIST_MIGRATION_EVENT_DESCRIPTION);
    }

    @Test
    void shouldThrowWhenStateIsNotResponseReceived() {
        var data = new HashMap<String, Object>();
        var caseDetails = CaseDetails.builder()
            .id(1234L)
            .state("someOtherState")
            .data(data)
            .build();

        RuntimeException ex = assertThrows(IllegalStateException.class, () -> underTest.migrate(caseDetails));

        assertThat(ex.getMessage()).contains("Skipping Case");
        assertThat(ex.getMessage()).contains("incorrect state");
        assertThat(ex.getMessage()).contains("someOtherState");
        assertThat(caseDetails.getData()).doesNotContainKey(CALLBACK_WARNING_FIELD);
    }

    @Test
    void shouldReturnEventId() {
        assertThat(underTest.getEventId()).isEqualTo(READY_TO_LIST_MIGRATION_EVENT_ID);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(underTest.getEventSummary()).isEqualTo(READY_TO_LIST_MIGRATION_EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(underTest.getEventDescription()).isEqualTo(READY_TO_LIST_MIGRATION_EVENT_DESCRIPTION);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/ready_to_list_migration_hours.csv", numLinesToSkip = 1)
    void shouldSelectCorrectEncodedStringBasedOnHour(int hour, String expected) {
        ReadyToListMigration migration = new ReadyToListMigration(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
        );

        Clock fixedClock = Clock.fixed(
            Instant.parse(String.format("2024-01-01T%02d:00:00Z", hour)),
            LONDON
        );
        ReflectionTestUtils.setField(migration, "clock", fixedClock);
        String result = migration.getEncodedString();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getEncodedString_shouldThrowOutsideConfiguredHours() {
        ReadyToListMigration migration = new ReadyToListMigration(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
        );

        Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T05:00:00Z"), LONDON);
        ReflectionTestUtils.setField(migration, "clock", fixedClock);

        RuntimeException ex = assertThrows(IllegalStateException.class, migration::getEncodedString);
        assertThat(ex.getMessage()).contains("Migration job not configured to run at 5");
    }


}
