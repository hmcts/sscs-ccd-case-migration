package uk.gov.hmcts.reform.migration.query;

import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("checkstyle:lineLength")
public class DefaultPanelCompositionQuery extends ElasticSearchQuery {

    static final String LIST_ASSIST_READY_TO_LIST_CASES = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "state": "readyToList" }},
                { "match": { "data.hearingRoute": "listAssist" }},
                {
                  "bool": {
                    "must": [
                      { "bool": { "must_not": { "exists": { "field": "data.panelMemberComposition.districtTribunalJudge" } } } },
                      { "bool": { "must_not": { "exists": { "field": "data.panelMemberComposition.panelCompositionJudge" } } } },
                      { "bool": { "must_not": { "exists": { "field": "data.panelMemberComposition.panelCompositionMemberMedical1" } } } },
                      { "bool": { "must_not": { "exists": { "field": "data.panelMemberComposition.panelCompositionMemberMedical2" } } } },
                      {
                        "bool": {
                          "should": [
                            { "bool": { "must_not": { "exists": { "field": "data.panelMemberComposition.panelCompositionDisabilityAndFqMember" } } } },
                            { "script": { "script": "doc['data.panelMemberComposition.panelCompositionDisabilityAndFqMember'].size() == 0" } }
                          ]
                        }
                      }
                    ]
                  }
                }
              ]
            }
          },
          "_source": [ "data.hearingRoute" ],
          "size": %s,
          "sort": [ { "reference.keyword": "desc" } ]
        """;

    @Override
    protected String getStartQuery() {
        return LIST_ASSIST_READY_TO_LIST_CASES;
    }
}
