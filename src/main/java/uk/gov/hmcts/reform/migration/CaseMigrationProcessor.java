package uk.gov.hmcts.reform.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public abstract class CaseMigrationProcessor implements DataMigrationService<Map<String, Object>> {

    public static final String LOG_STRING = "-----------------------------------------\n";

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Getter
    private final List<Long> migratedCases = new ArrayList<>();

    @Getter
    private final List<Long> failedCases = new ArrayList<>();

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    public CaseMigrationProcessor(CoreCaseDataService coreCaseDataService) {
        this.coreCaseDataService = coreCaseDataService;
    }

    public void migrateCases(String caseType) {
        validateCaseType(caseType);
        log.info("Data migration of cases started for case type: {}", caseType);
        ForkJoinPool threadPool = new ForkJoinPool(25);
        threadPool.submit(() -> getMigrationCases()
            .parallelStream()
            .limit(caseProcessLimit)
            .forEach(caseDetails -> updateCase(caseType, caseDetails)));
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

    public SscsCaseData getSscsCaseDataFrom(Map<String, Object> data) {
        return new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(data, SscsCaseData.class);
    }

    private void validateCaseType(String caseType) {
        if (!StringUtils.hasText(caseType)) {
            throw new CaseMigrationException("Provide case type for the migration");
        }

        if (caseType.split(",").length > 1) {
            throw new CaseMigrationException("Only One case type at a time is allowed for the migration");
        }
    }

    private void updateCase(String caseType, CaseDetails caseDetails) {
        if (accepts().test(caseDetails)) {
            Long id = caseDetails.getId();
            log.info("Updating case {}", id);
            try {
                log.debug("Case data: {}", caseDetails.getData());
                coreCaseDataService.update(
                    caseType,
                    caseDetails.getId(),
                    caseDetails.getJurisdiction(),
                    this
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
