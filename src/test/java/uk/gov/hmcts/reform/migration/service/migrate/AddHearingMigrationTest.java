package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.service.migrate.AddHearingMigration.ADD_HEARING_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.AddHearingMigration.ADD_HEARING_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.AddHearingMigration.ADD_HEARING_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
public class AddHearingMigrationTest {

    private static final String ENCODED_HEARING_STRING = "eJxljlEKwyAQRO+y3wmsq9GYQ/QCpR8St62FGpAkPyV3r4ZCqIGFgZlZ"
        + "5l0/kPjOiePIMIAwZLUUikxvUJgOGnD+NS0pss/xZcrGk10K8TGH9/6BA5Y7glCagqTtzWF6N5c2IekWVYslWjku/GtrC1tTs1jSUurMk"
        + "rVTFcuJpCI4bf8twnb7AkfMSKQ=";
    private final String validCaseId = "1729631427870175";
    private final String invalidCaseId = "1792633627826354";
    private final String hearingDate = "2026-04-07";
    private final String hearingTime = "10:00:00";
    private final String hearingId = "123987";
    private final String adjourned = "No";

    private final String venueId = "1269";
    private final String epimsId = "123456789";
    private final String venueAddressLine1 = "123 Test Street";
    private final String venueAddressLine2 = "Test Town";
    private final String venueAddressTown = "Test City";
    private final String venueAddressCounty = "Test County";
    private final String venueAddressPostcode = "TE5 7ST";
    private final String venueName = "Test Venue";
    private final String venueUrl = "http://testvenue.com";

    private final VenueDetails venueDetails = VenueDetails.builder()
        .epimsId(epimsId)
        .venName(venueName)
        .venAddressLine1(venueAddressLine1)
        .venAddressLine2(venueAddressLine2)
        .venAddressTown(venueAddressTown)
        .venAddressCounty(venueAddressCounty)
        .venAddressPostcode(venueAddressPostcode)
        .venueId(venueId)
        .url(venueUrl)
        .build();

    @Mock
    private VenueService venueService;

    private CaseDetails caseDetails;
    private AddHearingMigration addHearingMigration;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addHearingMigration = new AddHearingMigration(
            ENCODED_HEARING_STRING,
            venueService
        );
    }

    @Test
    void shouldReturnCorrectEventDetails() {
        assertThat(ADD_HEARING_EVENT_ID).isEqualTo(addHearingMigration.getEventId());
        assertThat(ADD_HEARING_EVENT_SUMMARY)
            .isEqualTo(addHearingMigration.getEventDescription());
        assertThat(ADD_HEARING_EVENT_DESCRIPTION).isEqualTo(addHearingMigration.getEventSummary());
    }

    @Test
    void shouldSkipMigrationWhenCaseLoaderHearingDetailsIsNull() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(validCaseId)
            .build();

        caseDetails = CaseDetails.builder().id(Long.valueOf(invalidCaseId)).data(buildCaseDataMap(caseData)).build();

        Map<String, Object> data = objectMapper.convertValue(caseData, Map.class);

        Exception exception = assertThrows(RuntimeException.class, () -> addHearingMigration.migrate(caseDetails));
        assertThat(exception.getMessage()).containsAnyOf("Skipping case for addHearing migration. "
                                                             + "Reason: CaseLoaderHearingDetails has missing values");
    }

    @Test
    void shouldSkipMigrationWhenHearingIdExistsOnCase() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(validCaseId)
            .hearings(List.of(
                Hearing.builder()
                    .value(HearingDetails.builder().hearingId(hearingId).build())
                    .build()
            )).build();

        caseDetails = CaseDetails.builder().id(Long.valueOf(validCaseId)).data(buildCaseDataMap(caseData)).build();

        Exception exception = assertThrows(RuntimeException.class, () -> addHearingMigration.migrate(caseDetails));
        assertThat(exception.getMessage()).containsAnyOf("Skipping case for addHearing migration. "
                                                             + "Reason: hearing with hearingId "
            + hearingId + " already exists for case id " + validCaseId);
    }


    @Test
    void shouldAddHearingToCaseWhenHearingIdDoesNotExist() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(validCaseId)
            .build();

        caseDetails = CaseDetails.builder().id(Long.valueOf(validCaseId)).data(buildCaseDataMap(caseData)).build();
        when(venueService.getEpimsIdForVenueId(venueId)).thenReturn(epimsId);
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(epimsId)).thenReturn(venueDetails);

        UpdateResult updateResult = addHearingMigration.migrate(caseDetails);

        List<Hearing>  hearings = (List<Hearing>) caseDetails.getData().get("hearings");

        assertThat(hearings).isNotEmpty();

        HearingDetails hearingDetails = hearings.getFirst().getValue();

        assertThat(hearingDetails.getHearingId()).isEqualTo(hearingId);
        assertThat(hearingDetails.getHearingDate()).isEqualTo(hearingDate);
        assertThat(hearingDetails.getTime()).isEqualTo(hearingTime);
        assertThat(hearingDetails.getVenue().getName()).isEqualTo(venueName);
        assertThat(hearingDetails.getVenue().getAddress()).isEqualTo(Address.builder()
            .line1(venueAddressLine1)
            .line2(venueAddressLine2)
            .town(venueAddressTown)
            .county(venueAddressCounty)
            .postcode(venueAddressPostcode).build());
        assertThat(hearingDetails.getVenue().getGoogleMapLink()).isEqualTo(venueUrl);
        assertThat(hearingDetails.getAdjourned()).isEqualTo(adjourned);
        assertThat(hearingDetails.getVenueId()).isEqualTo(venueId);
        assertThat(hearingDetails.getEpimsId()).isEqualTo(epimsId);

    }
}
