package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.updateListingReqsMissingAmendReason.enabled", havingValue = "true")
public class UpdateListingReqsMissingAmendReasonMigration extends DefaultPanelCompositionMigration {

    static final String UPDATE_LISTING_REQS_AMEND_REASON_SUMMARY =
        "System generated event to send Updated Listing Request to ListAssist";
    static final String UPDATE_LISTING_REQS_AMEND_REASON_DESCRIPTION =
        "Automated request did not send when Update Listing Requirements event used";

    public UpdateListingReqsMissingAmendReasonMigration(
        DefaultPanelCompositionQuery searchQuery,
        ElasticSearchRepository repository,
        @Value("${migration.updateListingReqsMissingAmendReason.encoded-data-string}")
        String encodedDataString
    ) {
        super(searchQuery, repository, true, encodedDataString);
    }

    @Override
    public String getEventDescription() {
        return UPDATE_LISTING_REQS_AMEND_REASON_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return UPDATE_LISTING_REQS_AMEND_REASON_SUMMARY;
    }
}
