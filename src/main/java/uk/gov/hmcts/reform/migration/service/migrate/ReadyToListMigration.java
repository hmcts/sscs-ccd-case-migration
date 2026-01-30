package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.readytolist.enabled", havingValue = "true")
public class ReadyToListMigration extends CaseMigrationProcessor {

    static final String READY_TO_LIST_MIGRATION_EVENT_ID = "readyToList";
    static final String READY_TO_LIST_MIGRATION_EVENT_SUMMARY = "Move case state to ready to list";
    static final String READY_TO_LIST_MIGRATION_EVENT_DESCRIPTION = "";
    static final String CALLBACK_WARNING_FIELD = "ignoreCallbackWarnings";
    static final String FAILURE_MSG = "Skipping Case (%s) for migration due to incorrect state: (%s)";

    private final String encodedDataString;

    public ReadyToListMigration(@Value("${migration.readytolist.encoded-data-string}")
                                 String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        if (Objects.equals(caseDetails.getState(), RESPONSE_RECEIVED.toString())) {
            caseDetails.getData().put(CALLBACK_WARNING_FIELD, "Yes");
            log.info("Setting ignoreCallbackWarning field to (Yes) for case {}", caseDetails.getId());
            return new UpdateResult(getEventSummary(), getEventDescription());

        } else {
            String skipMsg = format(FAILURE_MSG, caseDetails.getId(), caseDetails.getState());
            log.error(skipMsg);
            throw new IllegalStateException(skipMsg);
        }

    }

    @Override
    public String getEventId() {
        return READY_TO_LIST_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return READY_TO_LIST_MIGRATION_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return READY_TO_LIST_MIGRATION_EVENT_SUMMARY;
    }

}
