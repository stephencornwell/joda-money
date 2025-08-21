/*
 *  Copyright 2009-present, Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Test CurrencyUnit.
 */
class TestCurrencyUnitExtension {

    @Test
    void test_CurrencyFromMoneyData() {
        var curList = CurrencyUnit.registeredCurrencies();
        var found = false;
        for (CurrencyUnit currencyUnit : curList) {
            if (currencyUnit.getCode().equals("GBP")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void test_CurrencyFromMoneyDataExtension() {
        var curList = CurrencyUnit.registeredCurrencies();
        var found = false;
        for (CurrencyUnit currencyUnit : curList) {
            if (currencyUnit.getCode().equals("BTC")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void test_LargerDecimalPrecisionCurrencyFromMoneyDataExtension() {
        var curList = CurrencyUnit.registeredCurrencies();
        var found = false;
        for (CurrencyUnit currencyUnit : curList) {
            if (currencyUnit.getCode().equals("ETH")) {
                found = true;
                assertThat(Money.of(currencyUnit, 1.23456789d).toString()).isEqualTo("ETH 1.234567890000000000000000000000");
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void test_InvalidLargerDecimalPrecisionCurrencyFromMoneyDataExtension() {
        for (CurrencyUnit currencyUnit : CurrencyUnit.registeredCurrencies()) {
            if (currencyUnit.getCode().equals("XXL")) {
                fail("Currency XXL should not exist");
            }
        }
    }

    @Test
    void test_CurrencyMissing() {
        for (CurrencyUnit currencyUnit : CurrencyUnit.registeredCurrencies()) {
            if (currencyUnit.getCode().equals("NMC")) {
                fail("Currency NMC should not exist");
            }
        }
    }

    @Test
    void test_CurrencyEURChanged() {
        var currency = CurrencyUnit.ofCountry("HU");
        assertThat(currency).isEqualTo(CurrencyUnit.EUR);
        assertThat(CurrencyUnit.EUR.getCountryCodes()).contains("HU");
        assertThat(CurrencyUnit.of("HUF").getCountryCodes()).isEmpty();
    }

    @Test
    void test_of_Locale_validCountry() {
        CurrencyUnit currency = CurrencyUnit.of(Locale.US);
        assertThat(currency).isEqualTo(CurrencyUnit.USD);
    }

    @Test
    void test_of_Locale_validCountryGB() {
        CurrencyUnit currency = CurrencyUnit.of(Locale.UK);
        assertThat(currency).isEqualTo(CurrencyUnit.GBP);
    }

    @Test
    void test_of_Locale_invalidCountry() {
        Locale invalidLocale = new Locale("en", "XX");
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of(invalidLocale))
            .withMessageContaining("No currency found for locale");
    }

    @Test
    void test_of_Locale_null() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.of((Locale) null))
            .withMessageContaining("Locale must not be null");
    }

    @Test
    void test_getSymbol_USD() {
        CurrencyUnit usd = CurrencyUnit.USD;
        String symbol = usd.getSymbol();
        assertThat(symbol).isNotNull();
        assertThat(symbol).isNotEmpty();
    }

    @Test
    void test_getSymbol_GBP() {
        CurrencyUnit gbp = CurrencyUnit.GBP;
        String symbol = gbp.getSymbol();
        assertThat(symbol).isNotNull();
        assertThat(symbol).isNotEmpty();
    }

    @Test
    void test_getSymbol_XXX() {
        CurrencyUnit xxx = CurrencyUnit.of("XXX");
        String symbol = xxx.getSymbol();
        assertThat(symbol).isEqualTo("XXX");
    }

    @Test
    void test_getSymbol_Locale_USD() {
        CurrencyUnit usd = CurrencyUnit.USD;
        String symbol = usd.getSymbol(Locale.US);
        assertThat(symbol).isNotNull();
        assertThat(symbol).isNotEmpty();
    }

    @Test
    void test_getSymbol_Locale_null() {
        CurrencyUnit usd = CurrencyUnit.USD;
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> usd.getSymbol(null))
            .withMessageContaining("Locale must not be null");
    }

    @Test
    void test_getSymbol_Locale_XXX() {
        CurrencyUnit xxx = CurrencyUnit.of("XXX");
        String symbol = xxx.getSymbol(Locale.US);
        assertThat(symbol).isEqualTo("XXX");
    }

    @Test
    void test_isPseudoCurrency_false() {
        CurrencyUnit usd = CurrencyUnit.USD;
        assertThat(usd.isPseudoCurrency()).isFalse();
    }

    @Test
    void test_isPseudoCurrency_false_GBP() {
        CurrencyUnit gbp = CurrencyUnit.GBP;
        assertThat(gbp.isPseudoCurrency()).isFalse();
    }

    @Test
    void test_isPseudoCurrency_true() {
        boolean foundPseudoCurrency = false;
        for (CurrencyUnit currency : CurrencyUnit.registeredCurrencies()) {
            if (currency.isPseudoCurrency()) {
                foundPseudoCurrency = true;
                assertThat(currency.isPseudoCurrency()).isTrue();
                break;
            }
        }
        assertThat(foundPseudoCurrency).as("Should find at least one pseudo-currency").isTrue();
    }

    @Test
    void test_getCountryCodes_USD() {
        CurrencyUnit usd = CurrencyUnit.USD;
        Set<String> countryCodes = usd.getCountryCodes();
        assertThat(countryCodes).isNotNull();
        assertThat(countryCodes).contains("US");
    }

    @Test
    void test_getCountryCodes_GBP() {
        CurrencyUnit gbp = CurrencyUnit.GBP;
        Set<String> countryCodes = gbp.getCountryCodes();
        assertThat(countryCodes).isNotNull();
        assertThat(countryCodes).contains("GB");
    }

    @Test
    void test_getCountryCodes_EUR() {
        CurrencyUnit eur = CurrencyUnit.EUR;
        Set<String> countryCodes = eur.getCountryCodes();
        assertThat(countryCodes).isNotNull();
        assertThat(countryCodes.size()).isGreaterThan(1);
    }

    @Test
    void test_toCurrency_USD() {
        CurrencyUnit usd = CurrencyUnit.USD;
        Currency jdkCurrency = usd.toCurrency();
        assertThat(jdkCurrency).isNotNull();
        assertThat(jdkCurrency.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void test_toCurrency_GBP() {
        CurrencyUnit gbp = CurrencyUnit.GBP;
        Currency jdkCurrency = gbp.toCurrency();
        assertThat(jdkCurrency).isNotNull();
        assertThat(jdkCurrency.getCurrencyCode()).isEqualTo("GBP");
    }

    @Test
    void test_toCurrency_invalidCurrency() {
        for (CurrencyUnit currency : CurrencyUnit.registeredCurrencies()) {
            if (currency.getCode().equals("BTC")) {
                assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> currency.toCurrency());
                break;
            }
        }
    }

    @Test
    void test_compareTo_same() {
        CurrencyUnit usd1 = CurrencyUnit.USD;
        CurrencyUnit usd2 = CurrencyUnit.USD;
        assertThat(usd1.compareTo(usd2)).isEqualTo(0);
    }

    @Test
    void test_compareTo_different() {
        CurrencyUnit usd = CurrencyUnit.USD;
        CurrencyUnit gbp = CurrencyUnit.GBP;
        assertThat(usd.compareTo(gbp)).isGreaterThan(0);
        assertThat(gbp.compareTo(usd)).isLessThan(0);
    }

    @Test
    void test_compareTo_alphabetical() {
        CurrencyUnit eur = CurrencyUnit.EUR;
        CurrencyUnit usd = CurrencyUnit.USD;
        assertThat(eur.compareTo(usd)).isLessThan(0);
        assertThat(usd.compareTo(eur)).isGreaterThan(0);
    }

    @Test
    void test_registeredCountries() {
        var countries = CurrencyUnit.registeredCountries();
        assertThat(countries).isNotNull();
        assertThat(countries).isNotEmpty();
        assertThat(countries).contains("US");
        assertThat(countries).contains("GB");
    }

}
