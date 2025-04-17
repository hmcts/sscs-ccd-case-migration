package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.updateListingReqsMissingAmendReason.enabled", havingValue = "true")
public class UpdateListingRegsMissingAmedReasonMigration extends DefaultPanelCompositionMigration {

    static final String UPDATE_LISTING_REQS_AMEND_REASON_SUMMARY =
        "System generated event to send Updated Listing Request to ListAssist";
    static final String UPDATE_LISTING_REQS_AMEND_REASON_DESCRIPTION =
        "Automated request did not send when Update Listing Requirements event used";

    private final EncodedStringCaseList encodedStringCaseList;

    public UpdateListingRegsMissingAmedReasonMigration(
        DefaultPanelCompositionQuery searchQuery,
        ElasticSearchRepository repository,
        @Value("${migration.updateListingReqsMissingAmendReason.encoded-data-string}")
        String encodedDataString
    ) {
        super(searchQuery, repository);
        this.encodedStringCaseList = new EncodedStringCaseList(encodedDataString);
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return encodedStringCaseList.findCases();
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
