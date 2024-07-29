package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaseLoaderTest {
    @Test
    void whenFileDoesntExist_thenReturnEmptyList() {
        CaseLoader caseLoader = new CaseLoader("nonExistent.file");
        var result = caseLoader.loadCases();

        assertTrue(result.isEmpty());
    }

    @Test
    void whenFileExists_thenReturnListOfCases() {
        CaseLoader caseLoader = new CaseLoader("casesList.json");

        var result = caseLoader.loadCases();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).getId());
        assertEquals("SSCS", result.get(0).getJurisdiction());
    }
}
