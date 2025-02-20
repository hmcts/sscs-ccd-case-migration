package uk.gov.hmcts.reform.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.DwpDataMigrationServiceImpl;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DwpCaseMigrationProcessorTest {

    private static final String CASE_TYPE = "Test_Case_Type";

    @Mock
    private CoreCaseDataService coreCaseDataService;
    @Mock
    private ForkJoinPool threadPool;
    @Mock
    private ElasticSearchRepository elasticSearchRepository;

    @InjectMocks
    DwpDataMigrationServiceImpl caseMigrationProcessor;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(caseMigrationProcessor, "caseProcessLimit", 1);
    }

    @Test
    public void shouldMigrateCasesOfACaseType() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().id(1677777777L).build();
        when(elasticSearchRepository.findCases(any())).thenReturn(List.of(caseDetails));
        when(coreCaseDataService
                 .update(CASE_TYPE, caseDetails.getId(), caseDetails.getJurisdiction(), caseMigrationProcessor))
            .thenReturn(caseDetails);

        caseMigrationProcessor.migrateCases(CASE_TYPE);

        verify(coreCaseDataService, times(1))
            .update(CASE_TYPE, caseDetails.getId(), caseDetails.getJurisdiction(), caseMigrationProcessor);
    }

    @Test
    public void shouldMigrateOnlyLimitedNumberOfCases() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().id(1677777777L).jurisdiction("SSCS").build();
        List<CaseDetails> caseList = List.of(caseDetails, CaseDetails.builder().build());
        when(elasticSearchRepository.findCases(any())).thenReturn(caseList);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCases(null);
        Assertions.assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(CASE_TYPE, 1677777777L, "SSCS", caseMigrationProcessor))
            .thenReturn(caseDetails);

        caseMigrationProcessor.migrateCases(CASE_TYPE);

        verify(coreCaseDataService, times(1))
            .update(CASE_TYPE, 1677777777L, "SSCS", caseMigrationProcessor);
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
}
