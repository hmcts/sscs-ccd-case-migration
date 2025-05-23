package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.migration.service.migrate.HmctsDwpStateMigrationImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.HmctsDwpStateMigrationImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.HmctsDwpStateMigrationImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HmctsDwpStateMigrationImplTest {

    private CaseDetails caseDetails;

    HmctsDwpStateMigrationImpl hmctsDwpStateMigrationImpl;

    @BeforeEach
    public void setUp() {
        hmctsDwpStateMigrationImpl = new HmctsDwpStateMigrationImpl(null);
        caseDetails = CaseDetails.builder().state(State.DORMANT_APPEAL_STATE.toString()).id(1234L).build();
    }

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        var sscsCaseDetails = SscsCaseDetails.builder().build();
        assertTrue(hmctsDwpStateMigrationImpl.accepts().test(sscsCaseDetails));
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(hmctsDwpStateMigrationImpl.accepts().test(null));
    }

    @Test
    void shouldSkipWhenDataIsNull() {
        hmctsDwpStateMigrationImpl.migrate(caseDetails);

        assertNull(caseDetails.getData());
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeMigration() {
        assertThat(EVENT_ID).isEqualTo(hmctsDwpStateMigrationImpl.getEventId());
        assertThat(EVENT_DESCRIPTION).isEqualTo(hmctsDwpStateMigrationImpl.getEventDescription());
        assertThat(EVENT_SUMMARY).isEqualTo(hmctsDwpStateMigrationImpl.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() {
        SscsCaseData caseData = SscsCaseData.builder().hmctsDwpState("failedSendingFurtherEvidence").build();
        var data = buildCaseDataMap(caseData);
        caseDetails.setData(data);

        hmctsDwpStateMigrationImpl.migrate(caseDetails);

        assertThat(data).isNotNull();
        assertNull(data.get("hmctsDwpState"));
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithHmctsDwpStateNotFailedSendingFurtherEvidence() {
        caseDetails.setData(buildCaseDataMap(buildCaseData()));

        assertThatThrownBy(() -> hmctsDwpStateMigrationImpl.migrate(caseDetails))
            .hasMessageContaining("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                      + " 'failedSendingFurtherEvidence'");
    }
}
