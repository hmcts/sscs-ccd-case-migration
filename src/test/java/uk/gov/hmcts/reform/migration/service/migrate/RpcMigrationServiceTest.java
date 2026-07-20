package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.HEARING_ROUTE_TO_SKIP;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.INVALID_HEARING_ROUTE_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.INVALID_RPC_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.LEEDS_RPC;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.NEW_REGION;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.NULL_HEARING_ROUTE_FAILURE_MSG;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.REGION_FIELD;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.REGION_TO_MIGRATE;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.RPC_FIELD;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.RPC_MIGRATION_EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.RPC_MIGRATION_EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.RpcMigrationService.RPC_MIGRATION_EVENT_SUMMARY;

@ExtendWith(MockitoExtension.class)
class RpcMigrationServiceTest {

    private RpcMigrationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RpcMigrationService(ENCODED_STRING);
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var migrationCase = SscsCaseDetails.builder().id(ENCODED_CASE_ID).jurisdiction("SSCS").build();
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(migrationCase);
    }

    @Test
    void shouldUpdateVenueWhenHearingRouteGaps() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) REGION_TO_MIGRATE, "hearingRoute", "gaps"));
        var caseDetails = CaseDetails.builder().data(caseData).build();
        underTest.migrate(caseDetails);

        assertThat(caseData.get(REGION_FIELD)).isEqualTo(NEW_REGION);
        assertThat(caseData.get(RPC_FIELD)).isEqualTo(LEEDS_RPC);
    }

    @Test
    void shouldUpdateVenueWhenCaseReferencePresentAndHearingRouteNull() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) REGION_TO_MIGRATE, "caseReference", "SC1"));
        var caseDetails = CaseDetails.builder().data(caseData).build();
        underTest.migrate(caseDetails);

        assertThat(caseData.get(REGION_FIELD)).isEqualTo(NEW_REGION);
        assertThat(caseData.get(RPC_FIELD)).isEqualTo(LEEDS_RPC);
    }

    @Test
    void shouldSkipCaseWithInvalidVenue() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) "invalidRpc"));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(IllegalStateException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(INVALID_RPC_FAILURE_MSG, 1234L,
                                                                   REGION_TO_MIGRATE
        ));
    }

    @Test
    void shouldSkipCaseWithInvalidHearingRoute() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) REGION_TO_MIGRATE, "hearingRoute",
                                            HEARING_ROUTE_TO_SKIP));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(INVALID_HEARING_ROUTE_FAILURE_MSG, 1234L,
                                                                   HEARING_ROUTE_TO_SKIP));
        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldSkipCaseWithNullHearingRouteAndNullScReference() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) REGION_TO_MIGRATE));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(NULL_HEARING_ROUTE_FAILURE_MSG, 1234L));
        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldSkipCaseWithNullHearingRouteAndEmptyScReference() {
        var caseData = new HashMap<>(Map.of(REGION_FIELD, (Object) REGION_TO_MIGRATE, "caseReference", ""));
        var caseDetails = CaseDetails.builder().data(caseData).id(1234L).build();
        var exception = assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).isEqualTo(String.format(NULL_HEARING_ROUTE_FAILURE_MSG, 1234L));
        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldGetEventId() {
        assertThat(underTest.getEventId()).isEqualTo(RPC_MIGRATION_EVENT_ID);
    }

    @Test
    void shouldGetEventDescription() {
        assertThat(underTest.getEventDescription()).isEqualTo(RPC_MIGRATION_EVENT_DESCRIPTION);
    }

    @Test
    void shouldGetEventSummary() {
        assertThat(underTest.getEventSummary()).isEqualTo(RPC_MIGRATION_EVENT_SUMMARY);
    }
}
