package org.joda.money.format;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.Locale;

class TestMoneyPrintContext {

    @Test
    void test_constructor_validLocale() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.US);
        assertThat(context.getLocale()).isEqualTo(Locale.US);
    }

    @Test
    void test_constructor_nullLocale() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new MoneyPrintContext(null))
            .withMessage("Locale must not be null");
    }

    @Test
    void test_getLocale() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.FRANCE);
        assertThat(context.getLocale()).isEqualTo(Locale.FRANCE);
    }

    @Test
    void test_setLocale_validLocale() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.US);
        context.setLocale(Locale.GERMANY);
        assertThat(context.getLocale()).isEqualTo(Locale.GERMANY);
    }

    @Test
    void test_setLocale_nullLocale() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.US);
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> context.setLocale(null))
            .withMessage("Locale must not be null");
    }

    @Test
    void test_setLocale_sameLocale() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.JAPAN);
        context.setLocale(Locale.JAPAN);
        assertThat(context.getLocale()).isEqualTo(Locale.JAPAN);
    }

    @Test
    void test_setLocale_multipleChanges() {
        MoneyPrintContext context = new MoneyPrintContext(Locale.US);
        context.setLocale(Locale.UK);
        context.setLocale(Locale.CANADA);
        context.setLocale(Locale.ITALY);
        assertThat(context.getLocale()).isEqualTo(Locale.ITALY);
    }
}
