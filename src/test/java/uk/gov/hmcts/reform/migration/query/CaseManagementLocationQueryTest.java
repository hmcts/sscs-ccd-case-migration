package uk.gov.hmcts.reform.migration.query;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseManagementLocationQueryTest {

    private static final int QUERY_SIZE = 100;

    @Test
    void shouldReturnQuery() {
        CaseManagementLocactionQuery elasticSearchQuery = new CaseManagementLocactionQuery();
        String query = elasticSearchQuery.getQuery(null, QUERY_SIZE, true);
        assertEquals("""
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
                "size": 100,
                "sort": [
                    {
                        "reference.keyword": "asc"
                    }
                ]
            }
            """.replaceAll("\\s", ""), query.replaceAll("\\s", ""));
    }

    @Test
    void shouldReturnSearchAfterQuery() {
        CaseManagementLocactionQuery elasticSearchQuery = new CaseManagementLocactionQuery();
        String query = elasticSearchQuery.getQuery("1677777777", QUERY_SIZE, false);
        assertEquals("""
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
                "size": 100,
                "sort": [
                    {
                        "reference.keyword": "asc"
                    }
                ],
                "search_after": [
                    1677777777
                ]
            }
            """.replaceAll("\\s", ""), query.replaceAll("\\s", ""));
    }
}
