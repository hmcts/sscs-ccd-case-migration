package uk.gov.hmcts.reform.domain.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import static com.google.common.base.Strings.isNullOrEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder(toBuilder = true)
public class CaseLoaderHearingDetails {
    public String venueId;
    public String hearingId;
    public String hearingDate;
    public String hearingTime;
    public String hearingAdjourned;

    @JsonCreator
    public CaseLoaderHearingDetails(String venueId, String hearingId, String hearingDate, String hearingTime,
                                    String hearingAdjourned) {
        this.venueId = venueId;
        this.hearingId = hearingId;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.hearingAdjourned = hearingAdjourned;
    }

    public boolean hasMissingValues() {
        return isNullOrEmpty(venueId) || isNullOrEmpty(hearingId) || isNullOrEmpty(hearingDate)
            || isNullOrEmpty(hearingTime) || isNullOrEmpty(hearingAdjourned);
    }
}
