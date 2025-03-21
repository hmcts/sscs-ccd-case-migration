package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
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
import java.util.zip.InflaterOutputStream;

import static java.lang.Long.parseLong;
import static java.util.Map.entry;

@Slf4j
public class CaseLoader {

    private static final String JURISDICTION = "SSCS";
    private static final String ID_COLUMN = "reference";
    private static final String HEARING_ID_COLUMN = "hearingID";

    private final String encodedDataString;

    public CaseLoader(String encodedDataString) {
        this.encodedDataString = encodedDataString;
    }

    public List<SscsCaseDetails> findCases() {
        return decompressAndB64Decode(encodedDataString)
            .map(jsonObj -> SscsCaseDetails.builder()
                .jurisdiction(JURISDICTION)
                .id(parseLong(jsonObj.get(ID_COLUMN)))
                .build())
            .toList();
    }

    public Entry<Map<String, String>, List<SscsCaseDetails>> findCasesWithHearingID() {
        Map<String, String> caseRefToHearingIdMap = new HashMap<>();
        List<SscsCaseDetails> caseList = new ArrayList<>();
        decompressAndB64Decode(encodedDataString).forEach(jsonObj -> {
            caseRefToHearingIdMap.put(jsonObj.get(ID_COLUMN), jsonObj.get(HEARING_ID_COLUMN));
            caseList.add(SscsCaseDetails.builder()
                             .jurisdiction(JURISDICTION).id(parseLong(jsonObj.get(ID_COLUMN))).build());
        });
        return entry(caseRefToHearingIdMap, caseList);
    }

    private Stream<Map<String, String>> decompressAndB64Decode(String b64Compressed) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(Base64.getDecoder().decode(b64Compressed));
            JSONArray data = new JSONArray(outputStream.toString(StandardCharsets.UTF_8));
            AtomicInteger unprocessed = new AtomicInteger(data.length());
            log.info("Number of cases to be migrated: ({})", unprocessed.get());
            return data.toList().stream()
                .map(row -> (Map<String, String>)row);
        } catch (IOException e) {
            log.info("Failed to load cases from {}", this.getClass().getName());
        }
        return Stream.empty();
    }
}
