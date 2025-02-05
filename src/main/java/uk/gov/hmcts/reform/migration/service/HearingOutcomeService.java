package uk.gov.hmcts.reform.migration.service;

import static java.util.Objects.isNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@Slf4j
@Service
public class HearingOutcomeService {

    private final VenueService venueService;

    public HearingOutcomeService(VenueService venueService) {
        this.venueService = venueService;
    }

    public Map<String, HearingOutcome> mapHmcHearingToHearingOutcome(CaseHearing hmcHearing, SscsCaseData caseData) {
        log.info("Creating hearing outcome with hearingID {}", hmcHearing.getHearingId().toString());

        var hearingDaySchedule = isNull(hmcHearing.getHearingDaySchedule())
            ? HearingDaySchedule.builder().build()
            : hmcHearing.getHearingDaySchedule().stream().findFirst().orElse(HearingDaySchedule.builder().build());

        var hearingChannel = isNull(hmcHearing.getHearingChannels()) ? null : hmcHearing.getHearingChannels()
            .stream().findFirst()
            .orElse(null);

        HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
            .completedHearingId(hmcHearing.getHearingId().toString())
            .hearingStartDateTime(convertUtcToUk(hearingDaySchedule.getHearingStartDateTime()))
            .hearingEndDateTime(convertUtcToUk(hearingDaySchedule.getHearingEndDateTime()))
            .hearingOutcomeId(caseData.getCaseOutcome().getCaseOutcome())
            .didPoAttendHearing(caseData.getCaseOutcome().getDidPoAttend())
            .hearingChannelId(hearingChannel)
            .venue(mapEpimsIdToVenue(hearingDaySchedule.getHearingVenueEpimsId()))
            .epimsId(hearingDaySchedule.getHearingVenueEpimsId())
            .build();

        HearingOutcome hearingOutcome = HearingOutcome.builder().value(hearingOutcomeDetails).build();

        return Map.of("hearingOutcomes", hearingOutcome);
    }

    private Venue mapEpimsIdToVenue(String epimsId) {
        VenueDetails venueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(epimsId);
        if (isNull(venueDetails)) {
            log.info("EpimsId {} was not found", epimsId);
        }
        return isNull(venueDetails) ? Venue.builder().build() : mapVenueDetailsToVenue(venueDetails);
    }

    private Venue mapVenueDetailsToVenue(VenueDetails venueDetails) {
        return Venue.builder()
            .address(Address.builder()
                         .line1(venueDetails.getVenAddressLine1())
                         .line2(venueDetails.getVenAddressLine2())
                         .town(venueDetails.getVenAddressTown())
                         .county(venueDetails.getVenAddressCounty())
                         .postcode(venueDetails.getVenAddressPostcode())
                         .postcodeLookup(venueDetails.getVenAddressPostcode())
                         .postcodeAddress(venueDetails.getVenAddressPostcode())
                         .build())
            .googleMapLink(venueDetails.getUrl())
            .name(venueDetails.getVenName())
            .build();
    }

    private LocalDateTime convertUtcToUk(LocalDateTime utcLocalDateTime) {
        if (isNull(utcLocalDateTime)) {
            return null;
        }
        ZonedDateTime utcZone = utcLocalDateTime.atZone(ZoneId.of("UTC"));
        return utcZone.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime();
    }
}
