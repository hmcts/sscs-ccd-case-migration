package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.PanelMemberCompositionRemovalMigration.CLEAR_PMC_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.PanelMemberCompositionRemovalMigration.CLEAR_PMC_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.PanelMemberCompositionRemovalMigration.CLEAR_PMC_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

class PanelMemberCompositionRemovalMigrationTest {
    private PanelMemberCompositionRemovalMigration underTest;

    @BeforeEach
    void setUp() {
        underTest = new PanelMemberCompositionRemovalMigration(ENCODED_STRING);
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    void shouldMigrate() {
        var caseData = buildCaseData();
        caseData.setPanelMemberComposition(PanelMemberComposition.builder()
                                               .panelCompositionDisabilityAndFqMember(List.of("50"))
                                               .build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().data(data).id(1234L).build();

        underTest.migrate(caseDetails);

        assertThat(caseDetails.getData().get("panelMemberComposition")).isNull();
    }

    @Test
    void shouldThrowException() {
        var caseData = buildCaseData();
        caseData.setPanelMemberComposition(PanelMemberComposition.builder().build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().data(data).id(1234L).build();

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldGetEventId() {
        assertThat(underTest.getEventId()).isEqualTo(CLEAR_PMC_ID);
    }

    @Test
    void getEventDescription() {
        assertThat(underTest.getEventDescription()).isEqualTo(CLEAR_PMC_DESCRIPTION);
    }

    @Test
    void getEventSummary() {
        assertThat(underTest.getEventSummary()).isEqualTo(CLEAR_PMC_SUMMARY);
    }
}
