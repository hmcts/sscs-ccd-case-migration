package uk.gov.hmcts.reform.migration.repository;

import com.nimbusds.jose.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public List<SscsCaseDetails> findCases(boolean hasMultipleColumns) {
        List<SscsCaseDetails> cases = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(decompressAndB64Decode(encodedDataString));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            log.info("Number of cases to be migrated: ({})", unprocessed.get());

            data.iterator().forEachRemaining(row -> {
                JSONObject jsonObject = (JSONObject) row;
                String[] columns = jsonObject.getString("data").split(",");
                cases.add(SscsCaseDetails.builder()
                              .jurisdiction(JURISDICTION)
                              .id(Long.parseLong(columns[0]))
                              .build());
            });

        } catch (IOException e) {
            log.info("Failed to load cases from {}", this.getClass().getName());
        }
        return cases;
    }

    public Map<String, String> findCasesWithHearingID() {
        LinkedHashMap<String, String> mapdata = new LinkedHashMap<>();
        try {
            JSONArray data = new JSONArray(decompressAndB64Decode(encodedDataString));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            data.iterator().forEachRemaining(row -> {
                JSONObject jsonObject = (JSONObject) row;
                String[] columns = jsonObject.getString("data").split(",");
                Arrays.stream(columns).iterator().forEachRemaining(column -> {
                        mapdata.put(columns[0], columns[1]);
                });
            });
        } catch (IOException e) {
            log.info("Failed create mapping for {}", this.getClass().getName());
        }
        return mapdata;
    }

    private static String decompressAndB64Decode(String b64Compressed) throws IOException {
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
