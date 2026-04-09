package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.common.CaseLoaderHearingDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToCaseLoaderHearingDetails;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.addHearingMigration.enabled", havingValue = "true")
public class AddHearingMigration extends CaseMigrationProcessor {

    static final String ADD_HEARING_EVENT_ID = "addHearing";
    static final String ADD_HEARING_EVENT_SUMMARY = "London Tribunals migration";
    static final String ADD_HEARING_EVENT_DESCRIPTION = "London Tribunals migration";
    private final ObjectMapper objectMapper;
    private final VenueService venueService;
    private final Map<String, CaseLoaderHearingDetails> caseRefToCaseLoaderHearingDetailsMap;

    private final String encodedDataString;

    public AddHearingMigration(@Value("${migration.addHearingMigration.encoded-data-string}")
                                      String encodedDataString, VenueService venueService) {
        this.encodedDataString = encodedDataString;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.venueService = venueService;
        this.caseRefToCaseLoaderHearingDetailsMap = mapCaseRefToCaseLoaderHearingDetails(encodedDataString);
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        Map<String, Object> data = caseDetails.getData();

        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            SscsCaseData sscsCaseData = objectMapper.convertValue(data, SscsCaseData.class);
            CaseLoaderHearingDetails caseLoaderHearingDetails = caseRefToCaseLoaderHearingDetailsMap.get(caseId);

            log.info("CaseLoaderHearingDetails for case id {}: {}", caseId, caseLoaderHearingDetails);

            if (isNull(caseLoaderHearingDetails) || caseLoaderHearingDetails.hasMissingValues()) {
                log.info("Missing CaseLoaderHearingDetails for case id {}, skipping migration.", caseId);
                throw new RuntimeException("Skipping case for addHearing migration. Reason: CaseLoaderHearingDetails "
                                               + "has missing values " + caseId);
            }

            String selectedHearingId = caseLoaderHearingDetails.getHearingId();

            List<Hearing> caseDataHearings = sscsCaseData.getHearings();
            if (isNull(caseDataHearings)) {
                log.info("No hearings found for case id {}, initialising empty hearings list", caseId);
                caseDataHearings = new ArrayList<>();
            } else {
                boolean hearingExists = caseDataHearings.stream()
                    .anyMatch(hearing -> hearing.getValue().getHearingId().equals(selectedHearingId));
                if (hearingExists) {
                    log.info("Hearing with hearingId {} already exists for case id {}, skipping migration",
                             selectedHearingId, caseId);
                    throw new RuntimeException("Skipping case for addHearing migration. Reason: hearing with hearingId "
                                                   + selectedHearingId + " already exists for case id " + caseId);
                }
            }

            String epimsId = venueService.getEpimsIdForVenueId(caseLoaderHearingDetails.venueId);
            VenueDetails venueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(epimsId);

            HearingDetails newHearing = HearingDetails.builder()
                .hearingId(selectedHearingId)
                .hearingDate(caseLoaderHearingDetails.getHearingDate())
                .time(caseLoaderHearingDetails.getHearingTime())
                .venue(Venue.builder()
                           .name(venueDetails.getVenName())
                           .address(Address.builder()
                                        .line1(venueDetails.getVenAddressLine1())
                                        .line2(venueDetails.getVenAddressLine2())
                                        .town(venueDetails.getVenAddressTown())
                                        .county(venueDetails.getVenAddressCounty())
                                        .postcode(venueDetails.getVenAddressPostcode())
                                        .build())
                           .googleMapLink(venueDetails.getUrl())
                           .build())
                .adjourned(caseLoaderHearingDetails.getHearingAdjourned())
                .venueId(venueDetails.getVenueId())
                .epimsId(epimsId)
                .build();
            log.info("New hearing details for case id {}: {}", caseId, newHearing);

            caseDataHearings.add(Hearing.builder().value(newHearing).build());
            log.info("Adding hearing with hearingId {} to case id {}", selectedHearingId, caseId);

            data.put("hearings", caseDataHearings);
        }
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public String getEventId() {
        return ADD_HEARING_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return ADD_HEARING_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return ADD_HEARING_EVENT_SUMMARY;
    }
}
