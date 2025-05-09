package uk.gov.hmcts.reform.domain.hmc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingsUpdateResponse {
    private Long hearingRequestId;

    private HmcStatus status;

    private LocalDateTime timeStamp;

    private Long versionNumber;
}

