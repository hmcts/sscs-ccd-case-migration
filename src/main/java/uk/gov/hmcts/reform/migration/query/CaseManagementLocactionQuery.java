package uk.gov.hmcts.reform.migration.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "migration.case-management-location.enabled", havingValue = "true")
public class CaseManagementLocactionQuery extends AbstractElasticQuery implements ElasticSearchQuery {
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
                                { "match": { "state": "draftArchived" }}
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
