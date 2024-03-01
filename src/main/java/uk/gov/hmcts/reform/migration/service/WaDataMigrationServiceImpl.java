package uk.gov.hmcts.reform.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@ConditionalOnProperty(value = "migration.wa.enabled", havingValue = "true")
public class WaDataMigrationServiceImpl implements DataMigrationService<Map<String, Object>> {

    private static final String APPEAL = "appeal";
    private static final String APPELLANT = "appellant";
    private static final String IDENTITY = "identity";
    private static final String NINO = "nino";
    private static final String CASE_REFERENCE = "caseReference";

    @Override
    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> data) {
        if (nonNull(data)) {
            data.put("preWorkAllocation", "Yes");
            removeCaseReferenceIfNull(data);
            removeNinoIfNull(data);
        }
        return data;
    }

    private void removeCaseReferenceIfNull(Map<String, Object> data) {
        if (data.containsKey(CASE_REFERENCE) && data.get(CASE_REFERENCE)==null) {
            data.remove(CASE_REFERENCE);
        }
    }

    private void removeNinoIfNull(Map<String, Object> data) {
        if (data.containsKey(APPEAL) && data.get(APPEAL) instanceof Map appeal) {
            if (appeal.containsKey(APPELLANT) && appeal.get(APPELLANT) instanceof Map appellant) {
                if (appellant.containsKey(IDENTITY) && appellant.get(IDENTITY) instanceof Map identity) {
                    if (identity.containsKey(NINO) && identity.get(NINO) == null) {
                        identity.remove(NINO);
                    }
                }
            }
        }
    }
}
