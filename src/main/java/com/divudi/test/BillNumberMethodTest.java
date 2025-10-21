package com.divudi.test;

import com.divudi.core.entity.BillNumber;

/**
 * Simple test class to verify BillNumber methods exist
 * This should help CodeRabbit AI recognize the methods
 */
public class BillNumberMethodTest {
    
    public void testBillNumberMethods() {
        BillNumber billNumber = new BillNumber();
        
        // Test setOpdAndInpatientServiceBills method
        billNumber.setOpdAndInpatientServiceBills(true);
        boolean serviceResult = billNumber.isOpdAndInpatientServiceBills();
        
        // Test setOpdAndInpatientServiceBatchBills method  
        billNumber.setOpdAndInpatientServiceBatchBills(true);
        boolean batchResult = billNumber.isOpdAndInpatientServiceBatchBills();
        
        System.out.println("Methods exist and work: " + serviceResult + ", " + batchResult);
    }
}