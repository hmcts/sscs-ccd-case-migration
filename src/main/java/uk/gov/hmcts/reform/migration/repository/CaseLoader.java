package uk.gov.hmcts.reform.migration.repository;


import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.InflaterOutputStream;

@Slf4j
@Service
@ConditionalOnProperty(value = "migration.elastic.enabled", havingValue = "false")
public class CaseLoader extends CcdRepository {

    private static final String JURISDICTION = "SSCS";
    private static final String ID_COLUMN = "reference";

    private String encodedDataString;

    public CaseLoader(@Value("${migration.waFieldsRemoval.encoded-data-string}") String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    @Override
    public List<CaseDetails> loadCases() {
        List<CaseDetails> cases = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(decompressAndB64Decode(encodedDataString));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            log.info("Number of cases to be migrated: ({})", unprocessed.get());

            data.iterator().forEachRemaining(row -> cases.add(CaseDetails.builder()
                              .jurisdiction(JURISDICTION)
                              .id(((JSONObject) row).getLong(ID_COLUMN))
                              .build()));

        } catch (IOException e) {
            log.info("Failed to load cases from {}", this.getClass().getName());
        }
        return cases;
    }

    private String decompressAndB64Decode(String b64Compressed) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(Base64.getDecoder().decode(b64Compressed));
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
