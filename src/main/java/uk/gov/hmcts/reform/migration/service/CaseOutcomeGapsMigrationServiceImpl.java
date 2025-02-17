package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.caseOutcomeGapsMigration.enabled", havingValue = "true")
public class CaseOutcomeGapsMigrationServiceImpl extends CaseMigrationProcessor {

    static final String EVENT_ID = "removeGapsOutcomeTab";
    static final String EVENT_SUMMARY = "Outcome tab removed as it is Gaps case";
    static final String EVENT_DESCRIPTION = "Outcome tab removed as it is Gaps case";

    private final String encodedDataString;

    public CaseOutcomeGapsMigrationServiceImpl(@Value("${migration.caseOutcomeGapsMigration.encoded-data-string}")
                      String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public Map<String, Object> migrate(CaseDetails caseDetails) throws Exception {
        var data = caseDetails.getData();
        if (nonNull(data)) {

            SscsCaseData caseData = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build().convertValue(data, SscsCaseData.class);

            String caseId = caseDetails.getId().toString();

            String hearingRoute = caseDetails.getData().get("hearingRoute").toString();

            if (!hearingRoute.equalsIgnoreCase("gaps")) {
                log.info("Skipping case for case outcome migration. Case id: {} "
                             + "Reason: Hearing Route is not gaps it is {}",
                         caseId, hearingRoute);
                throw new Exception("Skipping case for case outcome migration. Hearing Route is not gaps");
            }

            if (caseData.getCaseOutcome().getCaseOutcome() == null) {

                log.info("Skipping case for case outcome migration. Case id: {} Reason: Case outcome is empty", caseId);
                throw new Exception("Skipping case for case outcome migration. Case outcome is empty");

            } else {
                log.info("case outcome found with value {} and set to null for case id {}",
                         data.get("caseOutcome"), caseId);
                data.put("caseOutcome", null);

                log.info("did Po Attend found with value {} and set to null for case id {}",
                         data.get("didPoAttend"), caseId);
                data.put("didPoAttend", null);

                log.info("Completed migration for case outcome gaps migration. Case id: {}", caseId);
            }
        }
        return data;
    }

    @Override
    public List<CaseDetails> getMigrationCases() {
        return new CaseLoader(encodedDataString).findCases();
    }

    @Override
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
