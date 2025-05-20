package uk.gov.hmcts.reform.migration.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.InflaterOutputStream;

import static java.lang.Long.parseLong;
import static java.util.Map.entry;

@Slf4j
public class EncodedStringCaseList {

    private static final String JURISDICTION = "SSCS";
    private static final String ID_COLUMN = "reference";
    private static final String HEARING_ID_COLUMN = "hearingID";

    private EncodedStringCaseList() {
    }

    public static List<SscsCaseDetails> findCases(String encodedDataString) {
        return decompressAndB64Decode(encodedDataString)
            .map(row -> SscsCaseDetails.builder()
                .jurisdiction(JURISDICTION).id(parseLong(row.get(ID_COLUMN))).build())
            .toList();
    }

    public static Map<String, String> mapCaseRefToHearingId(String encodedDataString) {
        Map<String, String> resultMap = new HashMap<>();

        decompressAndB64Decode(encodedDataString)
            .map(row -> entry(
                Optional.ofNullable(row.get(ID_COLUMN)).orElse("").trim(),
                Optional.ofNullable(row.get(HEARING_ID_COLUMN)).orElse("").trim()))
            .forEach(entry -> {
                String reference = entry.getKey();
                if (resultMap.containsKey(reference)) {
                    log.info("Case reference {} is a duplicate. Removing hearing id to skip migration.", reference);
                    resultMap.put(reference, "");
                } else {
                    resultMap.put(reference, entry.getValue());
                }
            });

        return resultMap;
    }

    private static Stream<Map<String, String>> decompressAndB64Decode(String b64Compressed) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(Base64.getDecoder().decode(b64Compressed));
            var caseList = new ObjectMapper()
                .readValue(outputStream.toByteArray(), new TypeReference<List<Map<String, String>>>() {});
            log.info("Number of cases to be migrated: ({})", caseList.size());
            return caseList.stream();
        } catch (IOException e) {
            log.info("Failed to load cases from encode string {}", b64Compressed);
        }
        return Stream.empty();
    }
}
