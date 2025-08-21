package org.joda.money.banking;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.money.Money;
import org.junit.jupiter.api.Test;

public class TestBankingOperations {
    
    @Test
    public void testBasicTransfer() {
        // Naive test - doesn't check concurrency!
        BankingOperations ops = new BankingOperations();
        BankingOperations.TransferResult result = 
            ops.processWireTransfer("account123", Money.parse("USD 100"));
        
        assertThat(result).isNotNull();
        assertThat(result.requiresAmlReview).isFalse();
    }
    
    // TODO: Need to test concurrent transfers and AML threshold
    // TODO: Need to test the midnight reset and AML tracking
}