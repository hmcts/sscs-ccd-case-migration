package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.query.WaElasticSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;

import java.util.List;
import java.util.Map;

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

    public WaDataMigrationServiceImpl(CoreCaseDataService coreCaseDataService,
                                      WaElasticSearchQuery elasticSearchQuery, ElasticSearchRepository repository) {
        super(coreCaseDataService);
        this.elasticSearchQuery = elasticSearchQuery;
        this.repository = repository;
    }

    @Override
    public Map<String, Object> migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            data.put("preWorkAllocation", "Yes");
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return repository.findCases(elasticSearchQuery);
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
