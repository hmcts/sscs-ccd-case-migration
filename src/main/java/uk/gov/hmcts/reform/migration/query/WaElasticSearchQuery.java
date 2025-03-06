package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class WaElasticSearchQuery extends ElasticSearchQuery {

    private static final String WA_START_QUERY = """
        {
          "query": {
            "bool": {
                "should": [
                    {
                        "bool": {
                            "must_not": [
                                { "exists": { "field": "data.preWorkAllocation" }},
                                { "match": { "state": "draft" }}
                            ]
                        }
                    },
                    {
                        "bool": {
                            "must_not": [
                                { "exists": { "field": "data.SearchCriteria.OtherCaseReferences" }},
                                { "match": { "state": "draft" }}
                            ]
                        }
                    }
                 ]
            }
          },
          "_source": [ "reference" ],
          "size": %s,
          "sort": [ { "reference.keyword": "asc" } ]
        """;

    @Override
    protected String getStartQuery() {
        return WA_START_QUERY;
    }
}
