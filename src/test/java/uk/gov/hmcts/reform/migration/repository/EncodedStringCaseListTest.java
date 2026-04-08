package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.domain.common.CaseLoaderHearingDetails;

import static java.lang.String.valueOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.findCases;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToCaseLoaderHearingDetails;
import static uk.gov.hmcts.reform.migration.repository.EncodedStringCaseList.mapCaseRefToHearingId;

public class EncodedStringCaseListTest {

    public static final long ENCODED_CASE_ID = 1729631427870175L;
    public static final String ENCODED_HEARING_ID = "12332445";
    public static final String ENCODED_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VaqNBQC1oglo";
    private static final String ENCODED_HEARING_STRING = "eJyLrlYqSk1LLUrNS05VslIyNDeyNDM2NDEytzA3MDQ3VdJRykhNLMrMS89"
        + "MAUkbGRsbmZiYKtXGAgDaSA+Z";
    private static final String ENCODED_CASE_LOADER_HEARING_DETAILS_STRING = "eJxFjUsKAjEQRK8ivXag04mJM2cQV+7ERTCtRjCBfIRh8O4mIgz"
        + "0qt6r6vMCiW+cOFwZJhCGRi2FIrM3KMwOtmDdM9YU2DV8jC14sE0+3J0tvUFIekA1oGnozaGy76YgPa5u8a/fupgQ263g70pJSvVnudhSc8s"
        + "2ia2bT/Hgc4HP5QucrzBA";
    private static final String INVALID_ENCODED_DATA_STRING = "xxxxxxxxxxxxxxx";

    @Test
    void givenValidEncodedString_thenReturnListOfCases() {
        var result = findCases(ENCODED_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getFirst().getId()).isEqualTo(ENCODED_CASE_ID);
        assertThat(result.getFirst().getJurisdiction()).isEqualTo("SSCS");
    }

    @Test
    void givenValidEncodedStringWithHearingId_thenReturnListOfCasesAndHearingIdMap() {
        var result = mapCaseRefToHearingId(ENCODED_HEARING_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.get(valueOf(ENCODED_CASE_ID))).isEqualTo(ENCODED_HEARING_ID);
    }

    @Test
    void givenValidEncodedStringWithCaseloaderHearingDetails_thenReturnListOfCasesAndHearingDetailsMap() {
        var result = mapCaseRefToCaseLoaderHearingDetails(
            ENCODED_CASE_LOADER_HEARING_DETAILS_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();

        CaseLoaderHearingDetails caseLoaderHearingDetails = result.get(valueOf(ENCODED_CASE_ID));
        assertThat(caseLoaderHearingDetails.getVenueId()).isEqualTo("1269");
        assertThat(caseLoaderHearingDetails.getHearingId()).isEqualTo(ENCODED_HEARING_ID);
        assertThat(caseLoaderHearingDetails.getHearingDate()).isEqualTo("2026-04-07");
        assertThat(caseLoaderHearingDetails.getHearingTime()).isEqualTo("11:00:00");
        assertThat(caseLoaderHearingDetails.getHearingAdjourned()).isEqualTo("No");
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyList() {
        var result = findCases(INVALID_ENCODED_DATA_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyListAndMap() {
        var result = mapCaseRefToHearingId(INVALID_ENCODED_DATA_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void givenInvalidEncodedString_thenReturnEmptyCaseLoaderHearingDetails() {
        var result = mapCaseRefToCaseLoaderHearingDetails(INVALID_ENCODED_DATA_STRING);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }
}
