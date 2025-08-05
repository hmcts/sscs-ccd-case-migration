package uk.gov.hmcts.reform.migration.service.migrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.domain.hmc.HmcStatus;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.query.DefaultPanelCompositionQuery;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.sscs.ccd.domain.AmendReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_CASE_ID;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseListTest.ENCODED_STRING;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_ID;
import static uk.gov.hmcts.reform.migration.service.migrate.DefaultPanelCompositionMigration.UPDATE_LISTING_REQUIREMENTS_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseDataMap;

@ExtendWith(MockitoExtension.class)
class DefaultPanelCompositionMigrationTest {

    @Mock
    private DefaultPanelCompositionQuery query;
    @Mock
    private ElasticSearchRepository repository;
    @Mock
    private HmcHearingsApiService hearingsApi;

    private DefaultPanelCompositionMigration underTest;
    private final CaseHearing caseHearing1 = CaseHearing.builder().hearingId(parseLong("1234"))
        .hmcStatus(HmcStatus.AWAITING_LISTING).build();
    private final CaseHearing caseHearing2 = CaseHearing.builder().hearingId(parseLong("4321"))
        .hmcStatus(HmcStatus.CANCELLED).build();

    @BeforeEach
    void setUp() {
        underTest =
            new DefaultPanelCompositionMigration(query, repository, hearingsApi, false, "xxx", "");
    }

    @Test
    void shouldFetchCasesToMigrateFromRepository() {
        var caseA = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        var caseB = buildCaseWith("readyToList", HearingRoute.GAPS);
        var caseC = buildCaseWith("draft", HearingRoute.LIST_ASSIST);
        var caseD = buildCaseWith("validAppeal", HearingRoute.LIST_ASSIST);
        var caseE = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        caseE.getData().setPanelMemberComposition(new PanelMemberComposition());
        var caseF = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        caseF.getData().setPanelMemberComposition(new PanelMemberComposition(List.of("84")));

        when(repository.findCases(query, true))
            .thenReturn(List.of(caseA, caseB, caseC, caseD, caseE, caseF));

        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(2);
        assertThat(migrationCases).containsOnly(caseA, caseE);
    }

    @Test
    void shouldFetchCasesExceptThoseInExclusionList() {
        var caseA = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        var caseB = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        caseB.setId(ENCODED_CASE_ID);
        when(repository.findCases(query, true)).thenReturn(List.of(caseA, caseB));

        underTest = new DefaultPanelCompositionMigration(
            query, repository, hearingsApi, false,
            "dummy-string", ENCODED_STRING);
        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).containsOnly(caseA);
    }

    @Test
    void shouldFetchCasesToMigrateFromEncodedDataString() {
        underTest =
            new DefaultPanelCompositionMigration(
                query, repository, hearingsApi, true,
                ENCODED_STRING, "");

        var casesToMigrate = underTest.fetchCasesToMigrate();

        assertThat(casesToMigrate).hasSize(1);
        assertEquals(ENCODED_CASE_ID, casesToMigrate.getFirst().getId());
    }

    @Test
    void shouldReturnMigratedCaseData() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();

        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hearingsApi.getHearingsRequest(anyString(), any())).thenReturn(response);

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
    }

    @ParameterizedTest
    @MethodSource("amendReasonListProvider")
    void shouldSetAmendReasonsToAdminRequest(List<AmendReason> amendReasons) {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .amendReasons(amendReasons)
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();
        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hearingsApi.getHearingsRequest(anyString(), any())).thenReturn(response);

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(List.of(AmendReason.ADMIN_REQUEST), data.get("amendReasons"));
    }

    @Test
    void shouldNotMigrateCaseIfNotReadyToList() {
        var caseData = buildCaseData();
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().state(HEARING.toString()).data(data).build();

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldNotMigrateCaseIfNotListAssist() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1L).state(READY_TO_LIST.toString()).data(data).build();

        Exception exception = assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
        assertThat(exception.getMessage()).containsAnyOf("hearingRoute is not list assist");
    }

    @Test
    void shouldNotMigrateCaseifNotInAwaitingListingOrUpdateRequestedState() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();

        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing2)).build();
        when(hearingsApi.getHearingsRequest(anyString(), any())).thenReturn(response);

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldReturnEventId() {
        assertThat(underTest.getEventId()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_ID);
    }

    @Test
    void shouldReturnEventDescription() {
        assertThat(underTest.getEventSummary()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_SUMMARY);
    }

    @Test
    void shouldReturnEventSummary() {
        assertThat(underTest.getEventDescription()).isEqualTo(UPDATE_LISTING_REQUIREMENTS_DESCRIPTION);
    }

    private SscsCaseDetails buildCaseWith(String state, HearingRoute hearingRoute) {
        return SscsCaseDetails.builder()
            .id(1L).state(state)
            .data(SscsCaseData.builder()
                      .schedulingAndListingFields(
                          SchedulingAndListingFields.builder().hearingRoute(hearingRoute).build()
                      ).build()
            ).build();
    }

    static Stream<List<AmendReason>> amendReasonListProvider() {
        return Stream.of(
            null,
            List.of(),
            List.of(AmendReason.JUDGE_REQUEST),
            List.of(AmendReason.ADMIN_REQUEST, AmendReason.PARTY_REQUEST)
        );
    }
}
