package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.migration.service.HmctsDwpStateMigrationImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.HmctsDwpStateMigrationImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.HmctsDwpStateMigrationImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HmctsDwpStateMigrationImplTest {

    private  CaseDetails caseDetails;

    HmctsDwpStateMigrationImpl hmctsDwpStateMigrationImpl;

    @BeforeEach
    public void setUp() {
        hmctsDwpStateMigrationImpl = new HmctsDwpStateMigrationImpl(null,null);
        caseDetails = CaseDetails.builder().state(State.DORMANT_APPEAL_STATE.toString()).id(1234L).build();
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertThat(hmctsDwpStateMigrationImpl.accepts().test(caseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(hmctsDwpStateMigrationImpl.accepts().test(null)).isFalse();
    }

    @Test
    void shouldSkipWhenDataIsNull() throws Exception {
        Map<String, Object> result = hmctsDwpStateMigrationImpl.migrate(caseDetails);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertThat(EVENT_ID).isEqualTo(hmctsDwpStateMigrationImpl.getEventId());
        assertThat(EVENT_DESCRIPTION).isEqualTo(hmctsDwpStateMigrationImpl.getEventDescription());
        assertThat(EVENT_SUMMARY).isEqualTo(hmctsDwpStateMigrationImpl.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() throws Exception {
        SscsCaseData caseData = SscsCaseData.builder().hmctsDwpState("failedSendingFurtherEvidence").build();
        caseDetails.setData(buildCaseDataMap(caseData));

        Map<String, Object> result = hmctsDwpStateMigrationImpl.migrate(caseDetails);

        assertThat(result).isNotNull();
        assertThat(result.get("hmctsDwpState")).isNull();
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithHmctsDwpStateNotFailedSendingFurtherEvidence() {
        caseDetails.setData(buildCaseDataMap(buildCaseData()));

        assertThatThrownBy(() -> hmctsDwpStateMigrationImpl.migrate(caseDetails))
            .hasMessageContaining("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                      + " 'failedSendingFurtherEvidence'");
    }
}
