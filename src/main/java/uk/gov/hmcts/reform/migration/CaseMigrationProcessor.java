package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class CaseMigrationProcessor implements DataMigrationService {

    public static final String LOG_STRING = "-----------------------------------------\n";

    @Autowired
    private IdamService idamService;
    @Autowired
    private UpdateCcdCaseService ccdUpdateService;

    @Getter
    private final List<Long> migratedCases = new ArrayList<>();

    @Getter
    private final List<Long> failedCases = new ArrayList<>();

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    public void migrateCases(String caseType) {
        validateCaseType(caseType);
        log.info("Data migration of cases started for case type: {}", caseType);
        ForkJoinPool threadPool = new ForkJoinPool(25);
        threadPool.submit(() -> getMigrationCases()
            .parallelStream()
            .limit(caseProcessLimit)
            .forEach(this::updateCase));
        shutdownThreadPool(threadPool);
        log.info("""
                {}Data migration completed
                {}
                Total number of processed cases: {}
                Total number of migrations performed: {}
                 {}
                """,
                LOG_STRING, LOG_STRING,
                getMigratedCases().size() + getFailedCases().size(), getMigratedCases().size(),
                LOG_STRING
        );

        log.info("Migrated cases: {}", getMigratedCases().isEmpty() ? "NONE" : getMigratedCases());
        log.info("Failed/Skipped Migrated cases: {}", getFailedCases().isEmpty() ? "NONE" : getFailedCases());
        log.info("Data migration of cases completed");
    }

    private void validateCaseType(String caseType) {
        if (!StringUtils.hasText(caseType)) {
            throw new CaseMigrationException("Provide case type for the migration");
        }

        if (caseType.split(",").length > 1) {
            throw new CaseMigrationException("Only One case type at a time is allowed for the migration");
        }
    }

    private void updateCase(SscsCaseDetails caseDetails) {
        if (accepts().test(caseDetails)) {
            Long caseId = caseDetails.getId();
            log.info("Updating case {}", caseId);
            try {
                log.debug("Case data: {}", caseDetails.getData());
                ccdUpdateService.updateCaseV2(caseId, getEventId(), idamService.getIdamTokens(), sscsCaseDetails -> {
                    migrate(sscsCaseDetails);
                    return new UpdateResult(getEventSummary(), getEventDescription());
                });
                log.info("Case {} successfully updated", caseId);
                migratedCases.add(caseId);
            } catch (Exception e) {
                log.error("Case {} update failed due to: {}", caseId, e.getMessage());
                failedCases.add(caseId);
            }
        } else {
            log.info("Case {} does not meet criteria for migration", caseDetails.getId());
        }
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
