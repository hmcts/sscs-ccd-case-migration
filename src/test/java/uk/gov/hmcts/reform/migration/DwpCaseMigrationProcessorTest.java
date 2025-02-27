package uk.gov.hmcts.reform.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.DwpDataMigrationServiceImpl;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.DwpDataMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.DwpDataMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.DwpDataMigrationServiceImpl.EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
public class DwpCaseMigrationProcessorTest {

    private static final String CASE_TYPE = "Test_Case_Type";
    private static final IdamTokens tokens = IdamTokens.builder().build();
    private static final UpdateResult updateResult = new UpdateResult(EVENT_SUMMARY, EVENT_DESCRIPTION);

    @Mock
    private UpdateCcdCaseService ccdUpdateService;
    @Mock
    private ForkJoinPool threadPool;
    @Mock
    private ElasticSearchRepository elasticSearchRepository;
    @Mock
    private IdamService idamService;

    @InjectMocks
    DwpDataMigrationServiceImpl caseMigrationProcessor;

    private final SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(1677777777L).jurisdiction("SSCS").build();
    private final List<SscsCaseDetails> caseList = List.of(caseDetails, SscsCaseDetails.builder().build());

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(caseMigrationProcessor, "caseProcessLimit", 1);
        ReflectionTestUtils.setField(caseMigrationProcessor, "idamService", idamService);
        ReflectionTestUtils.setField(caseMigrationProcessor, "ccdUpdateService", ccdUpdateService);
    }

    @Test
    public void shouldMigrateCasesOfACaseType() {
        setupMocks();

        caseMigrationProcessor.migrateCases(CASE_TYPE);

        verify(ccdUpdateService, times(1))
            .updateCaseV2(eq(1677777777L), eq(EVENT_ID), eq(tokens), any());
    }

    @Test
    public void shouldMigrateOnlyLimitedNumberOfCases() {
        setupMocks();

        caseMigrationProcessor.migrateCases(CASE_TYPE);

        verify(ccdUpdateService)
                 .updateCaseV2(eq(1677777777L), eq(EVENT_ID), eq(tokens), any());
    }

    @Test
    public void shouldThrowExceptionWhenCaseTypeNull() {
        assertThrows(CaseMigrationException.class, () -> caseMigrationProcessor.migrateCases(null));
    }

    @Test
    public void shouldThrowExceptionWhenMultipleCaseTypesPassed() {
        assertThrows(CaseMigrationException.class, () ->
            caseMigrationProcessor.migrateCases("Cast_Type1,Cast_Type2"));
    }

    @Test
    public void shutdownThreadPoolTimedOut() throws InterruptedException {
        when(threadPool.awaitTermination(anyLong(), any())).thenThrow(new InterruptedException());
        caseMigrationProcessor.shutdownThreadPool(threadPool);
        verify(threadPool, times(1)).shutdown();
    }

    private void setupMocks() {
        when(elasticSearchRepository.findCases(any())).thenReturn(caseList);
        when(idamService.getIdamTokens()).thenReturn(tokens);
        when(ccdUpdateService
                 .updateCaseV2(eq(1677777777L), eq(EVENT_ID), eq(tokens), any()))
            .thenReturn(updateResult.sscsCaseDetails());
    }
}
