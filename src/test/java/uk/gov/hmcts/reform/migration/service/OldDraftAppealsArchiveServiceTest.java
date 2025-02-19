package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.SixMonthsOldDraftsSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;

import java.util.List;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
class OldDraftAppealsArchiveServiceTest {

    @Mock
    private CoreCaseDataService coreCaseDataService;
    @Mock
    private SixMonthsOldDraftsSearchQuery searchQuery;
    @Mock
    private ElasticSearchRepository repository;

    private OldDraftAppealsArchiveService oldDraftAppealsArchiveService;

    @BeforeEach
    void setUp() {
        oldDraftAppealsArchiveService =
            new OldDraftAppealsArchiveService(coreCaseDataService, searchQuery, repository);
    }

    @Test
    void shouldReturnMigrationCases() {
        var caseA = CaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(5)).build();
        var caseB = CaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(6)).build();
        var caseC = CaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(7)).build();
        var caseD = CaseDetails.builder().id(1L).state("appealCreated").createdDate(now().minusMonths(6)).build();
        List<CaseDetails> caseList = List.of(caseA, caseB, caseC, caseD);
        when(repository.findCases(searchQuery)).thenReturn(caseList);

        List<CaseDetails> migrationCases = oldDraftAppealsArchiveService.getMigrationCases();

        assertThat(migrationCases).hasSize(2);
        assertThat(migrationCases).contains(caseB, caseC);
    }

    @Test
    void shouldReturnMigratedCaseData() throws Exception {
        var data = buildCaseDataMap(buildCaseData());
        var caseDetails = CaseDetails.builder().data(data).build();

        assertThat(oldDraftAppealsArchiveService.migrate(caseDetails)).isEqualTo(data);
    }

    @Test
    void shouldReturnEventId() {
        assertThat(oldDraftAppealsArchiveService.getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(oldDraftAppealsArchiveService.getEventSummary()).isEqualTo(EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(oldDraftAppealsArchiveService.getEventDescription()).isEqualTo(EVENT_DESCRIPTION);
    }
}
