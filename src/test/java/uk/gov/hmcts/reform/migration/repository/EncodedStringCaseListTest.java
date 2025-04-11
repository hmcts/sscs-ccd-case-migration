package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncodedStringCaseListTest {

    private static final String ENCODED_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VaqNBQC1oglo";
    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VdJRykhNLMrMS"
        + "/d0AUkbGZso1cYCAJvGDos=";
    private static final String INVALID_ENCODED_DATA_STRING = "xxxxxxxxxxxxxxx";


    @Test
    void givenValidEncodedString_thenReturnListOfCases() {
        EncodedStringCaseList encodedStringCaseList = new EncodedStringCaseList(ENCODED_STRING);

        var result = encodedStringCaseList.findCases();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1729631427870175L, result.getFirst().getId());
        assertEquals("SSCS", result.getFirst().getJurisdiction());
    }

    @Test
    void givenValidEncodedStringWithHearingId_thenReturnListOfCasesAndHearingIdMap() {
        EncodedStringCaseList encodedStringCaseList = new EncodedStringCaseList(ENCODED_HEARING_STRING);

        var result = encodedStringCaseList.mapCaseRefToHearingId();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("1234", result.get("1729631427870175"));
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyList() {
        EncodedStringCaseList encodedStringCaseList = new EncodedStringCaseList(INVALID_ENCODED_DATA_STRING);

        var result = encodedStringCaseList.findCases();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyListAndMap() {
        EncodedStringCaseList encodedStringCaseList = new EncodedStringCaseList(INVALID_ENCODED_DATA_STRING);

        var result = encodedStringCaseList.mapCaseRefToHearingId();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
