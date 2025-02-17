package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class SixMonthsOldDraftsSearchQuery extends ElasticSearchQuery {

    private static final String SIX_MONTHS_OLD_DRAFTS_QUERY = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "state": "draft" }},
                { "range": { "created_date": { "lte": "now-6M/M" }}}
              ]
            }
          },
          "_source": [ "reference" ],
          "size": %s,
          "sort": [ { "reference.keyword": "asc" } ]
        """;

    @Override
    protected String getStartQuery() {
        return SIX_MONTHS_OLD_DRAFTS_QUERY;
    }
}
