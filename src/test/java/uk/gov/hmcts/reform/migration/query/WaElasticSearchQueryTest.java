package uk.gov.hmcts.reform.migration.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaElasticSearchQueryTest {
    private static final int QUERY_SIZE = 100;

    @Test
    public void shouldReturnQuery() {
        WaElasticSearchQuery elasticSearchQuery = new WaElasticSearchQuery();
        String query = elasticSearchQuery.getQuery(null, QUERY_SIZE, true);
        assertEquals("""
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
          "size": 100,
          "sort": [
            {
              "reference.keyword": "asc"
            }
          ]
           }""".replaceAll("\\s", ""), query.replaceAll("\\s", ""));
    }

    @Test
    public void shouldReturnSearchAfterQuery() {
        WaElasticSearchQuery elasticSearchQuery = new WaElasticSearchQuery();
        String query = elasticSearchQuery.getQuery("1677777777", QUERY_SIZE, false);
        assertEquals("""
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
          "size": 100,
          "sort": [
            {
              "reference.keyword": "asc"
            }
          ],
          "search_after": [1677777777]
           }""".replaceAll("\\s", ""), query.replaceAll("\\s", ""));
    }
}
