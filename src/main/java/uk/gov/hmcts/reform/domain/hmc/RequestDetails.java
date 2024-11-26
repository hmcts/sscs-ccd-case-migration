package uk.gov.hmcts.reform.domain.hmc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestDetails {

    @JsonInclude()
    private Long versionNumber;

    @JsonProperty("hearingRequestID")
    private String hearingRequestId;

    private HmcStatus status;

    private List<CancellationReason> cancellationReasonCodes;

    private LocalDateTime timestamp;

    private String hearingGroupRequestId;

    private LocalDateTime partiesNotified;

}
