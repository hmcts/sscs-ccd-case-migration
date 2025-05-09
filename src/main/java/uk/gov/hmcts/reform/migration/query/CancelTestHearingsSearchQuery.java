package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class CancelTestHearingsSearchQuery extends ElasticSearchQuery {
    static final String CASES_WITH_HEARINGS = """
        {
          "query": {
            "bool": {
                "must": [
                    {"exists": { "field": "data.hearings" }}
              ],
              "must_not": [
                {"exists": {"field": "data.hearings.value.hearingDate"}},
                {"match": {"data.hearings.value.hearingStatus": "CANCELLED"}}
              ]
            }
          },
          "_source": [ "data" ],
          "size": %s,
          "sort": [ { "reference.keyword": "asc" } ]
        """;


    @Override
    protected String getStartQuery() {
        return CASES_WITH_HEARINGS;
    }
}
