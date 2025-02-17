package uk.gov.hmcts.reform.migration.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.query.DwpElasticSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.dwp-enhancements.enabled", havingValue = "true")
public class DwpDataMigrationServiceImpl extends CaseMigrationProcessor {

    private static final String EVENT_ID = "dwpCaseMigration";
    private static final String EVENT_SUMMARY = "Migrate case for DWP Enhancements";
    private static final String EVENT_DESCRIPTION = "Migrate case for DWP Enhancements";

    private final DwpElasticSearchQuery elasticSearchQuery;
    private final ElasticSearchRepository repository;

    public DwpDataMigrationServiceImpl(DwpElasticSearchQuery elasticSearchQuery,
                                       ElasticSearchRepository repository) {
        this.elasticSearchQuery = elasticSearchQuery;
        this.repository = repository;
    }

    @Override
    public Map<String, Object> migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            if (!data.containsKey("poAttendanceConfirmed")) {
                data.put("poAttendanceConfirmed", "No");
            }
            if (!data.containsKey("dwpIsOfficerAttending")) {
                data.put("dwpIsOfficerAttending", "No");
            }
            if (!data.containsKey("tribunalDirectPoToAttend")) {
                data.put("tribunalDirectPoToAttend", "No");
            }
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return repository.findCases(elasticSearchQuery);
    }


    @Override
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
