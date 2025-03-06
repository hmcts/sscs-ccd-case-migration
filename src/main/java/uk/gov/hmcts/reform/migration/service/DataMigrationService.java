package uk.gov.hmcts.reform.migration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.UpdateResult;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public interface DataMigrationService {

    default Predicate<SscsCaseDetails> accepts() {
        return Objects::nonNull;
    }

    List<SscsCaseDetails> fetchCasesToMigrate();

    void migrateCases();

    UpdateResult migrate(CaseDetails caseDetails);

    String getEventId();

    String getEventDescription();

    String getEventSummary();
}
