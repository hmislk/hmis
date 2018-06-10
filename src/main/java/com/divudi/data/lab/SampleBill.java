/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import com.divudi.entity.Bill;
import java.util.List;

/**
 *
 * @author buddhika_ari
 */
public class SampleBill {
    private Bill bill;
        private List<SampleBillItem> sampleBillItems;

        public Bill getBill() {
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

        public List<SampleBillItem> getSampleBillItems() {
            return sampleBillItems;
        }

        public void setSampleBillItems(List<SampleBillItem> sampleBillItems) {
            this.sampleBillItems = sampleBillItems;
        }
}
