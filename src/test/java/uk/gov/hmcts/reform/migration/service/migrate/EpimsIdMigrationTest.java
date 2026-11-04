package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;

class EpimsIdMigrationTest {

    private static final long CASE_ID = 1234L;
    private static final String OLD_ID = "206150";
    private static final String NEW_ID = "999994";
    private static final String OTHER_ID = "366559";
    private static final String HISTORY_TEXT = "Update Ayr venue address";

    private final EpimsIdMigration underTest = new EpimsIdMigration(ENCODED_STRING);

    @Test
    void shouldFetchCasesToMigrate() {
        final List<SscsCaseDetails> cases = underTest.fetchCasesToMigrate();

        assertThat(cases).containsExactly(
            SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build());
    }

    @Test
    void shouldAcceptFetchedCasesThatHaveNoCaseData() {
        final SscsCaseDetails fetchedCase = underTest.fetchCasesToMigrate().getFirst();

        assertThat(fetchedCase.getData()).isNull();
        assertThat(underTest.accepts().test(fetchedCase)).isTrue();
    }

    @Test
    void shouldReplaceOldEpimsIdInOverrideAndDefaultListingValues() {
        final Map<String, Object> caseData = ayrCaseData();
        caseData.put("overrideFields", listingValues(OLD_ID));
        caseData.put("defaultListingValues", listingValues(OLD_ID));

        underTest.migrate(caseDetails(caseData, "readyToList"));

        final Map<String, Object> expected = ayrCaseData();
        expected.put("overrideFields", listingValues(NEW_ID));
        expected.put("defaultListingValues", listingValues(NEW_ID));
        assertThat(caseData).isEqualTo(expected);
    }

    @Test
    void shouldReplaceOnlyOldEpimsIdAndLeaveOtherCaseDataUnchanged() {
        final Map<String, Object> overrideFields = listingValues(OTHER_ID, OLD_ID);
        overrideFields.put("duration", 60);
        final Map<String, Object> regionalProcessingCenter = new HashMap<>(Map.of("epimsId", OTHER_ID));
        final Map<String, Object> caseData = ayrCaseData();
        caseData.put("overrideFields", overrideFields);
        caseData.put("regionalProcessingCenter", regionalProcessingCenter);

        underTest.migrate(caseDetails(caseData, "readyToList"));

        final Map<String, Object> expectedOverrideFields = listingValues(OTHER_ID, NEW_ID);
        expectedOverrideFields.put("duration", 60);
        final Map<String, Object> expected = ayrCaseData();
        expected.put("overrideFields", expectedOverrideFields);
        expected.put("regionalProcessingCenter", Map.of("epimsId", OTHER_ID));
        assertThat(caseData).isEqualTo(expected);
    }

    @Test
    void shouldRecordHistoryWhenCaseHoldsNoStoredEpimsIds() {
        final Map<String, Object> caseData = ayrCaseData();

        final UpdateResult result = underTest.migrate(caseDetails(caseData, "withDwp"));

        assertThat(caseData).isEqualTo(ayrCaseData());
        assertThat(result.summary()).isEqualTo(HISTORY_TEXT);
        assertThat(result.description()).isEqualTo(HISTORY_TEXT);
    }

    @Test
    void shouldHandleListingValuesWithoutEpimsIds() {
        final Map<String, Object> caseData = ayrCaseData();
        caseData.put("overrideFields", new HashMap<>(Map.of("duration", 60)));

        underTest.migrate(caseDetails(caseData, "readyToList"));

        assertThat(caseData.get("overrideFields")).isEqualTo(Map.of("duration", 60));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dormantAppealState", "draftArchived", "hearing", "voidState", "withUT"})
    void shouldNotMigrateCaseInExcludedState(final String state) {
        final Map<String, Object> caseData = ayrCaseData();
        caseData.put("overrideFields", listingValues(OLD_ID));
        final CaseDetails caseDetails = caseDetails(caseData, state);

        assertThatThrownBy(() -> underTest.migrate(caseDetails))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Skipping Case 1234 because it is in state " + state);
        assertThat(caseData.get("overrideFields")).isEqualTo(listingValues(OLD_ID));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Glasgow", "AYR"})
    void shouldNotMigrateCaseAtAnotherVenue(final String venue) {
        final Map<String, Object> caseData = new HashMap<>();
        caseData.put("processingVenue", venue);
        caseData.put("overrideFields", listingValues(OLD_ID));
        final CaseDetails caseDetails = caseDetails(caseData, "readyToList");

        assertThatThrownBy(() -> underTest.migrate(caseDetails))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Skipping Case 1234 because the processing venue is " + venue);
        assertThat(caseData.get("overrideFields")).isEqualTo(listingValues(OLD_ID));
    }

    @Test
    void shouldUseMigrateCaseEvent() {
        assertThat(underTest.getEventId()).isEqualTo("migrateCase");
    }

    @Test
    void shouldWireFromApplicationPropertiesWhenEnabled() {
        contextRunner()
            .withPropertyValues("migration.processingEpimsId.enabled=true")
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EpimsIdMigration.class));
    }

    @Test
    void shouldBeDisabledByDefault() {
        contextRunner()
            .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(EpimsIdMigration.class));
    }

    private static ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(EpimsIdMigration.class)
            .withBean(CoreCaseDataService.class, () -> mock(CoreCaseDataService.class));
    }

    private static CaseDetails caseDetails(final Map<String, Object> caseData, final String state) {
        return CaseDetails.builder().id(CASE_ID).state(state).data(caseData).build();
    }

    private static Map<String, Object> ayrCaseData() {
        final Map<String, Object> caseData = new HashMap<>();
        caseData.put("processingVenue", "Ayr");
        return caseData;
    }

    private static Map<String, Object> listingValues(final String... epimsIds) {
        final List<Map<String, Object>> items = new ArrayList<>();
        Arrays.stream(epimsIds).forEach(epimsId -> {
            final Map<String, Object> inner = new HashMap<>(Map.of("value", epimsId));
            items.add(new HashMap<>(Map.of("id", "item-" + items.size(), "value", inner)));
        });
        final Map<String, Object> listingValues = new HashMap<>();
        listingValues.put("hearingVenueEpimsIds", items);
        return listingValues;
    }
}
