package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
public class DwpElasticSearchQuery extends ElasticSearchQuery {

    private static final String DWP_START_QUERY = """
        {
          "query": {
            "bool": {
              "should": [
                {
                "bool": {
                    "must_not": {
                        "exists": {
                          "field": "data.poAttendanceConfirmed"
                        }
                      }
                    }
                },
                {
                  "bool": {
                      "must_not": {
                          "exists": {
                            "field": "data.tribunalDirectPoToAttend"
                          }
                        }
                      }
                  },
                  {
                    "bool": {
                        "must_not": {
                            "exists": {
                              "field": "data.dwpIsOfficerAttending"
                            }
                          }
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
        return DWP_START_QUERY;
    }
}
