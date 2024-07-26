package uk.gov.hmcts.reform.migration.repository;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(value = "migration.elastic.enabled", havingValue = "false")
public class CaseLoader implements CcdRepository {

    @Value("${migration.cases-file-name}")
    private String caseFile;

    public List<CaseDetails> loadCases() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(caseFile);
            log.info("Load cases from {} {}", caseFile, stream);
            List<String> cases = objectMapper.readValue(stream, new TypeReference<>() {});
            return cases.stream().map(this::process).toList();
        } catch (IOException e) {
            log.info("Failed to load cases from {}", this.getClass().getName());
        }
        return List.of();
    }

    private CaseDetails process(String caseId) {
        return CaseDetails.builder()
            .id(Long.valueOf(caseId))
            .jurisdiction("SSCS")
            .build();
    }
}
