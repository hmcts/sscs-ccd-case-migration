package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.defaultPanelComposition.enabled", havingValue = "true")
public class DefaultPanelCompositionMigration extends CaseMigrationProcessor {

    static final String UPDATE_LISTING_REQUIREMENTS_ID = "updateListingRequirements";
    static final String UPDATE_LISTING_REQUIREMENTS_SUMMARY = "Migration: Set default Panel Composition";
    static final String UPDATE_LISTING_REQUIREMENTS_DESCRIPTION = "Migration: Set default Panel Composition";

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
        log.info(getEventSummary() + " for Case: {}", caseDetails.getId());
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        var caseData = caseDetails.getData();
        var defaultListingValues = mapper.convertValue(caseData.get("defaultListingValues"), OverrideFields.class);
        var overrideFields = nonNull(caseData.get("overrideFields"))
            ? mapper.convertValue(caseData.get("overrideFields"), OverrideFields.class)
            : OverrideFields.builder().build();
        if (isNull(overrideFields.getDuration())) {
            overrideFields.setDuration(defaultListingValues.getDuration());
            log.info("Setting override fields duration to {} for Case: {}",
                     overrideFields.getDuration(), caseDetails.getId());
            caseDetails.getData().put("overrideFields", overrideFields);
        }
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    public String getEventId() {
        return UPDATE_LISTING_REQUIREMENTS_ID;
    }

    public String getEventDescription() {
        return UPDATE_LISTING_REQUIREMENTS_DESCRIPTION;
    }

    public String getEventSummary() {
        return UPDATE_LISTING_REQUIREMENTS_SUMMARY;
    }
}
