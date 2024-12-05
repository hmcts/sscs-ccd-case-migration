package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService.getFirstHalfOfPostcode;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.case-management-location.enabled", havingValue = "true")
public class CaseManagementLocationMigrationImpl implements DataMigrationService<Map<String, Object>> {

    private static final String EVENT_ID = "caseManagementLocationMigration";
    private static final String EVENT_SUMMARY = "Migrate case for Case Management Location";
    private static final String EVENT_DESCRIPTION = "Migrate case for Case Management Location";

    private final RefDataService refDataService;
    private final VenueService venueService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AirLookupService airLookupService;
    private HashMap<String, String> regiondIdsCache = new HashMap<>();

    public CaseManagementLocationMigrationImpl(RefDataService refDataService,
                                               VenueService venueService,
                                               RegionalProcessingCenterService regionalProcessingCenterService,
                                               AirLookupService airLookupService) {
        this.refDataService = refDataService;
        this.venueService = venueService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.airLookupService = airLookupService;
    }

    @Override
    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> data, CaseDetails caseDetails) {
        if (nonNull(data)) {
            if (!data.containsKey("caseManagementLocation")) {
                Map<String, Object> managementLocation = getManagementLocation(data);
                if (!isNull(managementLocation)) {
                    data.put("caseManagementLocation", managementLocation);
                }
            }
        }

        return data;
    }

    private Map<String, Object> getManagementLocation(Map<String, Object> data) {
        SscsCaseData caseData = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(data, SscsCaseData.class);
        String postCode = resolvePostCode(caseData);
        String firstHalfOfPostcode = getFirstHalfOfPostcode(postCode);

        RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
        if (isNull(rpc) || (isNull(rpc.getEpimsId()) && !isNull(firstHalfOfPostcode))) {
            rpc = regionalProcessingCenterService.getByPostcode(firstHalfOfPostcode);
        } else if (isNull(postCode) && !isNull(rpc.getPostcode())) {
            postCode = rpc.getPostcode();
        }

        if (isNull(rpc) || isNull(postCode) || isNull(rpc.getEpimsId())) {
            throw new CaseMigrationException("Either rpc or postcode is null");
        }

        String processingVenue = airLookupService.lookupAirVenueNameByPostCode(postCode, getBenefitType(caseData));
        String venueEpimsId = venueService.getEpimsIdForVenue(processingVenue);
        String regionId = getRegionId(venueEpimsId);

        if (!isNull(regionId) && !isNull(rpc.getEpimsId())) {
            return Map.of("region", regionId, "baseLocation", rpc.getEpimsId());
        }
        return null;
    }

    private String getRegionId(String venueEpimsId) {
        if (regiondIdsCache.containsKey(venueEpimsId)) {
            return regiondIdsCache.get(venueEpimsId);
        }

        CourtVenue courtVenue = refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId);
        if (!isNull(courtVenue)) {
            String regionId = courtVenue.getRegionId();
            regiondIdsCache.put(venueEpimsId, regionId);
            return regionId;
        }
        return null;
    }

    private BenefitType getBenefitType(SscsCaseData caseData) {
        return caseData.getAppeal().getBenefitType();
    }


    private static String resolvePostCode(SscsCaseData sscsCaseData) {
        if (YES.getValue().equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            return Optional.ofNullable(sscsCaseData.getAppeal().getAppellant().getAppointee())
                .map(Appointee::getAddress)
                .map(Address::getPostcode)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .orElse(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
        }

        if (isNull(sscsCaseData.getAppeal().getAppellant())
            || isNull(sscsCaseData.getAppeal().getAppellant().getAddress())) {
            return null;
        }

        return sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();
    }

    @Override
    public String getEventId() {
        return EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
