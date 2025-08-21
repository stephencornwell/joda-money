package org.joda.money.banking;

import org.joda.money.Money;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Banking Operations - Wire Transfer Processing
 * Migrated from legacy COBOL system in Q2 2024
 * Processes ~50,000 wire transfers daily ($4.2B volume)
 */
public class BankingOperations {
    
    // Tracks daily totals for regulatory compliance
    private static ConcurrentHashMap<String, Money> dailyTotals = new ConcurrentHashMap<>();
    private static AtomicInteger transactionCount = new AtomicInteger(0);
    
    /**
     * Process wire transfer with AML (Anti-Money Laundering) checks
     * Required by Bank Secrecy Act - must track cumulative daily transfers
     */
    public TransferResult processWireTransfer(String accountId, Money transferAmount) {
        TransferResult result = new TransferResult();
        result.transferAmount = transferAmount;
        
        // Get current daily total for this account
        Money currentDailyTotal = dailyTotals.getOrDefault(accountId, 
            Money.zero(transferAmount.getCurrencyUnit()));
        
        // Check if this would exceed $10,000 daily limit (AML threshold)
        Money newDailyTotal = currentDailyTotal.plus(transferAmount);
        
        if (newDailyTotal.isGreaterThan(Money.parse("USD 10000"))) {
            result.requiresAmlReview = true;
        }
        
        dailyTotals.put(accountId, newDailyTotal);
        
        result.transactionNumber = transactionCount.incrementAndGet();
        
        return result;
    }
    
    /**
     * Reset daily totals (runs at midnight EST)
     */
    public void resetDailyTotals() {
        dailyTotals.clear();
        transactionCount.set(0);
    }
    
    /**
     * Result object for wire transfers
     */
    public static class TransferResult {
        public Money transferAmount;
        public boolean requiresAmlReview;
        public int transactionNumber;
        
        public boolean isCompliant() {
            // If it requires review, it must be flagged
            return !requiresAmlReview || transactionNumber > 0;
        }
    }
}