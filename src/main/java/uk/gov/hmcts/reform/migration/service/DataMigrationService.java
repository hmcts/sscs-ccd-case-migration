package uk.gov.hmcts.reform.migration.service;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public interface DataMigrationService<T> {

    default Predicate<SscsCaseDetails> accepts() {
        return Objects::nonNull;
    }

    List<SscsCaseDetails> getMigrationCases();

    void migrateCases(String caseType);

    void migrate(SscsCaseDetails caseDetails);

    String getEventId();

    String getEventDescription();

    String getEventSummary();
}
