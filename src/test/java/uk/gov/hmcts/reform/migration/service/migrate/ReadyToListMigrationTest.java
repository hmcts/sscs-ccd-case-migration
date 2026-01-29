package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
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
            ENCODED_STRING
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

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }



}
