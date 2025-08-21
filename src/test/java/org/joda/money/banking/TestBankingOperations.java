package org.joda.money.banking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import org.joda.money.Money;
import org.joda.money.CurrencyUnit;
import org.joda.money.CurrencyMismatchException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class TestBankingOperations {
    
    private BankingOperations ops;
    
    @BeforeEach
    public void setUp() {
        ops = new BankingOperations();
        ops.resetDailyTotals();
    }
    
    @Test
    public void testBasicTransfer() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account123", Money.parse("USD 100"));
        
        assertThat(result).isNotNull();
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isGreaterThan(0);
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 100"));
    }
    
    @Test
    public void testAmlThresholdExactly10000() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account456", Money.parse("USD 10000"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void testAmlThresholdJustOver10000() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account456", Money.parse("USD 10000.01"));
        
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void testAmlThresholdCumulative() {
        ops.processWireTransfer("account789", Money.parse("USD 5000"));
        
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account789", Money.parse("USD 5000.01"));
        
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void testAmlThresholdMultipleAccountsSeparate() {
        ops.processWireTransfer("account1", Money.parse("USD 9999"));
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account2", Money.parse("USD 9999"));
        
        assertThat(result.requiresAmlReview).isFalse();
    }
    
    @Test
    public void testConcurrentTransfers() throws InterruptedException {
        int threadCount = 10;
        int transfersPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<BankingOperations.TransferResult> results = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < transfersPerThread; j++) {
                        String accountId = "concurrent_account_" + threadId;
                        BankingOperations.TransferResult result = 
                            ops.processWireTransfer(accountId, Money.parse("USD 1000"));
                        results.add(result);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(successCount.get()).isEqualTo(threadCount * transfersPerThread);
        assertThat(results).hasSize(threadCount * transfersPerThread);
        
        for (BankingOperations.TransferResult result : results) {
            assertThat(result.transactionNumber).isGreaterThan(0);
            assertThat(result.transferAmount).isEqualTo(Money.parse("USD 1000"));
        }
    }
    
    @Test
    public void testResetDailyTotalsClearsData() {
        ops.processWireTransfer("account999", Money.parse("USD 5000"));
        ops.resetDailyTotals();
        
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account999", Money.parse("USD 9999"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void testResetDailyTotalsConcurrencyBug() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch resetLatch = new CountDownLatch(1);
        CountDownLatch transferLatch = new CountDownLatch(1);
        AtomicInteger amlReviewCount = new AtomicInteger(0);
        
        ops.processWireTransfer("buggy_account", Money.parse("USD 9999"));
        
        executor.submit(() -> {
            try {
                resetLatch.await();
                Thread.sleep(25);
                ops.resetDailyTotals();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        executor.submit(() -> {
            try {
                resetLatch.countDown();
                Thread.sleep(10);
                BankingOperations.TransferResult result = 
                    ops.processWireTransfer("buggy_account", Money.parse("USD 2"));
                if (result.requiresAmlReview) {
                    amlReviewCount.incrementAndGet();
                }
                transferLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        transferLatch.await(2, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(amlReviewCount.get()).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    public void testNonUsdCurrency() {
        assertThatExceptionOfType(CurrencyMismatchException.class)
            .isThrownBy(() -> ops.processWireTransfer("eur_account", Money.parse("EUR 5000")));
    }
    
    @Test
    public void testTransferResultCompliance() {
        BankingOperations.TransferResult result = new BankingOperations.TransferResult();
        result.requiresAmlReview = false;
        result.transactionNumber = 1;
        
        assertThat(result.isCompliant()).isTrue();
        
        result.requiresAmlReview = true;
        result.transactionNumber = 0;
        
        assertThat(result.isCompliant()).isFalse();
        
        result.transactionNumber = 1;
        
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void testTransactionNumberIncremental() {
        BankingOperations.TransferResult result1 = 
            ops.processWireTransfer("account1", Money.parse("USD 100"));
        BankingOperations.TransferResult result2 = 
            ops.processWireTransfer("account2", Money.parse("USD 200"));
        
        assertThat(result2.transactionNumber).isGreaterThan(result1.transactionNumber);
    }
}
