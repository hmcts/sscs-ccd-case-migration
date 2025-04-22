package uk.gov.hmcts.reform.migration.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery.LIST_ASSIST_READY_TO_LIST_CASES;

@ExtendWith(MockitoExtension.class)
class DefaultPanelCompositionQueryTest {

    private final DefaultPanelCompositionQuery searchQuery = new DefaultPanelCompositionQuery();

    @Test
    void shouldReturnQuery() {
        assertThat(searchQuery.getStartQuery()).isEqualTo(LIST_ASSIST_READY_TO_LIST_CASES);
    }
}
