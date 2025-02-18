package uk.gov.hmcts.reform.migration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public interface DataMigrationService<T> {

    default Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    List<CaseDetails> getMigrationCases();

    void migrateCases(String caseType);

    T migrate(CaseDetails caseDetails) throws Exception;

    String getEventId();

    String getEventDescription();

    String getEventSummary();
}
