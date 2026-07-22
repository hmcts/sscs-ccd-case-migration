package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

        assertThat(migrationCases).hasSize(1).contains(migrationCase);
    }


    @Test
    void shouldPopulateConfidentialityFieldsWhenAppellantConfidentialityRequiredIsPresent() {
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
        assertThat(appellant).containsEntry("confidentialityRequirement", "Yes").containsKey("confidentialityRequired");
        assertThat(data).containsEntry("isConfidentialCase", "Yes");
    }

    @Test
    void shouldPopulateConfidentialityFieldsWhenOtherPartiesConfidentialityRequiredIsPresent() {
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
        assertThat(otherPartyValue)
            .containsEntry("confidentialityRequirement", "Yes")
            .containsKey("confidentialityRequired");
        assertThat(data).containsEntry("isConfidentialCase", "Yes");
    }

    @Test
    void shouldPopulateIsConfidentialCaseAsNoWhenNoConfidentialityIsRequired() {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "No");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data).containsEntry("isConfidentialCase", "No");
    }

    @Test
    void shouldPopulateIsConfidentialCaseAsNoWhenOtherPartyConfidentialityIsNo() {
        final Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "No");
        final Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        final Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "No");
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data).containsEntry("isConfidentialCase", "No");
    }

    @Test
    void shouldNotPopulateIsConfidentialCaseWhenAppellantIsAbsentAndOtherPartyConfidentialityIsNo() {
        final Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "No");
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data).doesNotContainKey("isConfidentialCase");
    }

    @Test
    void shouldNotPopulateIsConfidentialCaseWhenAppellantIsNoAndOtherPartyConfidentialityIsAbsent() {
        final Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "No");
        final Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        final Map<String, Object> otherPartyValue = new HashMap<>();
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data).doesNotContainKey("isConfidentialCase");
    }

    @Test
    void shouldThrowExceptionWhenOtherPartyConfidentialityRequiredIsAbsent() {
        final Map<String, Object> otherPartyValue = new HashMap<>();
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        final var exception = assertThrows(IllegalStateException.class,
                                           () -> confidentialityFlagMigration.migrate(caseDetails));

        assertThat(exception.getMessage())
            .isEqualTo(String.format(ConfidentialityFlagMigration.NO_CONFIDENTIALITY_MESSAGE, 123L));
        assertThat(otherPartyValue).doesNotContainKey("confidentialityRequirement");
    }

    @Test
    void shouldSkipOtherPartyWhenValueKeyIsAbsent() {
        final Map<String, Object> otherPartyWithoutValue = new HashMap<>();
        final Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "Yes");
        final Map<String, Object> otherPartyWithValue = new HashMap<>();
        otherPartyWithValue.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherPartyWithoutValue);
        otherParties.add(otherPartyWithValue);
        final Map<String, Object> data = new HashMap<>();
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        assertThrows(IllegalArgumentException.class, () -> confidentialityFlagMigration.migrate(caseDetails));

        assertThat(otherPartyValue).containsEntry("confidentialityRequirement", "Yes");
    }

    @Test
    void shouldNotUpdateAppellantWhenAppellantIsAbsent() {
        final Map<String, Object> appeal = new HashMap<>();
        final Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "Yes");
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        final var result = confidentialityFlagMigration.migrate(caseDetails);

        assertThat(result.summary()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
        assertThat(appeal.containsKey("appellant")).isFalse();
    }

    @Test
    void shouldNotUpdateAppellantWhenConfidentialityRequiredIsAbsent() {
        final Map<String, Object> appellant = new HashMap<>();
        final Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        final Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "Yes");
        final Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        final List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        final Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        data.put("otherParties", otherParties);
        final CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        final var result = confidentialityFlagMigration.migrate(caseDetails);

        assertThat(result.summary()).isEqualTo(CONFIDENTIALITY_FLAG_MIGRATION_EVENT_SUMMARY);
        assertThat(appellant.containsKey("confidentialityRequirement")).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {"childSupport, Child Support", "UC, Universal Credit"})
    void shouldHandleConfidentialityTabForUcCm(String code, String description) {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> benefitCode = new HashMap<>();
        benefitCode.put("code", code);
        benefitCode.put("description", description);
        appeal.put("benefitType", benefitCode);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);

        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data).containsKey("confidentialityTab");
    }

    @Test
    void shouldNotHandleConfidentialityTabWhenNotCmUc() {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> benefitCode = new HashMap<>();
        benefitCode.put("code", "taxCredit");
        benefitCode.put("description", "Tax Credit");
        appeal.put("benefitType", benefitCode);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);

        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data.containsKey("confidentialityTab")).isFalse();
    }


    @ParameterizedTest
    @CsvSource(value = {"null, Yes", "No, No"}, nullValues = "null")
    void shouldHandleUndeterminedConfidentialityForCm(String confidentialityRequired, String expectedResult) {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> benefitCode = new HashMap<>();
        benefitCode.put("code", "childSupport");
        benefitCode.put("description", "Child Support");
        appeal.put("benefitType", benefitCode);
        Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", confidentialityRequired);
        Map<String, Object> otherParty = new HashMap<>();
        otherParty.put("value", otherPartyValue);
        List<Map<String, Object>> otherParties = new ArrayList<>();
        otherParties.add(otherParty);
        Map<String, Object> data = new HashMap<>();
        data.put("otherParties", otherParties);
        data.put("appeal", appeal);

        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();

        confidentialityFlagMigration.migrate(caseDetails);

        assertThat(data)
            .containsKey("hasUndeterminedPartyConfidentiality")
            .containsEntry("hasUndeterminedPartyConfidentiality", expectedResult);
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

    @Test
    void shouldNotOverwriteAppellantConfidentialityRequirementIfAlreadyPresent() {
        Map<String, Object> appellant = new HashMap<>();
        appellant.put("confidentialityRequired", "No");
        appellant.put("confidentialityRequirement", "Yes");
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("appellant", appellant);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);
        CaseDetails caseDetails = CaseDetails.builder()
            .id(123L)
            .data(data)
            .build();
        assertThrows(IllegalStateException.class, () -> confidentialityFlagMigration.migrate(caseDetails));
    }

    @Test
    void shouldNotOverwriteOtherPartiesConfidentialityRequirementIfAlreadyPresent() {
        Map<String, Object> otherPartyValue = new HashMap<>();
        otherPartyValue.put("confidentialityRequired", "No");
        otherPartyValue.put("confidentialityRequirement", "Yes");
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
        assertThrows(IllegalStateException.class, () -> confidentialityFlagMigration.migrate(caseDetails));
    }

}
