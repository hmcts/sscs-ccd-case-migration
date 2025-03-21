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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import static java.util.Map.entry;

@Slf4j
public class CaseLoader {

    private static final String JURISDICTION = "SSCS";
    private static final String ID_COLUMN = "reference";
    private static final String HEARING_ID_COLUMN = "reference";

    private final String encodedDataString;

    public CaseLoader(String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    public List<SscsCaseDetails> findCases() {
        return decompressAndB64Decode(encodedDataString)
            .map(jsonObj -> SscsCaseDetails.builder()
                .jurisdiction(JURISDICTION)
                .id(jsonObj.getLong(ID_COLUMN))
                .build())
            .toList();
    }

    public Entry<Map<Long, Long>, List<SscsCaseDetails>> findCasesWithHearingID() {
        Map<Long, Long> caseRefToHearingIdMap = new HashMap<>();
        List<SscsCaseDetails> caseList = new ArrayList<>();
        decompressAndB64Decode(encodedDataString).forEach(jsonObj -> {
            caseRefToHearingIdMap.put(jsonObj.getLong(ID_COLUMN), jsonObj.getLong(HEARING_ID_COLUMN));
            caseList.add(SscsCaseDetails.builder().jurisdiction(JURISDICTION).id(jsonObj.getLong(ID_COLUMN)).build());
        });
        return entry(caseRefToHearingIdMap, caseList);
    }

    private Stream<JSONObject> decompressAndB64Decode(String b64Compressed) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(Base64.getDecoder().decode(b64Compressed));
            JSONArray data = new JSONArray(outputStream.toString(StandardCharsets.UTF_8));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            log.info("Number of cases to be migrated: ({})", unprocessed.get());
            return data.toList().stream()
                .map(row -> (JSONObject)row);
        } catch (IOException e) {
            log.info("Failed to load cases from {}", this.getClass().getName());
        }
        return Stream.empty();
    }

    public static String compressAndB64Encode(String text) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(text.getBytes());
        }
        return new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }
}
