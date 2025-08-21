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
package org.joda.money.banking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.money.CurrencyMismatchException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Advanced tests for BankingOperations covering AML compliance, concurrency, and edge cases.
 */
class TestBankingOperationsAdvanced {

    private BankingOperations bankingOps;

    @BeforeEach
    void setUp() {
        bankingOps = new BankingOperations();
        bankingOps.resetDailyTotals();
    }

    @Test
    void testTransferBelowAmlThreshold() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD 5000"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 5000"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testTransferExactlyAtAmlThreshold() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD 10000"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 10000"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testTransferExceedsAmlThreshold() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD 10000.01"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 10000.01"));
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testMultipleTransfersReachAmlThreshold() {
        var result1 = bankingOps.processWireTransfer("account123", Money.parse("USD 6000"));
        var result2 = bankingOps.processWireTransfer("account123", Money.parse("USD 3000"));
        var result3 = bankingOps.processWireTransfer("account123", Money.parse("USD 1000"));
        var result4 = bankingOps.processWireTransfer("account123", Money.parse("USD 0.01"));
        
        assertThat(result1.requiresAmlReview).isFalse();
        assertThat(result2.requiresAmlReview).isFalse();
        assertThat(result3.requiresAmlReview).isFalse();
        assertThat(result4.requiresAmlReview).isTrue();
        
        assertThat(result1.transactionNumber).isEqualTo(1);
        assertThat(result2.transactionNumber).isEqualTo(2);
        assertThat(result3.transactionNumber).isEqualTo(3);
        assertThat(result4.transactionNumber).isEqualTo(4);
    }

    @Test
    void testDifferentAccountsIndependentAmlTracking() {
        var result1 = bankingOps.processWireTransfer("account123", Money.parse("USD 9000"));
        var result2 = bankingOps.processWireTransfer("account456", Money.parse("USD 9000"));
        var result3 = bankingOps.processWireTransfer("account123", Money.parse("USD 1500"));
        var result4 = bankingOps.processWireTransfer("account456", Money.parse("USD 500"));
        
        assertThat(result1.requiresAmlReview).isFalse();
        assertThat(result2.requiresAmlReview).isFalse();
        assertThat(result3.requiresAmlReview).isTrue();
        assertThat(result4.requiresAmlReview).isFalse();
    }

    @Test
    void testDailyTotalsReset() {
        bankingOps.processWireTransfer("account123", Money.parse("USD 8000"));
        bankingOps.processWireTransfer("account456", Money.parse("USD 5000"));
        
        bankingOps.resetDailyTotals();
        
        var result1 = bankingOps.processWireTransfer("account123", Money.parse("USD 9000"));
        var result2 = bankingOps.processWireTransfer("account456", Money.parse("USD 9000"));
        
        assertThat(result1.requiresAmlReview).isFalse();
        assertThat(result2.requiresAmlReview).isFalse();
        assertThat(result1.transactionNumber).isEqualTo(1);
        assertThat(result2.transactionNumber).isEqualTo(2);
    }

    @Test
    void testTransactionCounterIncrementsCorrectly() {
        var result1 = bankingOps.processWireTransfer("account1", Money.parse("USD 100"));
        var result2 = bankingOps.processWireTransfer("account2", Money.parse("USD 200"));
        var result3 = bankingOps.processWireTransfer("account3", Money.parse("USD 300"));
        
        assertThat(result1.transactionNumber).isEqualTo(1);
        assertThat(result2.transactionNumber).isEqualTo(2);
        assertThat(result3.transactionNumber).isEqualTo(3);
    }

    @Test
    void testTransferResultComplianceLogic() {
        var result1 = bankingOps.processWireTransfer("account123", Money.parse("USD 5000"));
        var result2 = bankingOps.processWireTransfer("account456", Money.parse("USD 15000"));
        
        assertThat(result1.isCompliant()).isTrue();
        assertThat(result2.isCompliant()).isTrue();
        
        var manualResult = new BankingOperations.TransferResult();
        manualResult.requiresAmlReview = true;
        manualResult.transactionNumber = 0;
        assertThat(manualResult.isCompliant()).isFalse();
        
        manualResult.transactionNumber = 1;
        assertThat(manualResult.isCompliant()).isTrue();
    }

    @Test
    void testConcurrentTransfersFromSameAccount() throws InterruptedException {
        final String accountId = "concurrent-account";
        final int numThreads = 20;
        final Money transferAmount = Money.parse("USD 600");
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicInteger amlReviewCount = new AtomicInteger(0);
        final AtomicInteger totalTransactions = new AtomicInteger(0);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    var result = bankingOps.processWireTransfer(accountId, transferAmount);
                    totalTransactions.incrementAndGet();
                    if (result.requiresAmlReview) {
                        amlReviewCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(exception.get()).isNull();
        assertThat(totalTransactions.get()).isEqualTo(numThreads);
        assertThat(amlReviewCount.get()).isGreaterThan(0);
    }

    @Test
    void testConcurrentTransfersFromDifferentAccounts() throws InterruptedException {
        final int numThreads = 20;
        final Money transferAmount = Money.parse("USD 8000");
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicInteger amlReviewCount = new AtomicInteger(0);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            final String accountId = "account-" + i;
            executor.submit(() -> {
                try {
                    var result = bankingOps.processWireTransfer(accountId, transferAmount);
                    if (result.requiresAmlReview) {
                        amlReviewCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(exception.get()).isNull();
        assertThat(amlReviewCount.get()).isEqualTo(0);
    }

    @Test
    void testTransferWithDifferentCurrencies() {
        var result1 = bankingOps.processWireTransfer("account123", Money.parse("EUR 8000"));
        
        assertThat(result1.requiresAmlReview).isFalse();
        
        assertThatExceptionOfType(CurrencyMismatchException.class)
            .isThrownBy(() -> bankingOps.processWireTransfer("account123", Money.parse("USD 3000")))
            .withMessage("Currencies differ: EUR/USD");
    }

    @Test
    void testZeroAmountTransfer() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD 0"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 0"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testNegativeAmountTransfer() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD -5000"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD -5000"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testLargeTransferAmount() {
        var result = bankingOps.processWireTransfer("account123", Money.parse("USD 1000000"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 1000000"));
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testEmptyAccountId() {
        var result = bankingOps.processWireTransfer("", Money.parse("USD 5000"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 5000"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
        assertThat(result.isCompliant()).isTrue();
    }

    @Test
    void testNullAccountId() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> bankingOps.processWireTransfer(null, Money.parse("USD 5000")));
    }

    @Test
    void testNullTransferAmount() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> bankingOps.processWireTransfer("account123", null));
    }

    @Test
    void testTransferWithPseudoCurrency() {
        var pseudoCurrency = CurrencyUnit.registerCurrency("PSC", 812, -1, java.util.Collections.emptyList(), true);
        
        assertThatExceptionOfType(ArithmeticException.class)
            .isThrownBy(() -> Money.of(pseudoCurrency, 5000))
            .withMessage("Scale of amount 5000.0 is greater than the scale of the currency PSC");
    }

    @Test
    void testAmlThresholdWithDifferentCurrencyPrecision() {
        assertThatExceptionOfType(CurrencyMismatchException.class)
            .isThrownBy(() -> bankingOps.processWireTransfer("account123", Money.parse("JPY 1000000")))
            .withMessage("Currencies differ: JPY/USD");
    }
}
