package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaseLoaderTest {

    private static final String ENCODED_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VaqNBQC1oglo";
    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VdJRykhNLMrMS"
        + "/d0AUkbGZso1cYCAJvGDos=";
    private static final String INVALID_ENCODED_DATA_STRING = "xxxxxxxxxxxxxxx";


    @Test
    void givenValidEncodedString_thenReturnListOfCases() {
        CaseLoader caseLoader = new CaseLoader(ENCODED_STRING);

        var result = caseLoader.findCases();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1729631427870175L, result.getFirst().getId());
        assertEquals("SSCS", result.getFirst().getJurisdiction());
    }

    @Test
    void givenValidEncodedStringWithHearingId_thenReturnListOfCasesAndHearingIdMap() {
        CaseLoader caseLoader = new CaseLoader(ENCODED_HEARING_STRING);

        var result = caseLoader.findCasesWithHearingID();

        assertNotNull(result);
        assertFalse(result.getValue().isEmpty());
        assertEquals("1234", result.getLeft().get("1729631427870175"));
        assertEquals(1729631427870175L, result.getRight().getFirst().getId());
        assertEquals("SSCS", result.getRight().getFirst().getJurisdiction());
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyList() {
        CaseLoader caseLoader = new CaseLoader(INVALID_ENCODED_DATA_STRING);

        var result = caseLoader.findCases();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyListAndMap() {
        CaseLoader caseLoader = new CaseLoader(INVALID_ENCODED_DATA_STRING);

        var result = caseLoader.findCasesWithHearingID();

        assertNotNull(result);
        assertTrue(result.getKey().isEmpty());
        assertTrue(result.getValue().isEmpty());
    }
}
