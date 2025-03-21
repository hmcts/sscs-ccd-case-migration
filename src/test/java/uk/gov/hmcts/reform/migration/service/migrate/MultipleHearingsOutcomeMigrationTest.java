package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class MultipleHearingsOutcomeMigrationTest {

    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VdJRykhNLMrMS"
        + "/d0AUkbGZso1cYCAJvGDos=";

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
    @Mock
    private HearingOutcomeService hearingOutcomeService;

    @InjectMocks
    private MultipleHearingsOutcomeMigration multipleHearingsOutcomeMigration;

    @BeforeEach
    void setUp() {
        multipleHearingsOutcomeMigration = new MultipleHearingsOutcomeMigration(
            hmcHearingsApiService,
            hearingOutcomeService,
            ENCODED_HEARING_STRING
        );
    }

    @Test
    void shouldFetchCasesToMigrate() {
        var caseDetailsList = List.of(SscsCaseDetails.builder().jurisdiction("SSCS").id(1729631427870175L).build());
        var caseRefToHearingIdMap = Map.of("1729631427870175", "1234");

        List<SscsCaseDetails> result = multipleHearingsOutcomeMigration.fetchCasesToMigrate();

        assertEquals(result, caseDetailsList);
        assertEquals(getField(multipleHearingsOutcomeMigration, "caseRefToHearingIdMap"), caseRefToHearingIdMap);
    }

    @Test
    void shouldGetHearingsFromHmc() {
        CaseHearing caseHearing1 = CaseHearing.builder().hearingId(1234L).build();
        CaseHearing caseHearing2 = CaseHearing.builder().hearingId(6789L).build();
        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);
        setField(multipleHearingsOutcomeMigration, "caseRefToHearingIdMap", Map.of("caseId", "1234"));

        List<CaseHearing> result = multipleHearingsOutcomeMigration.getHearingsFromHmc("caseId");

        assertThat(result).containsExactly(caseHearing1);
    }
}
