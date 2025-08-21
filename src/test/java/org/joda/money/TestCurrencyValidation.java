package org.joda.money;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class TestCurrencyValidation {

    @Test
    void test_invalidCurrencyCode_empty() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of(""));
    }

    @Test
    void test_invalidCurrencyCode_tooShort() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("AB"));
    }

    @Test
    void test_invalidCurrencyCode_tooLong() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("ABCD"));
    }

    @Test
    void test_invalidCurrencyCode_lowercase() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("usd"));
    }

    @Test
    void test_invalidCurrencyCode_mixedCase() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("UsD"));
    }

    @Test
    void test_invalidCurrencyCode_numbers() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("U5D"));
    }

    @Test
    void test_invalidCurrencyCode_specialChars() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("U$D"));
    }

    @Test
    void test_invalidCurrencyCode_null() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.of((String) null));
    }

    @Test
    void test_unknownCurrencyCode() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("ZZZ"));
    }

    @Test
    void test_validCurrencyCode_boundaries() {
        CurrencyUnit usd = CurrencyUnit.of("USD");
        assertThat(usd.getCode()).isEqualTo("USD");
        assertThat(usd.getNumericCode()).isEqualTo(840);
        assertThat(usd.getDecimalPlaces()).isEqualTo(2);
    }

    @Test
    void test_currencyMismatch_differentCurrencies() {
        Money usd = Money.parse("USD 100");
        Money eur = Money.parse("EUR 100");
        
        assertThatExceptionOfType(CurrencyMismatchException.class)
            .isThrownBy(() -> usd.plus(eur));
    }

    @Test
    void test_currencyMismatch_message() {
        Money usd = Money.parse("USD 100");
        Money eur = Money.parse("EUR 100");
        
        assertThatExceptionOfType(CurrencyMismatchException.class)
            .isThrownBy(() -> usd.plus(eur))
            .withMessageContaining("USD")
            .withMessageContaining("EUR");
    }
}
