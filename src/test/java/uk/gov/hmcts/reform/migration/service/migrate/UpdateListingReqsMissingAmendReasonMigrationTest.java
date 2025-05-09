package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.migration.service.migrate.UpdateListingReqsMissingAmendReasonMigration.UPDATE_LISTING_REQS_AMEND_REASON_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.UpdateListingReqsMissingAmendReasonMigration.UPDATE_LISTING_REQS_AMEND_REASON_SUMMARY;

@ExtendWith(MockitoExtension.class)
class UpdateListingReqsMissingAmendReasonMigrationTest {

    private static final String ENCODED_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VaqNBQC1oglo";

    @Mock
    private DefaultPanelCompositionQuery searchQuery;
    @Mock
    private ElasticSearchRepository repository;

    private UpdateListingReqsMissingAmendReasonMigration underTest;

    @BeforeEach
    void setUp() {
        underTest =
            new UpdateListingReqsMissingAmendReasonMigration(searchQuery, repository, ENCODED_STRING);
    }

    @Test
    void shouldReturnMigrationCases() {
        var migrationCase = SscsCaseDetails.builder().id(1729631427870175L).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(underTest.getEventDescription()).isEqualTo(UPDATE_LISTING_REQS_AMEND_REASON_DESCRIPTION);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(underTest.getEventSummary()).isEqualTo(UPDATE_LISTING_REQS_AMEND_REASON_SUMMARY);
    }
}
