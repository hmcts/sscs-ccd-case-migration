package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;

import java.util.Map;

@Slf4j
@SpringBootApplication
@EnableRetry
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.migration.hmc", "uk.gov.hmcts.reform.sscs.client"})
@ComponentScan(basePackages = {"uk.gov.hmcts.reform", "uk.gov.hmcts.reform.sscs",
    "uk.gov.hmcts.reform.sscs.ccd.config"})
public class CaseMigrationRunner implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    public CaseMigrationRunner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            getMigrationJobs().forEach((name, job) -> {
                log.info("Running Migration job: {}", name);
                job.migrateCases();
            });
        } catch (Exception e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }

    public Map<String, DataMigrationService> getMigrationJobs() {
        return applicationContext.getBeansOfType(DataMigrationService.class);
    }

    @Bean
    public CcdRequestDetails getRequestDetails(
        @Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
        @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId) {
        return CcdRequestDetails.builder()
            .caseTypeId(coreCaseDataCaseTypeId)
            .jurisdictionId(coreCaseDataJurisdictionId)
            .build();
    }
}
