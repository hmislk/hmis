/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import com.divudi.entity.Item;
import com.divudi.entity.lab.PatientSample;

/**
 *
 * @author buddhika_ari
 */
public class SampleTest {
    private PatientSample patientSample;
        private Item test;
        private Item investigationComponent;
        
        

        public PatientSample getPatientSample() {
            return patientSample;
        }

        public void setPatientSample(PatientSample patientSample) {
            this.patientSample = patientSample;
        }

        public Item getTest() {
            return test;
        }

        public void setTest(Item test) {
            this.test = test;
        }

        public Item getInvestigationComponent() {
            return investigationComponent;
        }

        public void setInvestigationComponent(Item investigationComponent) {
            this.investigationComponent = investigationComponent;
        }
}
