package uk.gov.hmcts.reform.migration.query;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DwpElasticSearchQueryTest {

    private static final int QUERY_SIZE = 100;

    @Test
    void shouldReturnQuery() {
        DwpElasticSearchQuery elasticSearchQuery = new DwpElasticSearchQuery();
        String query = elasticSearchQuery.getQuery(null, QUERY_SIZE, true);
        assertEquals("""
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
        DwpElasticSearchQuery elasticSearchQuery = new DwpElasticSearchQuery();
        String query = elasticSearchQuery.getQuery("1677777777", QUERY_SIZE, false);
        assertEquals("""
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
