package uk.gov.hmcts.reform.migration.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.migration.query.OldDraftsSearchQuery.SIX_MONTHS_OLD_DRAFTS_QUERY;

@ExtendWith(MockitoExtension.class)
class OldDraftsSearchQueryTest {

    private final OldDraftsSearchQuery searchQuery = new OldDraftsSearchQuery();

    @Test
    void shouldReturnQuery() {
        assertThat(searchQuery.getStartQuery()).isEqualTo(SIX_MONTHS_OLD_DRAFTS_QUERY);
    }
}
