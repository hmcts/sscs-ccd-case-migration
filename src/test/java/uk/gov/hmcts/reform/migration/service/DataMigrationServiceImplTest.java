package uk.gov.hmcts.reform.migration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationServiceImplTest {

    private WaDataMigrationServiceImpl service = new WaDataMigrationServiceImpl();

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1234L)
            .build();
        assertTrue(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(service.accepts().test(null));
    }

    @Test
    public void shouldReturnPassedDataWhenMigrateCalled() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    public void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = service.migrate(null);
        assertNull(result);
    }

    @Test
    public void removeNullNino() {
        Map<String, Object> identity = new HashMap<>();
        identity.put("dob", "1980-08-10");
        identity.put("nino", null);
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", Map.of(
                     "appellant", Map.of(
                         "identity", identity)));
        data.put("field", "value");

        Map<String, Object> result = service.migrate(data);

        assertTrue(result.equals(Map.of(
            "field", "value",
            "appeal", Map.of(
                "appellant", Map.of(
                    "identity", Map.of("dob", "1980-08-10"))),
            "preWorkAllocation", "Yes"
        )));
    }

    @Test
    public void removeNullCaseReference() {
        Map<String, Object> data = new HashMap<>();
        data.put("caseReference", null);
        data.put("field", "value");

        Map<String, Object> result = service.migrate(data);

        assertTrue(result.equals(Map.of(
            "field", "value",
            "preWorkAllocation", "Yes"
        )));
    }

    @Test
    public void keepValidNinoAndCaseReference() {
        Map<String, Object> identity = new HashMap<>();
        identity.put("dob", "1980-08-10");
        identity.put("nino", "AB121212A");
        Map<String, Object> data = new HashMap<>();
        data.put("appeal", Map.of(
            "appellant", Map.of(
                "identity", identity)));
        data.put("caseReference", "REF/TEST/123");

        Map<String, Object> result = service.migrate(data);

        assertTrue(result.equals(Map.of(
            "caseReference", "REF/TEST/123",
            "appeal", Map.of(
                "appellant", Map.of(
                    "identity", Map.of("dob", "1980-08-10", "nino", "AB121212A"))),
            "preWorkAllocation", "Yes"
        )));
    }

}
