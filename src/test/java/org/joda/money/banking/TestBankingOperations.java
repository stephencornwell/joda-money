package org.joda.money.banking;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.joda.money.Money;

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
    
    // Todo: Add concurrent, AML threshold, and reset timing tests
}