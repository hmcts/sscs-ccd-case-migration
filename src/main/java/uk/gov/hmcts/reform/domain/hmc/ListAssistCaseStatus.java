package uk.gov.hmcts.reform.domain.hmc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;

@Getter
@RequiredArgsConstructor
public enum ListAssistCaseStatus {
    LISTED("Listed", HEARING),
    PENDING_RELISTING("Pending Relisting", null),
    CLOSED("Closed", null),
    EXCEPTION("Exception", null);

    private final String label;
    private final State caseStateUpdate;
}

