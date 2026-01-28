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

    private final String encodedDataStringA;
    private final String encodedDataStringB;
    private final String encodedDataStringC;
    private final String encodedDataStringD;
    private final String encodedDataStringE;
    private final String encodedDataStringF;
    private final String encodedDataStringG;
    private final String encodedDataStringH;
    private final String encodedDataStringI;
    private final String encodedDataStringJ;

    private Clock clock = Clock.system(ZoneId.of("Europe/London"));



    public ReadyToListMigration(@Value("${migration.readytolist.encoded-string-a}")
                                 String encodedDataStringA,
                                @Value("${migration.readytolist.encoded-string-b}")
                                 String encodedDataStringB,
                                @Value("${migration.readytolist.encoded-string-c}")
                                 String encodedDataStringC,
                                @Value("${migration.readyToList.encoded-string-d}")
                                 String encodedDataStringD,
                                @Value("${migration.readyToList.encoded-string-e}")
                                 String encodedDataStringE,
                                @Value("${migration.readyToList.encoded-string-f}")
                                 String encodedDataStringF,
                                @Value("${migration.readyToList.encoded-string-g}")
                                 String encodedDataStringG,
                                @Value("${migration.readyToList.encoded-string-h}")
                                 String encodedDataStringH,
                                @Value("${migration.readyToList.encoded-string-i}")
                                 String encodedDataStringI,
                                @Value("${migration.readyToList.encoded-string-j}")
                                 String encodedDataStringJ
    ) {
        this.encodedDataStringA = encodedDataStringA;
        this.encodedDataStringB = encodedDataStringB;
        this.encodedDataStringC = encodedDataStringC;
        this.encodedDataStringD = encodedDataStringD;
        this.encodedDataStringE = encodedDataStringE;
        this.encodedDataStringF = encodedDataStringF;
        this.encodedDataStringG = encodedDataStringG;
        this.encodedDataStringH = encodedDataStringH;
        this.encodedDataStringI = encodedDataStringI;
        this.encodedDataStringJ = encodedDataStringJ;
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
            String failureMsg = format(FAILURE_MSG, caseDetails.getId(), caseDetails.getState());
            log.error(failureMsg);
            throw new IllegalStateException(failureMsg);
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
        return switch (time) {
            case 6 ->  encodedDataStringA;
            case 7 ->  encodedDataStringB;
            case 8 -> encodedDataStringC;
            case 9 -> encodedDataStringD;
            case 10 -> encodedDataStringE;
            case 11 -> encodedDataStringF;
            case 12 -> encodedDataStringG;
            case 13 -> encodedDataStringH;
            case 14 -> encodedDataStringI;
            case 15 -> encodedDataStringJ;
            default -> throw new IllegalStateException("Migration job not configured to run at " + time);
        };
    }
}
