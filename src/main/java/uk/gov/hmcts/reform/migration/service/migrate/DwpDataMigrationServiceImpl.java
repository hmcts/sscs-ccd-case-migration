package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.query.DwpElasticSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.dwp-enhancements.enabled", havingValue = "true")
public class DwpDataMigrationServiceImpl extends CaseMigrationProcessor {

    public static final String EVENT_ID = "dwpCaseMigration";
    public static final String EVENT_SUMMARY = "Migrate case for DWP Enhancements";
    public static final String EVENT_DESCRIPTION = "Migrate case for DWP Enhancements";

    private final DwpElasticSearchQuery elasticSearchQuery;
    private final ElasticSearchRepository repository;

    public DwpDataMigrationServiceImpl(DwpElasticSearchQuery elasticSearchQuery,
                                       ElasticSearchRepository repository) {
        this.elasticSearchQuery = elasticSearchQuery;
        this.repository = repository;
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
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
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return repository.findCases(elasticSearchQuery, true);
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
