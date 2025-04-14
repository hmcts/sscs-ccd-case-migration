package uk.gov.hmcts.reform.migration.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery.DEFAULT_PANEL_COMPOSITION;

@ExtendWith(MockitoExtension.class)
class DefaultPanelCompositionQueryTest {

    private final DefaultPanelCompositionQuery searchQuery = new DefaultPanelCompositionQuery();

    @Test
    void shouldReturnQuery() {
        assertThat(searchQuery.getStartQuery()).isEqualTo(DEFAULT_PANEL_COMPOSITION);
    }
}
