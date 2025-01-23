package com.divudi.ejb;

import com.divudi.data.clinical.ClinicalFindingValueType;
import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.facade.ClinicalFindingValueFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Chinthaka Prasad
 */
@Stateless
public class PharmacyService {

    @EJB
    private ClinicalFindingValueFacade clinicalFindingValueFacade;

    public List<ClinicalFindingValue> getAllergyListForPatient(Patient patient) {

        List<ClinicalFindingValue> allergyListOfPatient = new ArrayList<>();

        if (patient == null) {
            return allergyListOfPatient;
        }
        String jpql = "Select clinicalValue from ClinicalFindingValue clinicalValue "
                + " where clinicalValue.patient = :patient "
                + " and clinicalValue.clinicalFindingValueType = :type "
                + " and clinicalValue.retired = :retireStatus";

        Map paramsForAllergySearch = new HashMap();
        paramsForAllergySearch.put("patient", patient);
        paramsForAllergySearch.put("type", ClinicalFindingValueType.PatientAllergy);
        paramsForAllergySearch.put("retireStatus", false);

        allergyListOfPatient = clinicalFindingValueFacade.findByJpql(jpql, paramsForAllergySearch);
        return allergyListOfPatient;

    }

    public boolean isAllergyForPatient(Patient patient, BillItem billItem, List<ClinicalFindingValue> allergyListOfPatient) {

        if(allergyListOfPatient == null || allergyListOfPatient.isEmpty()){
             allergyListOfPatient = getAllergyListForPatient(patient);
        }
        
        if (allergyListOfPatient.isEmpty()) {
            return false;
        }

        for (ClinicalFindingValue c : allergyListOfPatient) {
            if (c.getItemValue() != null) {
                if (billItem.getPharmaceuticalBillItem().getItemBatch() != null) {
                    if (c.getItemValue().equals(billItem.getPharmaceuticalBillItem().getItemBatch().getItem())) {
                        return true;
                    }
                }
                if (billItem.getPharmaceuticalBillItem().getItemBatch().getItem() != null) {
                    if (c.getItemValue().equals(billItem.getPharmaceuticalBillItem().getItemBatch().getItem().getVmp())) {
                        return true;
                    }
                }

                if (billItem.getPharmaceuticalBillItem().getItemBatch().getItem().getVmp() != null) {
                    if (c.getItemValue().equals(billItem.getPharmaceuticalBillItem().getItemBatch().getItem().getVmp().getVtm())) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public String isAllergyForPatient(Patient patient, List<BillItem> items, List<ClinicalFindingValue> allergyLisForPatient) {
        
        boolean hasAllergicMedicines = false;
        List<Item> allergyItems = new ArrayList<>();
        StringBuilder allergyMsg = new StringBuilder();
        
        if(allergyLisForPatient == null || allergyLisForPatient.isEmpty()){
            allergyLisForPatient = getAllergyListForPatient(patient);
        }
        
        if(allergyLisForPatient.isEmpty()){
            return "";
        }

        for (BillItem billItem : items) {
            boolean thisItemIsAllergy = isAllergyForPatient(patient, billItem, allergyLisForPatient);

            if (thisItemIsAllergy) {
                allergyItems.add(billItem.getItem());
                hasAllergicMedicines = true;
            }
        }

        if (hasAllergicMedicines) {
            allergyMsg.append("This patient should be allergy of ");
            
            for(Item i : allergyItems){
                allergyMsg.append(i.getName() +" , ");
            }
            
            if(allergyMsg.length() >0){
                allergyMsg.setLength(allergyMsg.length()-2);
            }
            return allergyMsg.toString();
        }

        return "";
    }

}
