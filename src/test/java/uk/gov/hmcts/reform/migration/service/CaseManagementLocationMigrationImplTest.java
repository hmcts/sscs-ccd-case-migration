package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.query.CaseManagementLocactionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

@ExtendWith(MockitoExtension.class)
public class CaseManagementLocationMigrationImplTest {

    @Mock
    RefDataService refDataService;
    @Mock
    VenueService venueService;
    @Mock
    RegionalProcessingCenterService regionalProcessingCenterService;
    @Mock
    AirLookupService airLookupService;
    @Mock
    CaseManagementLocactionQuery searchQuery;
    @Mock
    ElasticSearchRepository repository;

    private final CaseManagementLocationMigrationImpl caseManagementLocationService =
        new CaseManagementLocationMigrationImpl(searchQuery, repository, refDataService, venueService,
                                                regionalProcessingCenterService, airLookupService);

    private final  CaseDetails caseDetails = CaseDetails.builder()
        .id(1234L)
        .build();

    @Test
    void shouldReturnTrueForCaseDetailsPassed() {
        assertTrue(caseManagementLocationService.accepts().test(caseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(caseManagementLocationService.accepts().test(null));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        SscsCaseData caseData = buildCaseData();
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        caseData.setRegionalProcessingCenter(rpc);
        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());
        CaseManagementLocationMigrationImpl caseManagementLocationService = new CaseManagementLocationMigrationImpl(
            searchQuery, repository, refDataService, venueService, regionalProcessingCenterService, airLookupService);
        Map<String, Object> result = caseManagementLocationService.migrate(caseDetails);
        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    void shouldUseRpcPostCodeWhenPostCodeIsNull() {
        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().getAppellant().setAddress(null);
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        caseData.setRegionalProcessingCenter(rpc);
        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        caseDetails.setData(data);
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());
        CaseManagementLocationMigrationImpl caseManagementLocationService = new CaseManagementLocationMigrationImpl(
            searchQuery, repository, refDataService, venueService, regionalProcessingCenterService, airLookupService);

        Map<String, Object> result = caseManagementLocationService.migrate(caseDetails);
        assertNotNull(result);
        assertNotNull(result.get("caseManagementLocation"));
    }

    @Test
    void shouldThrowCaseMigrationExceptionWhenAddressAndRpcIsNull() {
        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().getAppellant().setAddress(null);
        caseData.setRegionalProcessingCenter(null);
        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        caseDetails.setData(data);

        CaseManagementLocationMigrationImpl caseManagementLocationService = new CaseManagementLocationMigrationImpl(
            searchQuery, repository, refDataService, venueService, regionalProcessingCenterService, airLookupService);

        assertThrows(CaseMigrationException.class, () -> caseManagementLocationService.migrate(caseDetails));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalledAndRpcIsNull() {
        SscsCaseData caseData = buildCaseData();
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        caseData.setRegionalProcessingCenter(null);
        caseData.getAppeal().getAppellant().setIsAppointee(YES);
        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        when(regionalProcessingCenterService.getByPostcode(anyString(), anyBoolean())).thenReturn(rpc);
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());
        CaseManagementLocationMigrationImpl caseManagementLocationService =
            new CaseManagementLocationMigrationImpl(searchQuery, repository, refDataService, venueService,
                                                    regionalProcessingCenterService, airLookupService);
        Map<String, Object> result = caseManagementLocationService.migrate(caseDetails);
        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = caseManagementLocationService.migrate(null);
        assertNull(result);
    }

    @Test
    void shouldReturnCorrectValuesForDwpMigration() {
        assertEquals("caseManagementLocationMigration", caseManagementLocationService.getEventId());
        assertEquals("Migrate case for Case Management Location", caseManagementLocationService.getEventDescription());
        assertEquals("Migrate case for Case Management Location", caseManagementLocationService.getEventSummary());
    }
}
