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
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
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
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.convertCaseDetailsToSscsCaseDetails;

@ExtendWith(MockitoExtension.class)
class DefaultPanelCompositionMigrationTest {

    @Mock
    private DefaultPanelCompositionQuery searchQuery;
    @Mock
    private ElasticSearchRepository repository;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

    private DefaultPanelCompositionMigration underTest;
    private final CaseHearing caseHearing1 = CaseHearing.builder().hearingId(parseLong("1234"))
        .hmcStatus(HmcStatus.AWAITING_LISTING).build();
    private final CaseHearing caseHearing2 = CaseHearing.builder().hearingId(parseLong("4321"))
        .hmcStatus(HmcStatus.CANCELLED).build();

    @BeforeEach
    void setUp() {
        underTest =
            new DefaultPanelCompositionMigration(searchQuery, repository, hmcHearingsApiService, false, "dummy-string");
    }

    @Test
    void shouldFetchCasesToMigrateFromRepository() {
        var caseA = buildCaseWith("readyToList", HearingRoute.LIST_ASSIST);
        var caseB = buildCaseWith("readyToList", HearingRoute.GAPS);
        var caseC = buildCaseWith("draft", HearingRoute.LIST_ASSIST);
        var caseD = buildCaseWith("validAppeal", HearingRoute.LIST_ASSIST);
        List<SscsCaseDetails> caseList = List.of(caseA, caseB, caseC, caseD);
        when(repository.findCases(searchQuery, true)).thenReturn(caseList);

        List<SscsCaseDetails> migrationCases = underTest.fetchCasesToMigrate();

        assertThat(migrationCases).hasSize(1);
        assertThat(migrationCases).contains(caseA);
    }

    @Test
    void shouldFetchCasesToMigrateFromEncodedDataString() {
        underTest =
            new DefaultPanelCompositionMigration(searchQuery, repository, hmcHearingsApiService,true, ENCODED_STRING);

        var casesToMigrate = underTest.fetchCasesToMigrate();

        assertThat(casesToMigrate).hasSize(1);
        assertEquals(ENCODED_CASE_ID, casesToMigrate.getFirst().getId());
    }

    @Test
    void shouldReturnMigratedCaseData() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();

        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(60, convertCaseDetailsToSscsCaseDetails(caseDetails).getData()
            .getSchedulingAndListingFields().getOverrideFields().getDuration());
    }

    @Test
    void shouldNotResetOverrideFieldDuration() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build())
                .overrideFields(OverrideFields.builder().duration(90).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();

        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(90, convertCaseDetailsToSscsCaseDetails(caseDetails).getData()
            .getSchedulingAndListingFields().getOverrideFields().getDuration());
    }

    @ParameterizedTest
    @MethodSource("amendReasonListProvider")
    void shouldSetAmendReasonsToAdminRequest(List<AmendReason> amendReasons) {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build())
                .amendReasons(amendReasons)
                .build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();
        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

        underTest.migrate(caseDetails);

        assertEquals(caseDetails.getData(), data);
        assertEquals(List.of(AmendReason.ADMIN_REQUEST), data.get("amendReasons"));
    }

    @Test
    void shouldNotMigrateCaseIfNotReadyToList() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().state(HEARING.toString()).data(data).build();

        assertThrows(RuntimeException.class, () -> underTest.migrate(caseDetails));
    }

    @Test
    void shouldNotMigrateCaseifNotInAwaitingListingOrUpdateRequestedState() {
        var caseData = buildCaseData();
        caseData.setSchedulingAndListingFields(
            SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(60).build()).build());
        var data = buildCaseDataMap(caseData);
        var caseDetails = CaseDetails.builder().id(1234L).state(READY_TO_LIST.toString()).data(data).build();

        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

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
