package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.domain.hmc.CaseHearing;
import uk.gov.hmcts.reform.domain.hmc.HearingsGetResponse;
import uk.gov.hmcts.reform.migration.hmc.HmcHearingsApiService;
import uk.gov.hmcts.reform.migration.service.HearingOutcomeService;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class MultipleHearingsOutcomeMigrationTest {

    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslKysLC0NDBQ0lHKSE0sysxL93QBChoaGZso1cYCACSuDIc=";
    private final String REFERENCE = "889900";
    private final String HEARING_ID = "1234";

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
    @Mock
    private HearingOutcomeService hearingOutcomeService;

    private MultipleHearingsOutcomeMigration multipleHearingsOutcomeMigration;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        multipleHearingsOutcomeMigration = new MultipleHearingsOutcomeMigration(
            hmcHearingsApiService,
            hearingOutcomeService,
            ENCODED_HEARING_STRING
        );
    }

    @Test
    void shouldGetHearingsFromHmc() {
        CaseHearing caseHearing1 = CaseHearing.builder().hearingId(parseLong(HEARING_ID)).build();
        CaseHearing caseHearing2 = CaseHearing.builder().hearingId(6789L).build();
        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

        List<CaseHearing> result = multipleHearingsOutcomeMigration.getHearingsFromHmc(REFERENCE);

        assertThat(result).containsExactly(caseHearing1);
    }

    @Test
    void shouldSkipMigrationWhenCaseOutcomeIsNull() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(REFERENCE)
            .caseOutcome(CaseOutcome.builder().build())
            .build();

        Map<String, Object> data = objectMapper.convertValue(caseData, Map.class);

        assertThat(multipleHearingsOutcomeMigration.skipMigration(data)).isTrue();
    }

    @Test
    void shouldSkipMigrationWhenHearingAlreadyUsed() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(REFERENCE)
            .caseOutcome(CaseOutcome.builder()
                             .caseOutcome("11")
                             .didPoAttend(YesNo.YES)
                             .build())
            .hearingOutcomes(List.of(
                HearingOutcome.builder()
                    .value(HearingOutcomeDetails.builder()
                               .completedHearingId(HEARING_ID)
                               .build())
                    .build()))
            .build();

        Map<String, Object> data = objectMapper.convertValue(caseData, Map.class);

        assertThat(multipleHearingsOutcomeMigration.skipMigration(data)).isTrue();
    }

    @Test
    void shouldNotSkipMigrationWhenCaseOutcomeExistsAndHearingNotUsed() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(REFERENCE)
            .caseOutcome(CaseOutcome.builder()
                             .caseOutcome("11")
                             .didPoAttend(YesNo.YES)
                             .build())
            .hearingOutcomes(List.of(
                HearingOutcome.builder()
                    .value(HearingOutcomeDetails.builder()
                               .completedHearingId("9876")
                               .build())
                    .build()))
            .build();

        Map<String, Object> data = objectMapper.convertValue(caseData, Map.class);

        assertThat(multipleHearingsOutcomeMigration.skipMigration(data)).isFalse();
    }

    @Test
    void shouldAppendNewHearingOutcome() {
        CaseHearing caseHearing = CaseHearing.builder().hearingId(parseLong(HEARING_ID)).build();
        HearingsGetResponse response = HearingsGetResponse.builder().caseHearings(List.of(caseHearing)).build();
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(REFERENCE)
            .caseOutcome(CaseOutcome.builder()
                             .caseOutcome("11")
                             .didPoAttend(YesNo.YES)
                             .build())
            .hearingOutcomes(List.of(
                HearingOutcome.builder()
                    .value(HearingOutcomeDetails.builder()
                               .completedHearingId("9876")
                               .build())
                    .build()))
            .build();

        HearingOutcome mappedHearingOutcome = HearingOutcome.builder()
            .value(HearingOutcomeDetails.builder()
                       .completedHearingId(HEARING_ID)
                       .build())
            .build();

        Map<String, Object> data = objectMapper.convertValue(caseData, Map.class);

        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);
        when(hearingOutcomeService.mapHmcHearingToHearingOutcome(any(), any()))
            .thenReturn(List.of(mappedHearingOutcome));

        multipleHearingsOutcomeMigration.setHearingOutcome(data, REFERENCE);

        SscsCaseData updatedData = objectMapper.convertValue(data, SscsCaseData.class);
        assertThat(updatedData.getHearingOutcomes().size()).isEqualTo(2);
        assertThat(updatedData.getHearingOutcomes()).containsOnlyOnce(mappedHearingOutcome);
    }
}
