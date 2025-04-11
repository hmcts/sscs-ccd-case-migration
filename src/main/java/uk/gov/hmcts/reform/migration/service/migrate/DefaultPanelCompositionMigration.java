package uk.gov.hmcts.reform.migration.service.migrate;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.defaultPanelComposition.enabled", havingValue = "true")
public class DefaultPanelCompositionMigration extends CaseMigrationProcessor {

    static final String EVENT_ID = "updateListingRequirements";
    static final String EVENT_SUMMARY = "Set default Panel Composition";
    static final String EVENT_DESCRIPTION = "Set default Panel Composition";

    private final DefaultPanelCompositionQuery searchQuery;
    private final ElasticSearchRepository repository;

    public DefaultPanelCompositionMigration(DefaultPanelCompositionQuery searchQuery,
                                            ElasticSearchRepository repository) {
        this.searchQuery = searchQuery;
        this.repository = repository;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return repository.findCases(searchQuery, true)
            .stream()
            .filter(caseDetails -> READY_TO_LIST.toString().equals(caseDetails.getState())
                && caseDetails.getData().getSchedulingAndListingFields().getHearingRoute()
                .equals(HearingRoute.LIST_ASSIST))
            .toList();
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        log.info("Setting default Panel composition for Case: {}", caseDetails.getId());
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    public String getEventId() {
        return EVENT_ID;
    }

    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
