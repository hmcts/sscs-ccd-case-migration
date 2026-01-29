package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

    final Map<Integer, String> secretsMap;

    private Clock clock = Clock.system(ZoneId.of("Europe/London"));



    public ReadyToListMigration(@Value("${migration.readytolist.encoded-case-a}")
                                 String encodedCasesBatch1,
                                @Value("${migration.readytolist.encoded-case-b}")
                                 String encodedCasesBatch2,
                                @Value("${migration.readytolist.encoded-cases-c}")
                                 String encodedCasesBatch3,
                                @Value("${migration.readyToList.encoded-cases-d}")
                                 String encodedCasesBatch4,
                                @Value("${migration.readyToList.encoded-cases-e}")
                                 String encodedCasesBatch5,
                                @Value("${migration.readyToList.encoded-cases-f}")
                                 String encodedCasesBatch6,
                                @Value("${migration.readyToList.encoded-cases-g}")
                                 String encodedCasesBatch7,
                                @Value("${migration.readyToList.encoded-cases-h}")
                                 String encodedCasesBatch8,
                                @Value("${migration.readyToList.encoded-cases-i}")
                                 String encodedCasesBatch9,
                                @Value("${migration.readyToList.encoded-cases-j}")
                                 String encodedCasesBatch10
    ) {
        secretsMap = Map.of(6, encodedCasesBatch1, 7, encodedCasesBatch2, 8, encodedCasesBatch3,
                            9, encodedCasesBatch4, 10, encodedCasesBatch5, 11, encodedCasesBatch6,
                            12, encodedCasesBatch7, 13, encodedCasesBatch8, 14, encodedCasesBatch9,
                            15, encodedCasesBatch10);
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(getEncodedString());
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

    protected String getEncodedString() {
        int time = LocalTime.now(clock).getHour();
        if (secretsMap.containsKey(time)) {
            return secretsMap.get(time);
        } else {
            throw new IllegalStateException("Migration job not configured to run at " + time);
        }
    }
}
