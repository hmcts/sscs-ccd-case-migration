package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("checkstyle:lineLength")
public class DefaultPanelCompositionQuery extends ElasticSearchQuery {

    static final String LIST_ASSIST_READY_TO_LIST_CASES = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "state": "readyToList" }},
                { "match": { "data.hearingRoute": "listAssist" }}
              ]
            }
          },
          "_source": [ "data.hearingRoute" ],
          "size": %s,
          "sort": [ { "reference.keyword": "asc" } ]
        """;

    @Override
    protected String getStartQuery() {
        return LIST_ASSIST_READY_TO_LIST_CASES;
    }
}
