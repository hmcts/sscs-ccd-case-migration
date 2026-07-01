package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.ConfidentialityFlagMigration.CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.ConfidentialityFlagMigration.CONFIDENTIALITY_FLAG_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.ConfidentialityFlagMigration.CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;

class ConfidentialityFlagMigrationTest {

    private ConfidentialityFlagMigration confidentialityFlagMigration;

    @BeforeEach
    void setUp() {
        confidentialityFlagMigration = new ConfidentialityFlagMigration(ENCODED_STRING);
    }

    @Test
    void shouldReturnEventId() {
        assertThat(confidentialityFlagMigration.getEventId()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_ID);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(confidentialityFlagMigration.getEventSummary())
            .isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(confidentialityFlagMigration.getEventDescription())
            .isEqualTo(CONFIDENTIALITY_FLAG_EVENT_DESCRIPTION);
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder()
            .id(ENCODED_CASE_ID)
            .jurisdiction("SSCS")
            .build();
        List<SscsCaseDetails> migrationCases = confidentialityFlagMigration.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }


    @Test
    void shouldMigrateWhenAppellantConfidentialityRequiredIsPresent() {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        var result = confidentialityFlagMigration.migrate(caseDetails);

        assertThat(result.summary()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
        assertThat(appellant.get("confidentialityRequirement")).isEqualTo("Yes");
        assertThat(appellant.containsKey("confidentialityRequired")).isFalse();
    }

    @Test
    void shouldMigrateWhenOtherPartiesConfidentialityRequiredIsPresent() {
        Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "Yes");
        Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        Map<String, Object> data = new HashMap<>();
        data.put("otherParties", otherParties);
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        var result = confidentialityFlagMigration.migrate(caseDetails);

        assertThat(result.summary()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
        assertThat(otherPartyValue.get("confidentialityRequirement")).isEqualTo("Yes");
        assertThat(otherPartyValue.containsKey("confidentialityRequired")).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenNoConfidentialityFieldsPresent() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(new HashMap<>())
            .build();

        var exception = assertThrows(IllegalStateException.class,
                                     () -> confidentialityFlagMigration.migrate(caseDetails));

        assertThat(exception.getMessage())
            .isEqualTo(String.format(ConfidentialityFlagMigration.NO_CONFIDENTIALITY_MESSAGE, 123L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"draftArchived", "voidState"})
    void shouldThrowExceptionWhenCaseIsVoid(String state) {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .state(state)
            .build();

        var exception = assertThrows(IllegalStateException.class,
                                     () -> confidentialityFlagMigration.migrate(caseDetails));

        assertThat(exception.getMessage())
            .isEqualTo(String.format(ConfidentialityFlagMigration.STATE_FAILURE_MSG, 123L, state));
    }

    @Test
    void shouldThrowExceptionWhenCaseIsDormantAndOlderThan6Months() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .state(DORMANT_APPEAL_STATE.toString())
            .lastModified(LocalDateTime.now().minusMonths(7))
            .build();

        var exception = assertThrows(IllegalStateException.class,
                                     () -> confidentialityFlagMigration.migrate(caseDetails));

        assertThat(exception.getMessage())
            .isEqualTo(String.format(ConfidentialityFlagMigration.DATE_FAILURE_MESSAGE, 123L));
    }

    @Test
    void shouldMigrateWhenCaseIsDormantAndLessThan6Months() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        data.put("appeal", appeal);
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .state(DORMANT_APPEAL_STATE.toString())
            .lastModified(LocalDateTime.now().minusMonths(5))
            .data(data)
            .build();

        var result = confidentialityFlagMigration.migrate(caseDetails);
        assertThat(result.summary()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
    }
}
