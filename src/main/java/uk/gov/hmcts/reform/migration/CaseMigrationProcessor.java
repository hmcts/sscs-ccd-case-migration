package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.CcdRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CaseMigrationProcessor {
    public static final String LOG_STRING = "-----------------------------------------\n";

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private DataMigrationService<Map<String, Object>> dataMigrationService;

    @Autowired
    private CcdRepository repository;

    @Autowired
    private IdamRepository idamRepository;

    @Getter
    private List<Long> migratedCases = new ArrayList<>();

    @Getter
    private List<Long> failedCases = new ArrayList<>();

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    public void migrateCases(String caseType) {
        validateCaseType(caseType);
        log.info("Data migration of cases started for case type: {}", caseType);
        List<CaseDetails> listOfCaseDetails = repository.findCases();
        ForkJoinPool threadPool = new ForkJoinPool(25);
        String userToken =  idamRepository.generateUserToken();
        threadPool.submit(() -> listOfCaseDetails.parallelStream()
                    .limit(caseProcessLimit)
                    .forEach(caseDetails -> updateCase(userToken, caseType, caseDetails)));
        shutdownThreadPool(threadPool);
        log.info("""
                {}Data migration completed\n{}
                Total number of processed cases: {}
                Total number of migrations performed: {}\n {}
                """,
                LOG_STRING, LOG_STRING,
                getMigratedCases().size() + getFailedCases().size(), getMigratedCases().size(),
                LOG_STRING
        );

        log.info("Migrated cases: {}", getMigratedCases().isEmpty() ? "NONE" : getMigratedCases());
        log.info("Migrated cases: {}", getFailedCases().isEmpty() ? "NONE" : getFailedCases());
        log.info("Data migration of cases completed");
    }

    public void shutdownThreadPool(ForkJoinPool threadPool) {
        threadPool.shutdown();
        log.info("Waiting for thread pool to terminate");
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.warn("Timed out waiting for thread pool to terminate");
            Thread.currentThread().interrupt();
        }
    }

    private void validateCaseType(String caseType) {
        if (!StringUtils.hasText(caseType)) {
            throw new CaseMigrationException("Provide case type for the migration");
        }

        if (caseType.split(",").length > 1) {
            throw new CaseMigrationException("Only One case type at a time is allowed for the migration");
        }
    }

    private void updateCase(String authorisation, String caseType, CaseDetails caseDetails) {
        if (dataMigrationService.accepts().test(caseDetails)) {
            Long id = caseDetails.getId();
            log.info("Updating case {}", id);
            try {
                log.debug("Case data: {}", caseDetails.getData());
                coreCaseDataService.update(
                    authorisation,
                    caseType,
                    caseDetails.getId(),
                    caseDetails.getJurisdiction()
                );
                log.info("Case {} successfully updated", id);
                migratedCases.add(id);
            } catch (Exception e) {
                log.error("Case {} update failed due to: {}", id, e.getMessage());
                failedCases.add(id);
            }
        } else {
            log.info("Case {} does not meet criteria for migration", caseDetails.getId());
        }
    }
}
