package uk.gov.hmcts.reform.domain.hmc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingsGetResponse {
    private String hmctsServiceCode;
    @JsonProperty("caseRef")
    private Long caseId;
    private List<CaseHearing> caseHearings;
}
