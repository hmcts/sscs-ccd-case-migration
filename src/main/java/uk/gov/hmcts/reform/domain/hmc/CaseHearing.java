package uk.gov.hmcts.reform.domain.hmc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseHearing {
    private List<HearingChannel> hearingChannels;
    private List<HearingDaySchedule> hearingDaySchedule;
    private String hearingGroupRequestId;
    @JsonProperty("hearingID")
    private Long hearingId;
    private Boolean hearingIsLinkedFlag;
    private LocalDateTime hearingRequestDateTime;
    private String hearingType;
    private HmcStatus hmcStatus;
    private LocalDateTime lastResponseReceivedDateTime;
    private Long requestVersion;
}
