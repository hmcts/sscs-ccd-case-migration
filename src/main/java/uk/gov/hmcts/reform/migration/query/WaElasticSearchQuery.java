package uk.gov.hmcts.reform.migration.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "migration.wa.enabled", havingValue = "true")
public class WaElasticSearchQuery extends AbstractElasticQuery implements ElasticSearchQuery {

    private static final String WA_START_QUERY = """
        {
          "query": {
            "bool": {
                "must_not": [
                    {
                        "exists": {
                            "field": "data.preWorkAllocation"
                         }
                        },
                        {
                            "match": {
                                "state": "draft"
                        }
                    }
                ]
            }
          },
          "_source": [
            "reference"
          ],
          "size": %s,
          "sort": [
            {
              "reference.keyword": "asc"
            }
          ]
          """;

    @Override
    protected String getStartQuery() {
        return WA_START_QUERY;
    }
}
