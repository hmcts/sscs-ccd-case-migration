package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class HearingOutcomeService {

    private final VenueService venueService;

    public HearingOutcomeService(VenueService venueService) {
        this.venueService = venueService;
    }

    public List<HearingOutcome> mapHmcHearingToHearingOutcome(CaseHearing hmcHearing, Map<String, Object> data) {
        log.info("Creating hearing outcome with hearingID {} using case outcome", hmcHearing.getHearingId());
        return createHearingOutcome(hmcHearing, data, "caseOutcome");
    }

    public List<HearingOutcome> mapHmcHearingToHearingOutcomeUsingOutcome(CaseHearing hmcHearing, Map<String, Object> data) {
        log.info("Creating hearing outcome with hearingID {} using outcome", hmcHearing.getHearingId());
        return createHearingOutcome(hmcHearing, data, "outcome");
    }

    private List<HearingOutcome> createHearingOutcome(CaseHearing hmcHearing, Map<String, Object> data, String outcomeKey) {

        var hearingDaySchedule = Optional.ofNullable(hmcHearing.getHearingDaySchedule())
            .flatMap(schedules -> schedules.stream().findFirst())
            .orElseGet(() -> HearingDaySchedule.builder().build());

        var hearingChannel = Optional.ofNullable(hmcHearing.getHearingChannels())
            .flatMap(channels -> channels.stream().findFirst())
            .orElse(null);

        var caseOutcome = Optional.ofNullable(data.get(outcomeKey))
            .map(Object::toString)
            .orElse(null);

        var didPoAttend = Optional.ofNullable(data.get("didPoAttend"))
            .map(Object::toString)
            .map(String::toUpperCase)
            .map(YesNo::valueOf)
            .orElse(null);

        HearingOutcomeDetails hearingOutcomeDetails = HearingOutcomeDetails.builder()
            .completedHearingId(hmcHearing.getHearingId().toString())
            .hearingStartDateTime(convertUtcToUk(hearingDaySchedule.getHearingStartDateTime()))
            .hearingEndDateTime(convertUtcToUk(hearingDaySchedule.getHearingEndDateTime()))
            .hearingOutcomeId(caseOutcome)
            .didPoAttendHearing(didPoAttend)
            .hearingChannelId(hearingChannel)
            .venue(mapEpimsIdToVenue(hearingDaySchedule.getHearingVenueEpimsId()))
            .epimsId(hearingDaySchedule.getHearingVenueEpimsId())
            .build();

        return List.of(HearingOutcome.builder().value(hearingOutcomeDetails).build());
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
