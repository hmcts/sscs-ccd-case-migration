package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.query.CaseManagementLocactionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

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

    private CaseManagementLocationMigrationImpl caseManagementLocationService;

    private final SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(1234L).build();

    @BeforeEach
    void setUp() {
        caseManagementLocationService =
            new CaseManagementLocationMigrationImpl(searchQuery, repository, refDataService,
                                                    venueService, regionalProcessingCenterService, airLookupService);
    }


    @Test
    void shouldReturnTrueForCaseDetailsPassed() {
        assertTrue(caseManagementLocationService.accepts().test(caseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(caseManagementLocationService.accepts().test(null));
    }

    @Test
    void shouldMigrateDataWhenCaseManagementLocationIsNull() {
        SscsCaseData caseData = buildCaseData();
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        caseData.setRegionalProcessingCenter(rpc);
        caseDetails.setData(caseData);
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());

        caseManagementLocationService.migrate(caseDetails);

        assertNotNull(caseData.getCaseManagementLocation());
        assertEquals(caseData.getCaseManagementLocation().getBaseLocation(), "rpgEpims");
        assertEquals(caseData.getCaseManagementLocation().getRegion(), "id");
    }

    @Test
    void shouldUseRpcPostCodeWhenPostCodeIsNull() {
        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().getAppellant().setAddress(null);
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        caseData.setRegionalProcessingCenter(rpc);
        caseDetails.setData(caseData);
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());

        caseManagementLocationService.migrate(caseDetails);

        assertNotNull(caseData.getCaseManagementLocation());
    }

    @Test
    void shouldThrowCaseMigrationExceptionWhenAddressAndRpcIsNull() {
        SscsCaseData caseData = buildCaseData();
        caseData.getAppeal().getAppellant().setAddress(null);
        caseData.setRegionalProcessingCenter(null);
        caseDetails.setData(caseData);

        assertThrows(CaseMigrationException.class, () -> caseManagementLocationService.migrate(caseDetails));
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalledAndRpcIsNull() {
        SscsCaseData caseData = buildCaseData();
        var rpc = caseData.getRegionalProcessingCenter().toBuilder().epimsId("rpgEpims").build();
        when(regionalProcessingCenterService.getByPostcode(anyString(), anyBoolean())).thenReturn(rpc);
        caseData.setRegionalProcessingCenter(null);
        caseData.getAppeal().getAppellant().setIsAppointee(YES);
        caseDetails.setData(caseData);
        when(airLookupService.lookupAirVenueNameByPostCode(anyString(), any())).thenReturn("");
        when(venueService.getEpimsIdForVenue(anyString())).thenReturn("epimsId");
        when(refDataService.getCourtVenueRefDataByEpimsId(anyString()))
            .thenReturn(CourtVenue.builder().regionId("id").build());

        caseManagementLocationService.migrate(caseDetails);

        assertNotNull(caseData.getCaseManagementLocation());
    }

    @Test
    void shouldReturnNullWhenDataIsNotPassed() {
        caseManagementLocationService.migrate(caseDetails);

        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForDwpMigration() {
        assertEquals("caseManagementLocationMigration", caseManagementLocationService.getEventId());
        assertEquals("Migrate case for Case Management Location", caseManagementLocationService.getEventDescription());
        assertEquals("Migrate case for Case Management Location", caseManagementLocationService.getEventSummary());
    }
}
