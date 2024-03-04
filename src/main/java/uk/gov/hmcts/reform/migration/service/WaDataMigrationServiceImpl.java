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

    @Override
    public Predicate<CaseDetails> accepts() {
        return Objects::nonNull;
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> data) {
        if (nonNull(data)) {
            data.put("preWorkAllocation", "Yes");
        }
        return data;
    }
}
