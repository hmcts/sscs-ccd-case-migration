package uk.gov.hmcts.reform.migration.query;

public interface ElasticSearchQuery {
    String getQuery(String searchAfterValue, int size, boolean initialSearch);
}
