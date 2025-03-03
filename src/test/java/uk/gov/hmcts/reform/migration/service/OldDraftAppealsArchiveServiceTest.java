package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.migration.query.OldDraftsSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.OldDraftAppealsArchiveService.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

@ExtendWith(MockitoExtension.class)
class OldDraftAppealsArchiveServiceTest {

    @Mock
    private OldDraftsSearchQuery searchQuery;
    @Mock
    private ElasticSearchRepository repository;

    private OldDraftAppealsArchiveService oldDraftAppealsArchiveService;

    @BeforeEach
    void setUp() {
        oldDraftAppealsArchiveService =
            new OldDraftAppealsArchiveService(searchQuery, repository);
    }

    @Test
    void shouldReturnMigrationCases() {
        var caseA = SscsCaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(5)).build();
        var caseB = SscsCaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(6)).build();
        var caseC = SscsCaseDetails.builder().id(1L).state("draft").createdDate(now().minusMonths(7)).build();
        var caseD = SscsCaseDetails.builder().id(1L).state("appealCreated").createdDate(now().minusMonths(6)).build();
        List<SscsCaseDetails> caseList = List.of(caseA, caseB, caseC, caseD);
        when(repository.findCases(searchQuery, false)).thenReturn(caseList);

        List<SscsCaseDetails> migrationCases = oldDraftAppealsArchiveService.getMigrationCases();

        assertThat(migrationCases).hasSize(2);
        assertThat(migrationCases).contains(caseB, caseC);
    }

    @Test
    void shouldReturnMigratedCaseData() {
        var data = buildCaseData();
        var caseDetails = SscsCaseDetails.builder().data(data).build();

        oldDraftAppealsArchiveService.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
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
