package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
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


@Slf4j
@ExtendWith(MockitoExtension.class)
public class HmctsDwpStateMigrationImplTest {

    private final  CaseDetails caseDetails = CaseDetails.builder()
        .state(State.DORMANT_APPEAL_STATE.toString())
        .id(1234L)
        .build();

    HmctsDwpStateMigrationImpl hmctsDwpStateMigrationImpl =
        new HmctsDwpStateMigrationImpl(null);

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
        Map<String, Object> result = hmctsDwpStateMigrationImpl.migrate(null);
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

        SscsCaseData caseData = SscsCaseData.builder()
            .hmctsDwpState("failedSendingFurtherEvidence")
            .build();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        caseDetails.setData(data);

        Map<String, Object> result = hmctsDwpStateMigrationImpl.migrate(caseDetails);
        assertThat(result).isNotNull();

        assertThat(result.get("hmctsDwpState")).isNull();
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithHmctsDwpStateNotFailedSendingFurtherEvidence() {
        SscsCaseData caseData = buildCaseData();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});
        caseDetails.setData(data);

        assertThatThrownBy(() -> hmctsDwpStateMigrationImpl.migrate(caseDetails))
            .hasMessageContaining("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                      + " 'failedSendingFurtherEvidence'");

    }
}
