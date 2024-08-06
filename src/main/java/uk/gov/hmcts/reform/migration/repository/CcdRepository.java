package uk.gov.hmcts.reform.migration.repository;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.List;

public abstract class CcdRepository {

    public abstract List<CaseDetails> findCases();
}
