package uk.gov.hmcts.reform.domain.hmc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListingStatus {
    DRAFT("Draft"),
    PROVISIONAL("Provisional"),
    FIXED("Fixed"),
    CNCL("Cancel");

    private final String label;
}
