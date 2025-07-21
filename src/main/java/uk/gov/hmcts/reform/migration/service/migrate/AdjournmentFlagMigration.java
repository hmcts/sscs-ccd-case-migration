package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.adjournmentFlag.enabled", havingValue = "true")
public class AdjournmentFlagMigration extends CaseMigrationProcessor {
    static final String EVENT_ID = "migrateCase";
    static final String EVENT_SUMMARY = "Set AdjournmentInProgress to No";
    static final String EVENT_DESCRIPTION = "Set AdjournmentInProgress to No";
    private final String encodedDataString;

    public AdjournmentFlagMigration(
        @Value("${migration.adjournmentFlag.encoded-data-string}") String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateCcdCaseService.UpdateResult migrate(CaseDetails caseDetails) {
        var data = caseDetails.getData();
        if (nonNull(data)) {
            String caseId = caseDetails.getId().toString();
            var caseData = convertToSscsCaseData(caseDetails.getData());
            if (nonNull(caseData) && nonNull(caseData.getAdjournment())) {
                if (nonNull(caseData.getAdjournment().getAdjournmentInProgress())
                    && YesNo.isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
                    log.info("Setting AdjournmentInProgress to No for case id {}", caseId);
                    data.put("adjournmentInProgress", "No");
                } else {
                    log.info(
                        "Skipping case for adjournmentInProgressMigration migration. Case id: {} "
                            + "Reason: AdjournmentInProgress is No or null", caseId);
                    throw new RuntimeException("Skipping case for adjournmentInProgressMigration migration. "
                                                   + "Reason: AdjournmentInProgress is No or null");
                }
            }
        }
        return new UpdateCcdCaseService.UpdateResult(getEventSummary(), getEventDescription());
    }

    public String getEventId() {
        return EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
