package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.query.WaElasticSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.wa.enabled", havingValue = "true")
public class WaDataMigrationServiceImpl extends CaseMigrationProcessor {

    private static final String EVENT_ID = "waCaseMigration";
    private static final String EVENT_SUMMARY = "Migrate case for WA";
    private static final String EVENT_DESCRIPTION = "Migrate case for WA";

    private final WaElasticSearchQuery elasticSearchQuery;
    private final ElasticSearchRepository repository;

    public WaDataMigrationServiceImpl(WaElasticSearchQuery elasticSearchQuery, ElasticSearchRepository repository) {
        this.elasticSearchQuery = elasticSearchQuery;
        this.repository = repository;
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            data.put("preWorkAllocation", "Yes");
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return repository.findCases(elasticSearchQuery, true);
    }

    public String getEventId() {
        return EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
