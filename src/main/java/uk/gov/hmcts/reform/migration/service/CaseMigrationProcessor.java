package uk.gov.hmcts.reform.migration.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class CaseMigrationProcessor implements DataMigrationService {

    private static final String LOG_STRING = """
        \n-----------------------------------------
        Data migration completed
        -----------------------------------------
        Total number of processed cases: {}
        Total number of migrations performed: {}
        -----------------------------------------
        Migrated cases: {}
        Failed/Skipped Migrated cases: {}
        -----------------------------------------
        """;

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Getter
    private final List<Long> migratedCases = new ArrayList<>();

    @Getter
    private final List<Long> failedCases = new ArrayList<>();

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    public void migrateCases() {
        log.info("Data migration of cases started");

        ForkJoinPool threadPool = new ForkJoinPool(25);
        threadPool.submit(
            () -> fetchCasesToMigrate()
                .parallelStream()
                .limit(caseProcessLimit)
                .forEach(caseDetails -> {
                    if (accepts().test(caseDetails)) {
                        log.info("Updating case {}", caseDetails.getId());
                        try {
                            log.debug("Case data: {}", caseDetails.getData());
                            coreCaseDataService.applyUpdatesInCcd(
                                caseDetails.getId(),
                                getEventId(),
                                this::migrate
                            );
                            migratedCases.add(caseDetails.getId());
                            log.info("Case {} successfully updated", caseDetails.getId());
                        } catch (Exception e) {
                            failedCases.add(caseDetails.getId());
                            log.error("Case {} update failed due to: {}", caseDetails.getId(), e.getMessage());
                        }
                    } else {
                        log.info("Case {} does not meet criteria for migration", caseDetails.getId());
                    }
                })
        );
        shutdownThreadPool(threadPool);

        log.info(
            LOG_STRING, getMigratedCases().size() + getFailedCases().size(),
            getMigratedCases().size(), getMigratedCases().isEmpty() ? "NONE" : getMigratedCases(),
            getFailedCases().isEmpty() ? "NONE" : getFailedCases()
        );
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
}
