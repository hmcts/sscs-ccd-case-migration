package uk.gov.hmcts.reform.migration.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "migration.wa.enabled", havingValue = "true")
public class WaElasticSearchQuery {

    private static final String START_QUERY = """
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

    private static final String END_QUERY = "\n}";

    private static final String SEARCH_AFTER = "\"search_after\": [%s]";

    private String searchAfterValue;
    private int size;
    private boolean initialSearch;

    public String getQuery(String searchAfterValue, int size, boolean initialSearch) {
        if (initialSearch) {
            return getInitialQuery(size);
        } else {
            return getSubsequentQuery(searchAfterValue, size);
        }
    }

    private String getInitialQuery(int size) {
        return String.format(START_QUERY, size) + END_QUERY;
    }

    private String getSubsequentQuery(String searchAfterValue, int size) {
        return String.format(START_QUERY, size) + "," + String.format(SEARCH_AFTER, searchAfterValue) + END_QUERY;
    }
}
