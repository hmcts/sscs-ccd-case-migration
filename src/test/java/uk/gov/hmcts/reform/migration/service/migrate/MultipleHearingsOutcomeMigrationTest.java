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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


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
    void shouldGetHearingsFromHmc() {
        CaseHearing caseHearing1 = CaseHearing.builder().hearingId(1234L).build();
        CaseHearing caseHearing2 = CaseHearing.builder().hearingId(6789L).build();
        HearingsGetResponse response =
            HearingsGetResponse.builder().caseHearings(List.of(caseHearing1, caseHearing2)).build();
        when(hmcHearingsApiService.getHearingsRequest(anyString(), any())).thenReturn(response);

        List<CaseHearing> result = multipleHearingsOutcomeMigration.getHearingsFromHmc("1729631427870175");

        assertThat(result).containsExactly(caseHearing1);
    }
}
