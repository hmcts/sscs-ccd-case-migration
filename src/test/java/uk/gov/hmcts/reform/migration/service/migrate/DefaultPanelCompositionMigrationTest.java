package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.convertCaseDetailsToSscsCaseDetails;

@ExtendWith(MockitoExtension.class)
class DefaultPanelCompositionMigrationTest {

    @Mock
    private DefaultPanelCompositionQuery searchQuery;
    @Mock
    private ElasticSearchRepository repository;

    private DefaultPanelCompositionMigration underTest;

    @BeforeEach
    void setUp() {
        underTest =
            new DefaultPanelCompositionMigration(searchQuery, repository);
    }

    @Test
    void shouldReturnMigrationCases() {
        var caseA = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        var caseB = buildCaseWith("readyToList", HearingRoute.GAPS);
        var caseC = buildCaseWith("draft", HearingRoute.LIST_ASSIST);
        var caseD = buildCaseWith("validAppeal", HearingRoute.LIST_ASSIST);
        List<SscsCaseDetails> caseList = List.of(caseA, caseB, caseC, caseD);
        when(repository.findCases(searchQuery, true)).thenReturn(caseList);

        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(caseA);
    }

    @Test
    void shouldReturnMigratedCaseData() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().state(READY_TO_LIST.toString()).data(data).build();

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(60, convertCaseDetailsToSscsCaseDetails(caseDetails).getData()
            .getSchedulingAndListingFields().getOverrideFields().getDuration());
    }

    @Test
    void shouldNotResetOverrideFieldDuration() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build())
                .overrideFields(OverrideFields.builder().duration(90).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().state(READY_TO_LIST.toString()).data(data).build();

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(90, convertCaseDetailsToSscsCaseDetails(caseDetails).getData()
            .getSchedulingAndListingFields().getOverrideFields().getDuration());
    }

    @Test
    void shouldNotMigrateCaseIfNotReadyToList() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().state(HEARING.toString()).data(data).build();

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldReturnEventId() {
        assertThat(underTest.getEventId()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_ID);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(underTest.getEventSummary()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_SUMMARY);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(underTest.getEventDescription()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_DESCRIPTION);
    }

    private SscsCaseDetails buildCaseWith(String state, HearingRoute hearingRoute) {
        return SscsCaseDetails.builder()
            .id(1L).state(state)
            .data(SscsCaseData.builder()
                      .schedulingAndListingFields(
                          SchedulingAndListingFields.builder().hearingRoute(hearingRoute).build()
                      ).build()
            ).build();
    }
}
