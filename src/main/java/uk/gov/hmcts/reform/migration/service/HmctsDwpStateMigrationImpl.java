package uk.gov.hmcts.reform.migration.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.hmctsDwpStateMigration.enabled", havingValue = "true")
public class HmctsDwpStateMigrationImpl implements DataMigrationService<Map<String, Object>> {

    static final String EVENT_ID = "clearExpiredFilters";
    static final String EVENT_SUMMARY = "Cleared expired filters";
    static final String EVENT_DESCRIPTION = "Cleared expired filters";

    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    public Map<String, Object> migrate(Map<String, Object> data, CaseDetails caseDetails) throws Exception {
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
            }

            if (!caseDetails.getState().equalsIgnoreCase("voidState")
                && !caseDetails.getState().equalsIgnoreCase("dormantAppealState")) {

                log.info("Skipping case for hmctsDwpState migration. Case id: {} Reason: state is not void or dormant,"
                             + "it is {}", caseId, caseDetails.getState());
                throw new CaseMigrationException("Skipping case for hmctsDwpState migration. State is not void or dormant");

            } else {
                log.info("case {} has hmctsDwpState as failedSendingFurtherEvidence. "
                             + "Removing it and setting it to null", caseId);
                data.put("hmctsDwpState", null);
            }
        }
        return data;
    }

    public String getEventId() {
        return EVENT_ID;
    }

    public String getEventDescription() {
        return EVENT_DESCRIPTION;
    }

    public String getEventSummary() {
        return EVENT_SUMMARY;
    }
}
