package uk.gov.hmcts.reform.domain.hmc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingResponse {
    @JsonProperty("listAssistTransactionID")
    private String listAssistTransactionId;

    private LocalDateTime receivedDateTime;

    private Long responseVersion;

    private String hearingCancellationReason;

    @JsonProperty("hearingDaySchedule")
    private List<HearingDaySchedule> hearingSessions;
}