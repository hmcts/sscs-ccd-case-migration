package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.query.OldDraftsSearchQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DRAFT;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.autoArchiveOldDrafts.enabled", havingValue = "true")
public class OldDraftAppealsArchiveService extends CaseMigrationProcessor {

    static final String EVENT_ID = "adminSendToDraftArchived";
    static final String EVENT_SUMMARY = "Auto archive draft appeals over 6 months old";
    static final String EVENT_DESCRIPTION = "Auto archive draft appeals over 6 months old";

    private final OldDraftsSearchQuery searchQuery;
    private final ElasticSearchRepository repository;

    public OldDraftAppealsArchiveService(OldDraftsSearchQuery searchQuery,
                                         ElasticSearchRepository repository) {
        this.searchQuery = searchQuery;
        this.repository = repository;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return repository.findCases(searchQuery, false)
            .stream()
            .filter(caseDetails -> DRAFT.toString().equals(caseDetails.getState())
                && !caseDetails.getCreatedDate().isAfter(now().minusMonths(6)))
            .toList();
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        log.info("Archiving draft appeal with case id: {}", caseDetails.getId());
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
