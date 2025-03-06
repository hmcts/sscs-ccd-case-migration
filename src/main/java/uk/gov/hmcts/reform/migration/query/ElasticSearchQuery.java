package uk.gov.hmcts.reform.migration.query;

public abstract class ElasticSearchQuery {

    private static final String END_QUERY = "\n}";
    private static final String SEARCH_AFTER = "\"search_after\": [%s]";

    public String getQuery(String searchAfterValue, int size, boolean initialSearch) {
        if (initialSearch) {
            return getInitialQuery(size);
        } else {
            return getSubsequentQuery(searchAfterValue, size);
        }
    }

    private String getInitialQuery(int size) {
        return String.format(getStartQuery(), size) + END_QUERY;
    }

    private String getSubsequentQuery(String searchAfterValue, int size) {
        return String.format(getStartQuery(), size) + "," + String.format(SEARCH_AFTER, searchAfterValue) + END_QUERY;
    }

    protected abstract String getStartQuery();
}
