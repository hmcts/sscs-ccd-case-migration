package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;

import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToHearingId;

public class EncodedStringCaseListTest {

    public static final long ENCODED_CASE_ID = 1729631427870175L;
    public static final String ENCODED_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VaqNBQC1oglo";
    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VdJRykhNLMrMS"
        + "/d0AUkbGZso1cYCAJvGDos=";
    private static final String INVALID_ENCODED_DATA_STRING = "xxxxxxxxxxxxxxx";

    @Test
    void givenValidEncodedString_thenReturnListOfCases() {
        var result = findCases(ENCODED_STRING);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(ENCODED_CASE_ID, result.getFirst().getId());
        assertEquals("SSCS", result.getFirst().getJurisdiction());
    }

    @Test
    void givenValidEncodedStringWithHearingId_thenReturnListOfCasesAndHearingIdMap() {
        var result = mapCaseRefToHearingId(ENCODED_HEARING_STRING);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("1234", result.get(valueOf(ENCODED_CASE_ID)));
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyList() {
        var result = findCases(INVALID_ENCODED_DATA_STRING);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyListAndMap() {
        var result = mapCaseRefToHearingId(INVALID_ENCODED_DATA_STRING);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
