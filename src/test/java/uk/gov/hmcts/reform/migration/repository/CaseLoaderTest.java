package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaseLoaderTest {

    private static final String VALID_ENCODED_DATA_STRING = "eJyLrlYqSk1LLUrNS05VslIyNLOwNDYyNzc0NDUxMjSyVKrVQVNgbmBobG"
        + "pkYmYGVGNkYqJUGwsAjiESMQ==";
    private static final String INVALID_ENCODED_DATA_STRING = "xxxxxxxxxxxxxxx";


    @Test
    void givenValidEncodedString_thenReturnListOfCases() {
        CaseLoader caseLoader = new CaseLoader(VALID_ENCODED_DATA_STRING);

        var result = caseLoader.findCases();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1689327711542129L, result.get(0).getId());
        assertEquals("SSCS", result.get(0).getJurisdiction());
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyList() {
        CaseLoader caseLoader = new CaseLoader(INVALID_ENCODED_DATA_STRING);

        var result = caseLoader.findCases();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
