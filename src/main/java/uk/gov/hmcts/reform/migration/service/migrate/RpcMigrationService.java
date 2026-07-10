package uk.gov.hmcts.reform.migration.service.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.CaseMigrationProcessor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.rpc.enabled", havingValue = "true")
public class RpcMigrationService extends CaseMigrationProcessor {

    static final String RPC_MIGRATION_EVENT_ID = "migrateCase";
    static final String RPC_MIGRATION_EVENT_SUMMARY =
        "Leeds RPC Migration";
    static final String RPC_MIGRATION_EVENT_DESCRIPTION = "Leeds RPC Migration";
    static final String REGION_FIELD = "region";
    static final String RPC_FIELD = "regionalProcessingCenter";
    static final String REGION_TO_MIGRATE = "BRADFORD";
    static final String NEW_REGION = "LEEDS";
    static final String HEARING_ROUTE_TO_SKIP = HearingRoute.LIST_ASSIST.getState();
    static final String INVALID_RPC_FAILURE_MSG = "Skipping case %s for migration because the "
        + "venue is not %s";
    static final String INVALID_HEARING_ROUTE_FAILURE_MSG = "Skipping Case %s for migration because the hearing route "
        + "is %s";
    static final String NULL_HEARING_ROUTE_FAILURE_MSG = "Skipping Case %s for migration because there is no SC "
        + "reference and the hearing route is null";
    static final RegionalProcessingCenter LEEDS_RPC = RegionalProcessingCenter.builder()
        .name("LEEDS")
                .address1("HM Courts & Tribunals Service")
                .address2("Social Security & Child Support Appeals")
                .address3("York House")
                .address4("31 York Place")
                .city("LEEDS")
                .postcode("LS1 2ED")
                .phoneNumber("0300 123 1142")
                .faxNumber("0870 739 4331")
                .email("Leeds_SYA_Response@justice.gov.uk")
                .hearingRoute(HearingRoute.GAPS)
                .epimsId("495952")
                .build();
    private final String encodedDataString;

    public RpcMigrationService(@Value("${migration.rpc.encoded-string}")
                                 String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<SscsCaseDetails> fetchCasesToMigrate() {
        return findCases(encodedDataString);
    }

    @Override
    public UpdateResult migrate(CaseDetails caseDetails) {
        validateCase(caseDetails);

        caseDetails.getData().put(REGION_FIELD, NEW_REGION);
        caseDetails.getData().put(RPC_FIELD, LEEDS_RPC);
        log.info("Setting rpc for case {} to {}", caseDetails.getId(), NEW_REGION);
        return new UpdateResult(getEventSummary(), getEventDescription());
    }

    @Override
    public String getEventId() {
        return RPC_MIGRATION_EVENT_ID;
    }

    @Override
    public String getEventDescription() {
        return RPC_MIGRATION_EVENT_DESCRIPTION;
    }

    @Override
    public String getEventSummary() {
        return RPC_MIGRATION_EVENT_SUMMARY;
    }

    private void validateCase(CaseDetails caseDetails) {
        validateCaseVenue(caseDetails);
        validateCaseHearingRoute(caseDetails);
    }

    private void validateCaseVenue(CaseDetails caseDetails) {
        log.info("Validating case {} for migration. Current Region: {}, Current RPC: {}", caseDetails.getId(),
                 caseDetails.getData().get(REGION_FIELD), caseDetails.getData().get(RPC_FIELD));
        if (!REGION_TO_MIGRATE.equals(caseDetails.getData().get(REGION_FIELD))) {
            String skipMsg = format(INVALID_RPC_FAILURE_MSG, caseDetails.getId(), REGION_TO_MIGRATE);
            log.info(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
    }

    private void validateCaseHearingRoute(CaseDetails caseDetails) {
        log.info("Validating case {} for migration. Current hearing route: {}", caseDetails.getId(),
                 caseDetails.getData().get("hearingRoute"));
        if (HEARING_ROUTE_TO_SKIP.equals(caseDetails.getData().get("hearingRoute"))) {
            String skipMsg = format(INVALID_HEARING_ROUTE_FAILURE_MSG, caseDetails.getId(),
                                    caseDetails.getData().get("hearingRoute"));
            log.info(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
        if (isNull(caseDetails.getData().get("hearingRoute")) && isNull(caseDetails.getData().get("caseReference"))) {
            String skipMsg = format(NULL_HEARING_ROUTE_FAILURE_MSG, caseDetails.getId());
            log.info(skipMsg);
            throw new IllegalStateException(skipMsg);
        }
    }
}
