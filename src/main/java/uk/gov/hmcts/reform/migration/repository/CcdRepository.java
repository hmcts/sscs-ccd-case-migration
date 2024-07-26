package uk.gov.hmcts.reform.migration.repository;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public interface CcdRepository {
    default List<CaseDetails> findCaseByCaseType(String userToken, String caseType) {
        return List.of();
    }

    default List<CaseDetails> loadCases() {
        return List.of();
    }
}
