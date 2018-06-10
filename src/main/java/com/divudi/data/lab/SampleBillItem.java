/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import com.divudi.entity.BillItem;
import java.util.List;

/**
 *
 * @author buddhika_ari
 */
public class SampleBillItem {
    private BillItem billItem;
        private List<SampleBillComponant> sampleBillComponants;

        public BillItem getBillItem() {
            return billItem;
        }

        public void setBillItem(BillItem billItem) {
            this.billItem = billItem;
        }

        public List<SampleBillComponant> getSampleBillComponants() {
            return sampleBillComponants;
        }

        public void setSampleBillComponants(List<SampleBillComponant> sampleBillComponants) {
            this.sampleBillComponants = sampleBillComponants;
        }
}
