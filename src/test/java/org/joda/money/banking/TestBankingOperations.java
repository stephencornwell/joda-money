package org.joda.money.banking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBankingOperations {
    
    private BankingOperations ops;
    
    @BeforeEach
    public void setUp() {
        ops = new BankingOperations();
        ops.resetDailyTotals();
    }
    
    @Test
    public void test_processWireTransfer_basic() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account123", Money.parse("USD 100"));
        
        assertThat(result).isNotNull();
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 100"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_amlThreshold_underLimit() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account456", Money.parse("USD 9999.99"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_amlThreshold_exactLimit() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account789", Money.parse("USD 10000.00"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_amlThreshold_overLimit() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account101", Money.parse("USD 10000.01"));
        
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_amlThreshold_cumulativeOverLimit() {
        ops.processWireTransfer("account202", Money.parse("USD 5000"));
        ops.processWireTransfer("account202", Money.parse("USD 4000"));
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account202", Money.parse("USD 1000.01"));
        
        assertThat(result.requiresAmlReview).isTrue();
        assertThat(result.transactionNumber).isEqualTo(3);
    }
    
    @Test
    public void test_processWireTransfer_amlThreshold_cumulativeUnderLimit() {
        ops.processWireTransfer("account303", Money.parse("USD 5000"));
        ops.processWireTransfer("account303", Money.parse("USD 4000"));
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account303", Money.parse("USD 1000"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(3);
    }
    
    @Test
    public void test_processWireTransfer_multipleAccounts() {
        BankingOperations.TransferResult result1 = 
            ops.processWireTransfer("account404", Money.parse("USD 8000"));
        BankingOperations.TransferResult result2 = 
            ops.processWireTransfer("account505", Money.parse("USD 8000"));
        
        assertThat(result1.requiresAmlReview).isFalse();
        assertThat(result2.requiresAmlReview).isFalse();
        assertThat(result1.transactionNumber).isEqualTo(1);
        assertThat(result2.transactionNumber).isEqualTo(2);
    }
    
    @Test
    public void test_processWireTransfer_usdCurrency() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account606", Money.parse("USD 9000"));
        
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 9000"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_zeroAmount() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account707", Money.parse("USD 0"));
        
        assertThat(result.transferAmount).isEqualTo(Money.parse("USD 0"));
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_resetDailyTotals() {
        ops.processWireTransfer("account808", Money.parse("USD 5000"));
        ops.resetDailyTotals();
        
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account808", Money.parse("USD 8000"));
        
        assertThat(result.requiresAmlReview).isFalse();
        assertThat(result.transactionNumber).isEqualTo(1);
    }
    
    @Test
    public void test_transferResult_isCompliant_noReview() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account909", Money.parse("USD 1000"));
        
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void test_transferResult_isCompliant_withReview() {
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account010", Money.parse("USD 15000"));
        
        assertThat(result.isCompliant()).isTrue();
    }
    
    @Test
    public void test_processWireTransfer_concurrent() throws InterruptedException {
        final String accountId = "concurrentAccount";
        final int threadCount = 10;
        final Money transferAmount = Money.parse("USD 1500");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger amlReviewCount = new AtomicInteger(0);
        final AtomicInteger totalTransfers = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    BankingOperations.TransferResult result = 
                        ops.processWireTransfer(accountId, transferAmount);
                    totalTransfers.incrementAndGet();
                    if (result.requiresAmlReview) {
                        amlReviewCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(totalTransfers.get()).isEqualTo(threadCount);
        assertThat(amlReviewCount.get()).as("Expected some AML reviews for concurrent transfers totaling $15,000").isGreaterThanOrEqualTo(1);
    }
    
    @Test
    public void test_processWireTransfer_concurrentDifferentAccounts() throws InterruptedException {
        final int threadCount = 5;
        final Money transferAmount = Money.parse("USD 8000");
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger amlReviewCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final String accountId = "account" + i;
            executor.submit(() -> {
                try {
                    BankingOperations.TransferResult result = 
                        ops.processWireTransfer(accountId, transferAmount);
                    if (result.requiresAmlReview) {
                        amlReviewCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(amlReviewCount.get()).isEqualTo(0);
    }
    
    @Test
    public void test_processWireTransfer_transactionNumberIncrement() {
        BankingOperations.TransferResult result1 = 
            ops.processWireTransfer("account111", Money.parse("USD 100"));
        BankingOperations.TransferResult result2 = 
            ops.processWireTransfer("account222", Money.parse("USD 200"));
        BankingOperations.TransferResult result3 = 
            ops.processWireTransfer("account333", Money.parse("USD 300"));
        
        assertThat(result1.transactionNumber).isEqualTo(1);
        assertThat(result2.transactionNumber).isEqualTo(2);
        assertThat(result3.transactionNumber).isEqualTo(3);
    }
}
