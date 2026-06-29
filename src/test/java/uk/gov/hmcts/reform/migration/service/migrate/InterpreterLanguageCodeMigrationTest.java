package uk.gov.hmcts.reform.migration.service.migrate;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.HEARING_OPTIONS_LANG_CODE_PATH;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.INTERPRETER_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.InterpreterLanguageCodeMigration.INTERPRETER_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.convertCaseDetailsToSscsCaseDetails;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Slf4j
class InterpreterLanguageCodeMigrationTest {
    private static final String ENCODED_STRING = "";
    private static final String CASE_REFERENCE = "12332445";
    private final String ALBANIAN_LEGACY_CODE = "alb";
    private final String ALBANIAN_MRD_CODE = "sqi";
    private final String SPANISH_MRD_CODE = "sqi";
    private final DynamicList ALBANIAN_LEGACY = new DynamicList(
        new DynamicListItem(ALBANIAN_LEGACY_CODE, "Albanian"), Collections.emptyList());
    private final DynamicList ALBANIAN_MRD = new DynamicList(
        new DynamicListItem(ALBANIAN_MRD_CODE, "Albanian"), Collections.emptyList());
    private final DynamicList SPANISH = new DynamicList(
        new DynamicListItem(SPANISH_MRD_CODE, "Spanish"), Collections.emptyList());
    private InterpreterLanguageCodeMigration interpreterLanguageCodeMigration;
    private CaseDetails caseDetails;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        interpreterLanguageCodeMigration = new InterpreterLanguageCodeMigration(ENCODED_STRING);
        caseData = buildCaseData();
        caseData.getAppeal()
            .setHearingOptions(HearingOptions.builder()
                                   .languagesList(ALBANIAN_LEGACY)
                                   .languages(ALBANIAN_LEGACY.getValue().getLabel())
                                   .wantsSupport("No")
                                   .wantsToAttend("Yes")
                                   .scheduleHearing("No")
                                   .languageInterpreter("Yes")
                                   .build());
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(caseData)).id(parseLong(CASE_REFERENCE)).build();
    }

    @Test
    @DisplayName("Event details should be correct")
    void shouldReturnCorrectEventDetails() {
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
    void shouldMigrateToMRDLanguageCodesWhenLegacyCodeIsConfigured() {
        caseData.getSchedulingAndListingFields()
            .setOverrideFields(OverrideFields.builder()
                                   .duration(75)
                                   .appellantInterpreter(HearingInterpreter.builder()
                                                             .interpreterLanguage(ALBANIAN_LEGACY)
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

        assertEquals(ALBANIAN_MRD_CODE, migratedcase.getData().getAppeal().getHearingOptions().getLanguagesList().getValue().getCode());
        assertEquals(ALBANIAN_MRD_CODE, migratedcase.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter().getInterpreterLanguage().getValue().getCode());
        assertNull(migratedcase.getData().getSchedulingAndListingFields().getDefaultListingValues().getAppellantInterpreter().getInterpreterLanguage());
    }

    @Test
    @DisplayName("Should skip migration if none of language codes are legacy")
    void shouldSkipMigrationIfNoneOfLanguagesAreLegacy() {
        caseData.getAppeal()
            .setHearingOptions(HearingOptions.builder()
                                   .languagesList(ALBANIAN_MRD)
                                   .languages(ALBANIAN_MRD.getValue().getLabel())
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
                                                                   .interpreterLanguage(SPANISH)
                                                                   .isInterpreterWanted(YesNo.YES)
                                                                   .build())
                                         .build());
        caseDetails.setData(buildCaseDataMap(caseData));

        assertThatThrownBy(() -> interpreterLanguageCodeMigration.migrate(caseDetails))
                    .hasMessageContaining("Skipping case for migration. Language codes are not included in configured migration codes:");
    }

    @Test
    @DisplayName("Should return parent field")
    void shouldReturnParentField() {
        Map<String, Object> caseDataMap = buildCaseDataMap(caseData);
        assertEquals(Map.of("code", "alb", "label", "Albanian"), InterpreterLanguageCodeMigration.getParentField(caseDataMap, HEARING_OPTIONS_LANG_CODE_PATH));
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
                       .getParentField(Map.of("appeal", Map.of("hearingOptions", "Not HearingOptions")), HEARING_OPTIONS_LANG_CODE_PATH));
    }
}
