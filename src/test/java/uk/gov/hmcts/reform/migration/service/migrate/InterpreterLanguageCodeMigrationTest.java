package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.HEARING_OPTIONS_LANG_CODE_PATH;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.INTERPRETER_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.INTERPRETER_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.INTERPRETER_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.convertCaseDetailsToSscsCaseDetails;

@Slf4j
class InterpreterLanguageCodeMigrationTest {
    private final String albanianLegacyCode = "alb";
    private final String albanianMrdCode = "sqi";
    private final String spanishMrdCode = "sqi";
    private final DynamicList albanianLegacy = new DynamicList(
        new DynamicListItem(albanianLegacyCode, "Albanian"),
        List.of(new DynamicListItem(albanianLegacyCode, "Albanian")));
    private final DynamicList albanianMrd = new DynamicList(
        new DynamicListItem(albanianMrdCode, "Albanian"), Collections.emptyList());
    private final DynamicList spanish = new DynamicList(
        new DynamicListItem(spanishMrdCode, "Spanish"), Collections.emptyList());
    private InterpreterLanguageCodeMigration interpreterLanguageCodeMigration;
    private CaseDetails caseDetails;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        interpreterLanguageCodeMigration = new InterpreterLanguageCodeMigration(ENCODED_STRING);
        caseData = buildCaseData();
        caseData.getAppeal()
            .setHearingOptions(HearingOptions.builder()
                                   .languagesList(albanianLegacy)
                                   .languages(albanianLegacy.getValue().getLabel())
                                   .wantsSupport("No")
                                   .wantsToAttend("Yes")
                                   .scheduleHearing("No")
                                   .languageInterpreter("Yes")
                                   .build());
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(caseData)).id(1234L).build();
    }

    @Test
    @DisplayName("Event details should be correct")
    void shouldReturnCorrectEventDetails() {
        assertEquals(INTERPRETER_MIGRATION_EVENT_ID, interpreterLanguageCodeMigration.getEventId());
        assertEquals(INTERPRETER_MIGRATION_EVENT_DESCRIPTION, interpreterLanguageCodeMigration.getEventDescription());
        assertEquals(INTERPRETER_MIGRATION_EVENT_SUMMARY, interpreterLanguageCodeMigration.getEventSummary());
    }

    @Test
    @DisplayName("Should skip migration when data is null")
    void shouldSkipMigrationWhenDataIsNull() {
        caseDetails.setData(null);
        interpreterLanguageCodeMigration.migrate(caseDetails);
        assertNull(caseDetails.getData());
    }

    @Test
    @DisplayName("Should migrate to MRD language codes when legacy code is configured")
    void shouldMigrateToMrdLanguageCodesWhenLegacyCodeIsConfigured() {
        caseData.getSchedulingAndListingFields()
            .setOverrideFields(OverrideFields.builder()
                                   .duration(75)
                                   .appellantInterpreter(HearingInterpreter.builder()
                                                             .interpreterLanguage(albanianLegacy)
                                                             .isInterpreterWanted(YesNo.YES)
                                                             .build())
                                   .build());
        caseData.getSchedulingAndListingFields()
            .setDefaultListingValues(OverrideFields.builder()
                                         .duration(75)
                                         .appellantInterpreter(HearingInterpreter.builder()
                                                                   .interpreterLanguage(null)
                                                                   .isInterpreterWanted(YesNo.NO)
                                                                   .build())
                                         .build());
        caseDetails.setData(buildCaseDataMap(caseData));

        interpreterLanguageCodeMigration.migrate(caseDetails);

        SscsCaseDetails migratedcase = convertCaseDetailsToSscsCaseDetails(caseDetails);

        assertEquals(albanianMrdCode,
                     migratedcase.getData().getAppeal().getHearingOptions().getLanguagesList().getValue().getCode());
        assertEquals(albanianMrdCode, migratedcase.getData().getSchedulingAndListingFields().getOverrideFields()
            .getAppellantInterpreter().getInterpreterLanguage().getValue().getCode());
        assertNull(migratedcase.getData().getSchedulingAndListingFields().getDefaultListingValues()
                       .getAppellantInterpreter().getInterpreterLanguage());
        assertThat(migratedcase.getData().getAppeal().getHearingOptions().getLanguagesList().getListItems()
                       .stream().noneMatch(lang -> Objects.equals(lang.getCode(), albanianLegacyCode))).isTrue();
        assertThat(migratedcase.getData().getAppeal().getHearingOptions().getLanguagesList().getListItems()
                       .stream().anyMatch(lang -> Objects.equals(lang.getCode(), albanianMrdCode))).isTrue();
    }

    @Test
    @DisplayName("Should skip migration if none of language codes are legacy")
    void shouldSkipMigrationIfNoneOfLanguagesAreLegacy() {
        caseData.getAppeal()
            .setHearingOptions(HearingOptions.builder()
                                   .languagesList(albanianMrd)
                                   .languages(albanianMrd.getValue().getLabel())
                                   .wantsSupport("No")
                                   .wantsToAttend("Yes")
                                   .scheduleHearing("No")
                                   .languageInterpreter("Yes")
                                   .build());
        caseData.getSchedulingAndListingFields()
            .setOverrideFields(OverrideFields.builder()
                                   .duration(75)
                                   .appellantInterpreter(HearingInterpreter.builder()
                                                             .interpreterLanguage(null)
                                                             .build())
                                   .build());
        caseData.getSchedulingAndListingFields()
            .setDefaultListingValues(OverrideFields.builder()
                                         .duration(75)
                                         .appellantInterpreter(HearingInterpreter.builder()
                                                                   .interpreterLanguage(spanish)
                                                                   .isInterpreterWanted(YesNo.YES)
                                                                   .build())
                                         .build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> interpreterLanguageCodeMigration.migrate(caseDetails))
            .hasMessageContaining("Skipping case for migration. Language codes are not included in "
                                      + "configured migration codes:");
    }

    @Test
    @DisplayName("Should return parent field")
    void shouldReturnParentField() {
        Map<String, Object> caseDataMap = buildCaseDataMap(caseData);
        assertEquals(Map.of("code", "alb", "label", "Albanian"),
                     InterpreterLanguageCodeMigration.getParentField(caseDataMap, HEARING_OPTIONS_LANG_CODE_PATH));
    }

    @Test
    @DisplayName("Should return null when root or path to parent field is empty")
    void shouldReturnNullWhenRootOrPathToParentFieldIsEmpty() {
        Map<String, Object> caseDataMap = buildCaseDataMap(caseData);
        assertNull(InterpreterLanguageCodeMigration.getParentField(null, HEARING_OPTIONS_LANG_CODE_PATH));
        assertNull(InterpreterLanguageCodeMigration.getParentField(caseDataMap, null));
        assertNull(InterpreterLanguageCodeMigration.getParentField(caseDataMap, List.of()));
    }

    @Test
    @DisplayName("Should return null when path to parent field is broken")
    void shouldReturnNullWhenPathToParentFieldIsBroken() {
        Map<String, Object> caseDataMap = buildCaseDataMap(caseData);
        assertNull(InterpreterLanguageCodeMigration
                       .getParentField(caseDataMap, List.of("defaultListingValues", "appellantInterpreter", "value")));
        assertNull(InterpreterLanguageCodeMigration
                       .getParentField(Map.of("appeal", Map.of("hearingOptions", "Not HearingOptions")),
                                       HEARING_OPTIONS_LANG_CODE_PATH));
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = interpreterLanguageCodeMigration.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }
}
