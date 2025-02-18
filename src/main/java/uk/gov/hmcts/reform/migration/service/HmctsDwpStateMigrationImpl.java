package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.CaseMigrationProcessor;
import uk.gov.hmcts.reform.migration.repository.CaseLoader;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hmctsDwpStateMigration.enabled", havingValue = "true")
public class HmctsDwpStateMigrationImpl extends CaseMigrationProcessor {

    static final String EVENT_ID = "clearExpiredFilters";
    static final String EVENT_SUMMARY = "Cleared expired filters";
    static final String EVENT_DESCRIPTION = "Cleared expired filters";

    private final String encodedDataString;

    public HmctsDwpStateMigrationImpl(@Value("${migration.hmctsDwpStateMigration.encoded-data-string}")
                                               String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    public Map<String, Object> migrate(CaseDetails caseDetails) throws Exception {
        var data = caseDetails.getData();
        if (nonNull(data)) {

            SscsCaseData caseData = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build().convertValue(data, SscsCaseData.class);

            String caseId = caseDetails.getId().toString();

            if (caseData.getHmctsDwpState() == null
                || !caseData.getHmctsDwpState().equalsIgnoreCase("failedSendingFurtherEvidence")) {
                log.info("Skipping case for hmctsDwpState migration. Case id: {} Reason: hmctsDwpState is not"
                             + " 'failedSendingFurtherEvidence' it is {}",
                         caseId, caseData.getHmctsDwpState());
                throw new Exception("Skipping case for hmctsDwpState migration. Reason: hmctsDwpState is not"
                                        + " 'failedSendingFurtherEvidence'");
            } else {
                log.info("case {} has hmctsDwpState as failedSendingFurtherEvidence. "
                             + "Removing it and setting it to null", caseId);
                data.put("hmctsDwpState", null);
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
