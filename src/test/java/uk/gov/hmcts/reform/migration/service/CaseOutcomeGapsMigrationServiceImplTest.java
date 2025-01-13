package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.EVENT_DESCRIPTION;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.EVENT_ID;
import static uk.gov.hmcts.reform.migration.service.CaseOutcomeGapsMigrationServiceImpl.EVENT_SUMMARY;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseOutcomeGapsMigrationServiceImplTest {

    private final  CaseDetails caseDetails = CaseDetails.builder()
        .id(1234L)
        .build();

    CaseOutcomeGapsMigrationServiceImpl caseOutcomeGapsMigrationService =
        new CaseOutcomeGapsMigrationServiceImpl();

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        assertThat(caseOutcomeGapsMigrationService.accepts().test(caseDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseForCaseDetailsNull() {
        assertThat(caseOutcomeGapsMigrationService.accepts().test(null)).isFalse();
    }

    @Test
    void shouldSkipWhenDataIsNull() throws Exception {
        Map<String, Object> result = caseOutcomeGapsMigrationService.migrate(null, null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnCorrectValuesForCaseOutcomeGapsMigration() {
        assertThat(EVENT_ID).isEqualTo(caseOutcomeGapsMigrationService.getEventId());
        assertThat(EVENT_DESCRIPTION).isEqualTo(caseOutcomeGapsMigrationService.getEventDescription());
        assertThat(EVENT_SUMMARY).isEqualTo(caseOutcomeGapsMigrationService.getEventSummary());
    }

    @Test
    void shouldReturnPassedDataWhenMigrateCalled() throws Exception {

        CaseOutcome caseOutcome = CaseOutcome.builder().caseOutcome("1234").didPoAttend(YesNo.YES).build();

        SscsCaseData caseData = SscsCaseData.builder()
            .regionalProcessingCenter(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.GAPS).build())
            .caseOutcome(caseOutcome)
            .build();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeGapsMigrationServiceImpl caseOutcomeGapsMigrationService =
            new CaseOutcomeGapsMigrationServiceImpl();
        Map<String, Object> result = caseOutcomeGapsMigrationService.migrate(data, caseDetails);
        assertThat(result).isNotNull();

        assertThat(result.get("caseOutcome")).isNull();
        assertThat(result.get("didPoAttend")).isNull();
    }

    @Test
    void shouldThrowErrorWhenMigrateCalledWithNonGapsCase() throws Exception {
        SscsCaseData caseData = buildCaseData();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(data, caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration. Hearing Route is not gaps");

    }

    @Test
    void shouldThrowErrorWhenMigrateCalledForGapsCaseWithNoCaseOutcome() throws Exception {
        SscsCaseData caseData = SscsCaseData.builder()
            .regionalProcessingCenter(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.GAPS).build())
            .build();

        var data = new ObjectMapper().registerModule(new JavaTimeModule())
            .convertValue(caseData, new TypeReference<Map<String, Object>>() {});

        CaseOutcomeGapsMigrationServiceImpl caseOutcomeGapsMigrationService =
            new CaseOutcomeGapsMigrationServiceImpl();

        assertThatThrownBy(() -> caseOutcomeGapsMigrationService.migrate(data, caseDetails))
            .hasMessageContaining("Skipping case for case outcome migration. Case outcome is empty");

    }
}
