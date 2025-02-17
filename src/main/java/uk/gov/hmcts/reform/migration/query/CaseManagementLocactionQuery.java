package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class CaseManagementLocactionQuery extends ElasticSearchQuery {

    private static final String CASE_MANAGEMENT_LOCATION_QUERY = """
                {
                  "query": {
                    "bool": {
                      "should": [
                        {
                        "bool": {
                            "must_not": [
                                { "exists": { "field": "data.caseManagementLocation" }},
                                { "match": { "state": "draft" }},
                                { "match": { "state": "draftArchived" }},
                                { "match": { "state": "voidState" }}
                            ]
                          }
                        }
                      ]
                    }
                  },
                  "_source": [
                    "reference"
                  ],
                  "size":  %s,
                  "sort": [
                    {
                      "reference.keyword": "asc"
                    }
                  ]
        """;

    @Override
    protected String getStartQuery() {
        return CASE_MANAGEMENT_LOCATION_QUERY;
    }
}
