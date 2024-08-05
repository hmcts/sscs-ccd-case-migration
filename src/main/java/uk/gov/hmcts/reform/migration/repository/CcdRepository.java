package uk.gov.hmcts.reform.migration.repository;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public class CcdRepository {

    @Value("${migration.elasticSearch.enabled}")
    boolean elasticSearchEnabled = false;

    public List<CaseDetails> findCases() {
        return elasticSearchEnabled ? findCaseByCaseType() : loadCases();
    }

    List<CaseDetails> findCaseByCaseType() {
        return List.of();
    }

    List<CaseDetails> loadCases() {
        return List.of();
    }
}
