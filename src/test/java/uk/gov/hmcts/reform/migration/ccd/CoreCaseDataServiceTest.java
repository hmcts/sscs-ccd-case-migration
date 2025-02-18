package uk.gov.hmcts.reform.migration.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreCaseDataServiceTest {
    private static final String EVENT_ID = "migrateCase";
    private static final String CASE_TYPE = "CARE_SUPERVISION_EPO";
    private static final String CASE_ID = "123456789";
    private static final String USER_ID = "30";
    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJubGJoN";
    private static final String EVENT_TOKEN = "Bearer aaaadsadsasawewewewew";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESC = "Migrate Case";

    @Mock
    CoreCaseDataApi coreCaseDataApi;
    @Mock
    private DataMigrationService<Map<String, Object>> dataMigrationService;
    @Mock
    private IdamService idamService;

    @InjectMocks
    private CoreCaseDataService underTest;

    @Test
    void shouldUpdateTheCase() throws Exception {
        CaseDetails caseDetails3 = createCaseDetails();
        setupMocks(caseDetails3.getData());
        when(dataMigrationService.getEventDescription()).thenReturn(EVENT_DESC);
        when(dataMigrationService.getEventId()).thenReturn(EVENT_ID);
        when(dataMigrationService.getEventSummary()).thenReturn(EVENT_SUMMARY);

        //when
        CaseDetails update = underTest.update(CASE_TYPE, caseDetails3.getId(),
                                              caseDetails3.getJurisdiction(), dataMigrationService);
        //then
        assertThat(update.getId(), is(Long.parseLong(CASE_ID)));
        assertThat(update.getData().get("solicitorEmail"), is("Padmaja.Ramisetti@hmcts.net"));
        assertThat(update.getData().get("solicitorName"), is("PADMAJA"));
        assertThat(update.getData().get("solicitorReference"), is("LL02"));
        assertThat(update.getData().get("applicantLName"), is("Mamidi"));
        assertThat(update.getData().get("applicantFMName"), is("Prashanth"));
        assertThat(update.getData().get("appRespondentFMName"), is("TestRespondant"));
    }

    private CaseDetails createCaseDetails() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("solicitorEmail", "Padmaja.Ramisetti@hmcts.net");
        data.put("solicitorName", "PADMAJA");
        data.put("solicitorReference", "LL02");
        data.put("applicantLName", "Mamidi");
        data.put("applicantFMName", "Prashanth");
        data.put("appRespondentFMName", "TestRespondant");
        return CaseDetails.builder()
            .id(Long.valueOf(CASE_ID))
            .data(data)
            .build();
    }

    private void setupMocks(Map<String, Object> data) throws Exception {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder()
                                                         .idamOauth2Token(AUTH_TOKEN)
                                                         .serviceAuthorization(AUTH_TOKEN)
                                                         .userId(USER_ID).build());

        CaseDetails caseDetails = CaseDetails.builder()
            .id(123456789L)
            .data(data)
            .build();

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .eventId(EVENT_ID)
            .token(EVENT_TOKEN)
            .caseDetails(caseDetails)
            .build();

        when(dataMigrationService.migrate(caseDetails))
            .thenReturn(data);

        when(coreCaseDataApi.startEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, "30",
                                                     null, CASE_TYPE, CASE_ID, EVENT_ID
        ))
            .thenReturn(startEventResponse);

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .event(Event.builder()
                       .id(EVENT_ID)
                       .description(EVENT_DESC)
                       .summary(EVENT_SUMMARY)
                       .build())
            .eventToken(EVENT_TOKEN)
            .data(data)
            .ignoreWarning(false)
            .build();

        when(coreCaseDataApi.submitEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, USER_ID, null,
                                                      CASE_TYPE, CASE_ID, true, caseDataContent
        )).thenReturn(caseDetails);
    }
}
