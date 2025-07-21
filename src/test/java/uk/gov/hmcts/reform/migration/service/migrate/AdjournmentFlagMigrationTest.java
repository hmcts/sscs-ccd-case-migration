package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.AdjournmentFlagMigration.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.AdjournmentFlagMigration.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.AdjournmentFlagMigration.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;


public class AdjournmentFlagMigrationTest {

    private AdjournmentFlagMigration adjournmentFlagMigration;

    @BeforeEach
    void setUp() {
        adjournmentFlagMigration = new AdjournmentFlagMigration(ENCODED_STRING);
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = adjournmentFlagMigration.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    void shouldMigrateCaseWhenAdjournmentFlagIsSetToYes() {
        var caseData = buildCaseData();
        caseData.setAdjournment(Adjournment.builder().adjournmentInProgress(YES).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();
        adjournmentFlagMigration.migrate(caseDetails);
        assertThat(data.get("adjournmentInProgress")).isEqualTo("No");
    }

    @Test
    void shouldNotMigrateCaseWhenAdjournmentFlagIsSetToNo() {
        var caseData = buildCaseData();
        caseData.setAdjournment(Adjournment.builder().adjournmentInProgress(NO).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();
        assertThrows(RuntimeException.class, () -> adjournmentFlagMigration.migrate(caseDetails));
    }

    @Test
    void shouldNotMigrateCaseWhenAdjournmentFlagIsNotPresent() {
        var caseData = buildCaseData();
        caseData.setAdjournment(Adjournment.builder().build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();
        assertThrows(RuntimeException.class, () -> adjournmentFlagMigration.migrate(caseDetails));
    }


    @Test
    void shouldReturnEventId() {
        assertThat(adjournmentFlagMigration.getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(adjournmentFlagMigration.getEventSummary()).isEqualTo(EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(adjournmentFlagMigration.getEventDescription()).isEqualTo(EVENT_DESCRIPTION);
    }

}
