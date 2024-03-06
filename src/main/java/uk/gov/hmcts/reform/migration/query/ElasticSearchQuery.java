package uk.gov.hmcts.reform.migration.query;

public interface ElasticSearchQuery {
    public String getQuery(String searchAfterValue, int size, boolean initialSearch);
}
