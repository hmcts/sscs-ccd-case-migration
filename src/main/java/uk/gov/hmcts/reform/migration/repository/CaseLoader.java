package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

@Slf4j
public class CaseLoader {

    private static final String JURISDICTION = "SSCS";
    private static final String ID_COLUMN = "reference";

    private final String encodedDataString;

    public CaseLoader(String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    public List<SscsCaseDetails> findCases() {
        List<SscsCaseDetails> cases = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(decompressAndB64Decode(encodedDataString));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            log.info("Number of cases to be migrated: ({})", unprocessed.get());

            data.iterator().forEachRemaining(row -> cases.add(SscsCaseDetails.builder()
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
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    public static String compressAndB64Encode(String text) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(text.getBytes());
        }
        return new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }
}
