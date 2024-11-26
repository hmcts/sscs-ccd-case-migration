package uk.gov.hmcts.reform.domain.hmc;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;


@Slf4j
public final class HearingsWindowMapping {

    public static final int DAYS_TO_ADD_HEARING_WINDOW_DWP_RESPONDED = 31;
    public static final int DAYS_TO_ADD_HEARING_WINDOW_TODAY_POSTPONEMENT = 14;
    public static final int DAYS_TO_ADD_HEARING_WINDOW_TODAY = 1;

    private HearingsWindowMapping() {
    }

    public static boolean isCasePostponed(SscsCaseData caseData) {
        return isYes(caseData.getPostponement().getUnprocessedPostponement());
    }
}
