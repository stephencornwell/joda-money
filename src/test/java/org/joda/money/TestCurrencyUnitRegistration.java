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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Test CurrencyUnit registration and validation functionality.
 */
class TestCurrencyUnitRegistration {

    @Test
    void testRegisterCurrency_validParameters() {
        var currency = CurrencyUnit.registerCurrency("TST", 813, 2, Arrays.asList("TS"), true);
        
        assertThat(currency).isNotNull();
        assertThat(currency.getCode()).isEqualTo("TST");
        assertThat(currency.getNumericCode()).isEqualTo(813);
        assertThat(currency.getDecimalPlaces()).isEqualTo(2);
        assertThat(currency.getCountryCodes()).contains("TS");
    }

    @Test
    void testRegisterCurrency_invalidCode_tooShort() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("AB", 123, 2, Collections.emptyList(), true))
            .withMessage("Invalid string code, must be length 3");
    }

    @Test
    void testRegisterCurrency_invalidCode_tooLong() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("ABCD", 123, 2, Collections.emptyList(), true))
            .withMessage("Invalid string code, must be length 3");
    }

    @Test
    void testRegisterCurrency_invalidCode_lowercase() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("abc", 123, 2, Collections.emptyList(), true))
            .withMessage("Invalid string code, must be ASCII upper-case letters");
    }

    @Test
    void testRegisterCurrency_invalidCode_numbers() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("A1B", 123, 2, Collections.emptyList(), true))
            .withMessage("Invalid string code, must be ASCII upper-case letters");
    }

    @Test
    void testRegisterCurrency_invalidNumericCode_negative() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("TST", -2, 2, Collections.emptyList(), true))
            .withMessage("Invalid numeric code");
    }

    @Test
    void testRegisterCurrency_invalidNumericCode_tooLarge() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("TST", 1000, 2, Collections.emptyList(), true))
            .withMessage("Invalid numeric code");
    }

    @Test
    void testRegisterCurrency_invalidDecimalPlaces_negative() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("TST", 123, -2, Collections.emptyList(), true))
            .withMessage("Invalid number of decimal places");
    }

    @Test
    void testRegisterCurrency_invalidDecimalPlaces_tooLarge() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("TST", 123, 31, Collections.emptyList(), true))
            .withMessage("Invalid number of decimal places");
    }

    @Test
    void testRegisterCurrency_nullCode() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency(null, 123, 2, Collections.emptyList(), true));
    }

    @Test
    void testRegisterCurrency_nullCountryCodes() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("TST", 123, 2, null, true));
    }

    @Test
    void testRegisterCurrency_duplicateWithoutForce() {
        CurrencyUnit.registerCurrency("DUP", 814, 2, Arrays.asList("DP"), true);
        
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("DUP", 815, 3, Arrays.asList("D2"), false))
            .withMessage("Currency already registered: DUP");
    }

    @Test
    void testRegisterCurrency_duplicateCountryWithoutForce() {
        CurrencyUnit.registerCurrency("DCA", 801, 2, Arrays.asList("DC"), true);
        
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CurrencyUnit.registerCurrency("DCB", 802, 3, Arrays.asList("DC"), false))
            .withMessage("Currency already registered for country: DC");
    }

    @Test
    void testRegisterCurrency_forceReplacement() {
        CurrencyUnit.registerCurrency("FRC", 803, 2, Arrays.asList("FR"), true);
        var original = CurrencyUnit.of("FRC");
        
        var replacement = CurrencyUnit.registerCurrency("FRC", 804, 3, Arrays.asList("F2"), true);
        
        assertThat(replacement.getNumericCode()).isEqualTo(804);
        assertThat(replacement.getDecimalPlaces()).isEqualTo(3);
    }

    @Test
    void testOfNumericCode_string_singleDigit() {
        var currency = CurrencyUnit.ofNumericCode("8");
        assertThat(currency).isNotNull();
        assertThat(currency.getNumericCode()).isEqualTo(8);
    }

    @Test
    void testOfNumericCode_string_invalidLength() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.ofNumericCode("1234"))
            .withMessage("Unknown currency '1234'");
    }

    @Test
    void testOfNumericCode_string_null() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.ofNumericCode((String) null));
    }

    @Test
    void testOfNumericCode_int_unknown() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.ofNumericCode(1234));
    }

    @Test
    void testOf_unknownCode() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of("ZZZ"))
            .withMessage("Unknown currency 'ZZZ'");
    }

    @Test
    void testOf_nullCode() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.of((String) null));
    }

    @Test
    void testOfCountry_unknownCountry() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.ofCountry("ZZ"))
            .withMessage("No currency found for country 'ZZ'");
    }

    @Test
    void testOfCountry_nullCountry() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.ofCountry(null));
    }

    @Test
    void testOf_locale_unknownCountry() {
        assertThatExceptionOfType(IllegalCurrencyException.class)
            .isThrownBy(() -> CurrencyUnit.of(new Locale("en", "ZZ")));
    }

    @Test
    void testOf_locale_null() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.of((Locale) null));
    }

    @Test
    void testPseudoCurrency() {
        var pseudoCurrency = CurrencyUnit.registerCurrency("PSE", 805, -1, Collections.emptyList(), true);
        
        assertThat(pseudoCurrency.isPseudoCurrency()).isTrue();
        assertThat(pseudoCurrency.getDecimalPlaces()).isEqualTo(0);
    }

    @Test
    void testGetSymbol_unknownCurrency() {
        var testCurrency = CurrencyUnit.registerCurrency("UNK", 806, 2, Collections.emptyList(), true);
        
        assertThat(testCurrency.getSymbol()).isEqualTo("UNK");
        assertThat(testCurrency.getSymbol(Locale.US)).isEqualTo("UNK");
    }

    @Test
    void testGetSymbol_nullLocale() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> CurrencyUnit.USD.getSymbol(null));
    }

    @Test
    void testToCurrency_unknownCurrency() {
        var testCurrency = CurrencyUnit.registerCurrency("TCU", 807, 2, Collections.emptyList(), true);
        
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> testCurrency.toCurrency());
    }

    @Test
    void testRegisterCurrency_emptyCountryList() {
        var currency = CurrencyUnit.registerCurrency("EMP", 817, 2, Collections.emptyList(), true);
        
        assertThat(currency.getCountryCodes()).isEmpty();
    }

    @Test
    void testRegisterCurrency_multipleCountries() {
        var currency = CurrencyUnit.registerCurrency("MUL", 818, 2, Arrays.asList("M1", "M2", "M3"), true);
        
        assertThat(currency.getCountryCodes()).containsExactlyInAnyOrder("M1", "M2", "M3");
    }

    @Test
    void testRegisterCurrency_zeroDecimalPlaces() {
        var currency = CurrencyUnit.registerCurrency("ZER", 819, 0, Collections.emptyList(), true);
        
        assertThat(currency.getDecimalPlaces()).isEqualTo(0);
        assertThat(currency.isPseudoCurrency()).isFalse();
    }

    @Test
    void testRegisterCurrency_maxDecimalPlaces() {
        var currency = CurrencyUnit.registerCurrency("MAX", 820, 30, Collections.emptyList(), true);
        
        assertThat(currency.getDecimalPlaces()).isEqualTo(30);
    }

    @Test
    void testRegisterCurrency_minusOneNumericCode() {
        var currency = CurrencyUnit.registerCurrency("MIN", -1, 2, Collections.emptyList(), true);
        
        assertThat(currency.getNumericCode()).isEqualTo(-1);
        assertThat(currency.getNumeric3Code()).isEmpty();
    }

    @Test
    void testRegisterCurrency_maxNumericCode() {
        var currency = CurrencyUnit.registerCurrency("MXN", 816, 2, Collections.emptyList(), true);
        
        assertThat(currency.getNumericCode()).isEqualTo(816);
        assertThat(currency.getNumeric3Code()).isEqualTo("816");
    }

    @Test
    void testGetNumeric3Code_formatting() {
        var currency1 = CurrencyUnit.registerCurrency("CAA", 809, 2, Collections.emptyList(), true);
        var currency2 = CurrencyUnit.registerCurrency("CBB", 810, 2, Collections.emptyList(), true);
        var currency3 = CurrencyUnit.registerCurrency("CCC", 811, 2, Collections.emptyList(), true);
        
        assertThat(currency1.getNumeric3Code()).isEqualTo("809");
        assertThat(currency2.getNumeric3Code()).isEqualTo("810");
        assertThat(currency3.getNumeric3Code()).isEqualTo("811");
    }

    @Test
    void testCurrencyEquality() {
        var currency1 = CurrencyUnit.registerCurrency("EQL", 821, 2, Collections.emptyList(), true);
        var currency2 = CurrencyUnit.of("EQL");
        
        assertThat(currency1).isEqualTo(currency2);
        assertThat(currency1.hashCode()).isEqualTo(currency2.hashCode());
        assertThat(currency1.toString()).isEqualTo("EQL");
    }

    @Test
    void testCurrencyComparison() {
        var currencyA = CurrencyUnit.registerCurrency("AAA", 822, 2, Collections.emptyList(), true);
        var currencyB = CurrencyUnit.registerCurrency("BBB", 823, 2, Collections.emptyList(), true);
        
        assertThat(currencyA.compareTo(currencyB)).isLessThan(0);
        assertThat(currencyB.compareTo(currencyA)).isGreaterThan(0);
        assertThat(currencyA.compareTo(currencyA)).isEqualTo(0);
    }

    @Test
    void testRegisteredCurrenciesContainsNewCurrency() {
        var currency = CurrencyUnit.registerCurrency("REG", 824, 2, Collections.emptyList(), true);
        
        assertThat(CurrencyUnit.registeredCurrencies()).contains(currency);
    }

    @Test
    void testRegisteredCountriesContainsNewCountry() {
        CurrencyUnit.registerCurrency("CTR", 825, 2, Arrays.asList("CT"), true);
        
        assertThat(CurrencyUnit.registeredCountries()).contains("CT");
    }
}
