package uk.gov.hmcts.reform.migration.service.migrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.sentToDwpMigration.enabled", havingValue = "true")
public class SentToDwpMigration extends CaseMigrationProcessor {

    public static final String SENT_TO_DWP_MIGRATION_EVENT_ID = "sentToDwp";
    public static final String SENT_TO_DWP_MIGRATION_EVENT_SUMMARY = "Migrate case for Sent to FTA";
    public static final String SENT_TO_DWP_MIGRATION_EVENT_DESCRIPTION = "Migrate case for Sent to FTA";
    public static final String HMCTS_DWP_STATE = "hmctsDwpState";
    public static final String SENT_TO_DWP = "sentToDwp";
    public static final String DATE_SENT_TO_DWP = "dateSentToDwp";

    private final ObjectMapper objectMapper;
    private final String encodedDataString;
    private final int dwpResponseDueDays;
    private final int dwpResponseDueDaysChildSupport;

    public SentToDwpMigration(@Value("${migration.sentToDwpMigration.encoded-string}") String encodedDataString,
                              @Value("${dwp.response.due.days}") int dwpResponseDueDays,
                              @Value("${dwp.response.due.days-child-support}") int dwpResponseDueDaysChildSupport) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.encodedDataString = encodedDataString;
        this.dwpResponseDueDays = dwpResponseDueDays;
        this.dwpResponseDueDaysChildSupport = dwpResponseDueDaysChildSupport;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (data != null) {
            SscsCaseData sscsCaseData = objectMapper.convertValue(data, SscsCaseData.class);

            if (isNull(data.get(DATE_SENT_TO_DWP))) {
                data.put(DATE_SENT_TO_DWP,calculateSentToDwpDate(sscsCaseData));
                data.put(HMCTS_DWP_STATE, SENT_TO_DWP);
            } else {
                throw new RuntimeException("Skipping case for migration due to " + DATE_SENT_TO_DWP
                                               + " is already set");
            }
        } else {
            throw new RuntimeException("Skipping case for migration due to case data is empty.");
        }

        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public String getEventId() {
        return SENT_TO_DWP_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return SENT_TO_DWP_MIGRATION_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return SENT_TO_DWP_MIGRATION_EVENT_SUMMARY;
    }

    private int getResponseDueDays(SscsCaseData caseData) {
        return caseData.getAppeal().getBenefitType() != null
            && Benefit.CHILD_SUPPORT.getShortName().equalsIgnoreCase(caseData.getAppeal().getBenefitType().getCode())
            ? dwpResponseDueDaysChildSupport : dwpResponseDueDays;
    }

    private String calculateSentToDwpDate(SscsCaseData caseData) {
        int numberOfDays = getResponseDueDays(caseData);
        if (isNull(caseData.getDwpDueDate())) {
            throw new RuntimeException("Skipping migration due to FTA response due date is empty");
        }

        LocalDate dwpDueDate = LocalDate.parse(caseData.getDwpDueDate());
        return dwpDueDate.minusDays(numberOfDays).toString();
    }
}
