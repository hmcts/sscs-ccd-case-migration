package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.service.migrate.AwaitingConfidentialityReqMigration.AWAITING_CONFIDENTIALITY_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.AwaitingConfidentialityReqMigration.AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.AwaitingConfidentialityReqMigration.AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

public class AwaitingConfidentialityReqMigrationTest {

    private AwaitingConfidentialityReqMigration awaitingConfidentialityReqMigration;
    private static final String ENCODED_STRING = "ENCODED_STRING";
    private CaseDetails caseDetails;
    private SscsCaseData caseData;


    @BeforeEach
    public void setUp() {
        awaitingConfidentialityReqMigration = new AwaitingConfidentialityReqMigration(ENCODED_STRING);
        caseData = buildCaseData("test", "childSupport", "Child Support");
        caseDetails = CaseDetails.builder().id(123L).data(buildCaseDataMap(caseData)).build();
    }

    @Test
    void shouldMigrateChildSupportCaseWithOtherParty_andIncompleteApplicationState() {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build();
        caseData.setOtherParties(List.of(otherParty));
        caseDetails.setData(buildCaseDataMap(caseData));
        caseDetails.setState("incompleteApplication");
        var result = assertDoesNotThrow(() -> awaitingConfidentialityReqMigration.migrate(caseDetails));
        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("Case state moved to Awaiting confidentiality requirements");
    }

    @Test
    void shouldNotMigrateChildSupportCaseWithoutOtherParty() {
        caseDetails.setState("incompleteApplication");
        var result = assertThrows(IllegalStateException.class,
                                  () -> awaitingConfidentialityReqMigration.migrate(caseDetails));

        assertThat(result.getMessage())
            .isEqualTo("Skipping Case (123) for migration due to invalid other party data");
    }

    @Test
    void shouldNotMigrateChildSupportCaseWithInvalidState() {
        caseDetails.setState("validAppeal");
        var result = assertThrows(IllegalStateException.class,
                                  () -> awaitingConfidentialityReqMigration.migrate(caseDetails));

        assertThat(result.getMessage())
            .isEqualTo("Skipping Case (123) for migration due to incorrect state: (validAppeal)");
    }

    @Test
    void shouldReturnEventId() {
        assertThat(awaitingConfidentialityReqMigration.getEventId())
            .isEqualTo(AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_ID);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(awaitingConfidentialityReqMigration.getEventSummary())
            .isEqualTo(AWAITING_CONFIDENTIALITY_MIGRATION_EVENT_SUMMARY);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(awaitingConfidentialityReqMigration.getEventDescription())
            .isEqualTo(AWAITING_CONFIDENTIALITY_EVENT_DESCRIPTION);
    }

}
