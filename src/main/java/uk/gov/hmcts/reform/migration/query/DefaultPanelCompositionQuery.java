package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class DefaultPanelCompositionQuery extends ElasticSearchQuery {

    static final String DEFAULT_PANEL_COMPOSITION = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "state": "readyToList" }},
                { "match": { "data.hearingRoute": "listAssist" }}
              ]
            }
          },
          "size": %s,
          "sort": [ { "reference.keyword": "asc" } ]
        """;

    @Override
    protected String getStartQuery() {
        return DEFAULT_PANEL_COMPOSITION;
    }
}
