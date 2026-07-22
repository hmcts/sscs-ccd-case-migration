package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.interpreterLanguageCode.enabled", havingValue = "true")
public class InterpreterLanguageCodeMigration extends CaseMigrationProcessor {

    static final String INTERPRETER_MIGRATION_EVENT_ID = "migrateCase";
    static final String INTERPRETER_MIGRATION_EVENT_SUMMARY = "Correct MRD interpreter language code added";
    static final String INTERPRETER_MIGRATION_EVENT_DESCRIPTION = "Correct MRD interpreter language code added";
    static final String LANG_CODE = "code";
    static final List<String> HEARING_OPTIONS_LANG_CODE_PATH =
        List.of("appeal", "hearingOptions", "languagesList", "value");
    static final List<String> OVERRIDE_FIELDS_LANG_CODE_PATH =
        List.of("overrideFields", "appellantInterpreter", "interpreterLanguage", "value");
    static final List<String> DEFAULT_LISTING_LANG_CODE_PATH =
        List.of("defaultListingValues", "appellantInterpreter", "interpreterLanguage", "value");
    static final List<String> HEARING_OPTIONS_LANG_LIST_PARENT_PATH =
        List.of("appeal", "hearingOptions", "languagesList");
    static final List<String> OVERRIDE_FIELDS_LANG_LIST_PARENT_PATH =
        List.of("overrideFields", "appellantInterpreter", "interpreterLanguage");
    static final List<String> DEFAULT_LISTING_LANG_LIST_PARENT_PATH =
        List.of("defaultListingValues", "appellantInterpreter", "interpreterLanguage");
    static final Map<String, String> LANGUAGE_CODE_MAP = Map.of(
        "acc-fey", "kur-fey",
        "acc-hki", "zho-hok",
        "acc-kgc", "bnt-kic",
        "acc-mhh", "ara-mag",
        "acc-wol", "wol",
        "alb", "sqi",
        "cze", "ces",
        "ger", "deu",
        "isn", "ish",
        "phj", "phr"
    );

    private final String encodedDataString;

    public InterpreterLanguageCodeMigration(@Value("${migration.interpreterLanguageCode.encoded-data-string}")
                                            String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        Map<String, Object> data = caseDetails.getData();
        if (nonNull(data)) {
            log.info("Preparing language codes for case {}", caseDetails.getId());
            Map<String, Object> hearingOptionsLang = getParentField(data, HEARING_OPTIONS_LANG_CODE_PATH);
            String hearingOptionLangCode = getLangCode(hearingOptionsLang);

            Map<String, Object> overrideFieldLang = getParentField(data, OVERRIDE_FIELDS_LANG_CODE_PATH);
            String overrideFieldLangCode = getLangCode(overrideFieldLang);

            Map<String, Object> defaultListingLang = getParentField(data, DEFAULT_LISTING_LANG_CODE_PATH);
            String defaultListingLangCode = getLangCode(defaultListingLang);

            if (!LANGUAGE_CODE_MAP.containsKey(hearingOptionLangCode)
                && !LANGUAGE_CODE_MAP.containsKey(overrideFieldLangCode)
                && !LANGUAGE_CODE_MAP.containsKey(defaultListingLangCode)) {
                String skipMigrationMsg = "Skipping case for migration. Language codes are not included in configured "
                    + "migration codes: hearing options language code = \"" + hearingOptionLangCode
                    + "\", override fields language code = \"" + overrideFieldLangCode
                    + "\", default listing language code = \"" + defaultListingLangCode + "\"";
                throw new RuntimeException(skipMigrationMsg);
            }

            Map<String, Object> defaultListingLangListParent =
                getParentField(data, DEFAULT_LISTING_LANG_LIST_PARENT_PATH);
            Map<String, Object> overrideFieldLangListParent =
                getParentField(data, OVERRIDE_FIELDS_LANG_LIST_PARENT_PATH);
            Map<String, Object> hearingOptionLangListParent =
                getParentField(data, HEARING_OPTIONS_LANG_LIST_PARENT_PATH);

            updateLangCode(hearingOptionsLang,hearingOptionLangListParent,
                           hearingOptionLangCode, "languagesList in hearingOptions");
            updateLangCode(overrideFieldLang, overrideFieldLangListParent,
                           overrideFieldLangCode, "interpreterLanguage in overrideFields");
            updateLangCode(defaultListingLang, defaultListingLangListParent,
                           defaultListingLangCode, "interpreterLanguage in defaultListingValues");
        }

        log.info("Case {} was updated", caseDetails.getId());
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public String getEventId() {
        return INTERPRETER_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return INTERPRETER_MIGRATION_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return INTERPRETER_MIGRATION_EVENT_SUMMARY;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParentField(Map<String, Object> root, List<String> path) {
        if (root == null || path == null || path.isEmpty()) {
            log.warn("Root or path not provided to get parent field");
            return null;
        }

        Object currentField = root;

        for (String key : path) {
            if (!(currentField instanceof Map<?, ?> parentField)) {
                log.warn("Field \"{}\" is not a map on path {}", key, path);
                return null;
            }

            currentField = parentField.get(key);

            if (currentField == null) {
                log.warn("Field \"{}\" doesn't exist on path {}", key,  path);
                return null;
            }
        }

        return currentField instanceof Map<?, ?>
            ? (Map<String, Object>) currentField
            : null;
    }

    public void updateLangCode(Map<String, Object> parentField, Map<String, Object> langListParent,
                               String oldLangCode, String fieldName) {
        if (parentField != null && LANGUAGE_CODE_MAP.containsKey(oldLangCode)) {
            if (nonNull(langListParent)) {
                List<Map<String, Object>> langList = (List<Map<String, Object>>) langListParent.get("list_items");
                if (nonNull(langList)) {
                    langList.stream().filter(lang -> Objects.equals(lang.get(LANG_CODE), oldLangCode))
                        .forEach(lang -> lang.put(LANG_CODE, LANGUAGE_CODE_MAP.get(oldLangCode)));
                }
            }
            parentField.put(LANG_CODE, LANGUAGE_CODE_MAP.get(oldLangCode));
            log.info("Updated language code of field {} from \"{}\" to \"{}\"",
                     fieldName, oldLangCode, parentField.get(LANG_CODE));
        } else  {
            log.warn("Language code for field \"{}\" was NOT updated. Parent exists: {}. Current value: \"{}\"",
                     fieldName,
                     parentField != null,
                     oldLangCode.isEmpty() ? "EMPTY" : oldLangCode);
        }

    }

    public String getLangCode(Map<String, Object> parentField) {
        return (parentField != null && parentField.get(LANG_CODE) != null) ? (String) parentField.get(LANG_CODE) : "";
    }
}
