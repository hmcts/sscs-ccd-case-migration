package uk.gov.hmcts.reform.migration.service.migrate;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.SentToDwpMigration.DATE_SENT_TO_DWP;
import static uk.gov.hmcts.reform.migration.service.migrate.SentToDwpMigration.HMCTS_DWP_STATE;
import static uk.gov.hmcts.reform.migration.service.migrate.SentToDwpMigration.SENT_TO_DWP_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.SentToDwpMigration.SENT_TO_DWP_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.SentToDwpMigration.SENT_TO_DWP_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

class SentToDwpMigrationTest {

    private static final int RESPONSE_DUE_DAYS = 28;
    private static final int RESPONSE_DUE_DAYS_CM = 42;

    private SentToDwpMigration sentToDwpMigration;
    private CaseDetails caseDetails;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        sentToDwpMigration = new SentToDwpMigration(ENCODED_STRING, RESPONSE_DUE_DAYS, RESPONSE_DUE_DAYS_CM);
        sscsCaseData = buildCaseData("test", "childSupport", "Child Support");
    }

    @Test
    @DisplayName("Should fetch cases from encoded string to migrate")
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = sentToDwpMigration.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    @DisplayName("Event details should be correct")
    void shouldReturnCorrectEventDetails() {
        assertEquals(SENT_TO_DWP_MIGRATION_EVENT_ID, sentToDwpMigration.getEventId());
        assertEquals(SENT_TO_DWP_MIGRATION_EVENT_SUMMARY, sentToDwpMigration.getEventSummary());
        assertEquals(SENT_TO_DWP_MIGRATION_EVENT_DESCRIPTION, sentToDwpMigration.getEventDescription());
    }

    @Test
    @DisplayName("Skip if there's no data in case details")
    void shouldThrowErrorWhenNoData() {
        caseDetails = CaseDetails.builder().data(null).id(1234L).build();

        assertThatThrownBy(() -> sentToDwpMigration.migrate(caseDetails))
            .hasMessageContaining("case data is empty");
    }

    @Test
    @DisplayName("Skip if dwpDueDate is null")
    void shouldThrowErrorWhenDwpDueDateIsNull() {
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(sscsCaseData)).id(1234L).build();

        assertThatThrownBy(() -> sentToDwpMigration.migrate(caseDetails))
            .hasMessageContaining("FTA response due date is empty");
    }

    @Test
    @DisplayName("Skip if dateSentToDwp is already set")
    void shouldThrowErrorWhenDateSentToDwpIsSet() {
        LocalDate sentToDwpDate = LocalDate.now().minusDays(15);
        sscsCaseData.setDateSentToDwp(sentToDwpDate.toString());
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(sscsCaseData)).id(1234L).build();

        assertThatThrownBy(() -> sentToDwpMigration.migrate(caseDetails))
            .hasMessageContaining(DATE_SENT_TO_DWP + " is already set");
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSupport", "UC", "attendanceAllowance"})
    @DisplayName("Should migrate case")
    void shouldMigrate(String benefitShortName) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(benefitShortName).build());
        LocalDate dueDate = LocalDate.now().plusDays(25);
        sscsCaseData.setDwpDueDate(dueDate.toString());
        sscsCaseData.setHmctsDwpState(null);
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(sscsCaseData)).id(1234L).build();

        sentToDwpMigration.migrate(caseDetails);

        assertNotNull(caseDetails.getData().get(HMCTS_DWP_STATE));
        assertNotNull(caseDetails.getData().get(DATE_SENT_TO_DWP));
        String expectedSentDate = benefitShortName.equals("childSupport")
            ? dueDate.minusDays(RESPONSE_DUE_DAYS_CM).toString() : dueDate.minusDays(RESPONSE_DUE_DAYS).toString();
        assertEquals(expectedSentDate, caseDetails.getData().get(DATE_SENT_TO_DWP));
    }

    @Test
    @DisplayName("Should migrate case and not overwrite hmctsDwpSate")
    void shouldMigrateAndKeepExistingHmctsDwpSate() {
        LocalDate dueDate = LocalDate.now().plusDays(13);
        sscsCaseData.setDwpDueDate(dueDate.toString());
        sscsCaseData.setHmctsDwpState("someState");
        caseDetails = CaseDetails.builder().data(buildCaseDataMap(sscsCaseData)).id(1234L).build();

        sentToDwpMigration.migrate(caseDetails);

        assertEquals("someState", caseDetails.getData().get(HMCTS_DWP_STATE));
    }
}
