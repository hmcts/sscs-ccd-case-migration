package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.migrate.DwpDataMigrationServiceImpl;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.migrate.DwpDataMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.DwpDataMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.DwpDataMigrationServiceImpl.EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
public class DwpCaseMigrationProcessorTest {

    private static final IdamTokens tokens = IdamTokens.builder().build();
    private static final UpdateResult updateResult = new UpdateResult(EVENT_SUMMARY, EVENT_DESCRIPTION);

    @Mock
    private ForkJoinPool threadPool;
    @Mock
    private ElasticSearchRepository elasticSearchRepository;
    @Mock
    private CoreCaseDataService coreCaseDataService;

    @InjectMocks
    DwpDataMigrationServiceImpl caseMigrationProcessor;

    private final SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(1677777777L).jurisdiction("SSCS").build();
    private final List<SscsCaseDetails> caseList = List.of(caseDetails, SscsCaseDetails.builder().build());

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(caseMigrationProcessor, "caseProcessLimit", 1);
        ReflectionTestUtils.setField(caseMigrationProcessor, "coreCaseDataService", coreCaseDataService);
    }

    @Test
    public void shouldMigrateCasesOfACaseType() {
        setupMocks();

        caseMigrationProcessor.migrateCases();

        verify(coreCaseDataService, times(1))
            .applyUpdatesInCcd(eq(1677777777L), eq(EVENT_ID), any());
    }

    @Test
    public void shouldMigrateOnlyLimitedNumberOfCases() {
        setupMocks();

        caseMigrationProcessor.migrateCases();

        verify(coreCaseDataService)
                 .applyUpdatesInCcd(eq(1677777777L), eq(EVENT_ID), any());
    }

    @Test
    public void shutdownThreadPoolTimedOut() throws InterruptedException {
        when(threadPool.awaitTermination(anyLong(), any())).thenThrow(new InterruptedException());
        caseMigrationProcessor.shutdownThreadPool(threadPool);
        verify(threadPool, times(1)).shutdown();
    }

    private void setupMocks() {
        when(elasticSearchRepository.findCases(any(), anyBoolean())).thenReturn(caseList);
    }
}
