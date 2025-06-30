package com.divudi.ejb;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.AmppFacade;
import com.divudi.service.BillService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    @EJB
    BillService billService;

    @EJB
    private AmppFacade amppFacade;

    @EJB
    private AmpFacade ampFacade;

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

        if (allergyListOfPatient == null || allergyListOfPatient.isEmpty()) {
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

        if (allergyLisForPatient == null || allergyLisForPatient.isEmpty()) {
            allergyLisForPatient = getAllergyListForPatient(patient);
        }

        if (allergyLisForPatient.isEmpty()) {
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

            for (Item i : allergyItems) {
                allergyMsg.append(i.getName()).append(" , ");
            }

            if (allergyMsg.length() > 0) {
                allergyMsg.setLength(allergyMsg.length() - 2);
            }
            return allergyMsg.toString();
        }

        return "";
    }

    public void addBillItemInstructions(BillItem billItem) {
        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        Item item = billItem.getItem();
        if (item == null) {
            return;
        }
        String instrcutions = item.getInstructions();
        Amp amp = null;
        Vmp vmp = null;
        Vtm vtm = null;
        if (instrcutions == null || instrcutions.trim().isEmpty()) {

            if (item instanceof Ampp) {
                Ampp ampp = (Ampp) item;
                amp = ampp.getAmp();
            } else if (item instanceof Amp) {
                amp = (Amp) item;
            }
            if (amp != null) {
                vmp = amp.getVmp();
                if (vmp != null) {
                    instrcutions = vmp.getInstructions();
                }
            }
        }
        if (instrcutions == null || instrcutions.trim().isEmpty()) {
            if (vmp != null) {
                if (vmp.getVtm() instanceof Vtm) {
                    vtm = (Vtm) vmp.getVtm();
                }
                if (vtm != null) {
                    instrcutions = vtm.getInstructions();
                }
            }

        }
        billItem.setInstructions(instrcutions);
    }

    /**
     * Fetch the given AMP together with all associated AMPPs.
     *
     * @param amp the AMP to search for
     * @return List containing the AMP and all related AMPPs
     */
    public List<Item> findRelatedItems(Amp amp) {
        List<Item> items = new ArrayList<>();
        if (amp == null) {
            return items;
        }

        items.add(amp);

        String jpql = "select a from Ampp a where a.retired=:ret and a.amp=:amp";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("amp", amp);
        List<Ampp> ampps = amppFacade.findByJpql(jpql, params);
        if (ampps != null) {
            items.addAll(ampps);
        }

        return items;
    }

    /**
     * Fetch the AMP of the given AMPP and return it together with all AMPPs
     * related to that AMP, including the provided AMPP.
     *
     * @param ampp the AMPP to use as reference
     * @return List containing the AMP and all related AMPPs
     */
    public List<Item> findRelatedItems(Ampp ampp) {
        if (ampp == null) {
            return new ArrayList<>();
        }

        List<Item> items = findRelatedItems(ampp.getAmp());

        if (!items.contains(ampp)) {
            items.add(ampp);
        }

        return items;
    }

    /**
     * Fetch related items for a given Item (Amp or Ampp). Delegates to the
     * appropriate method based on instance type.
     *
     * @param item the item to find related items for
     * @return List of related items or empty list if type is not supported or
     * item is null
     */
    public List<Item> findRelatedItems(Item item) {
        if (item == null) {
            return new ArrayList<>();
        }
        if (item instanceof Amp) {
            return findRelatedItems((Amp) item);
        }
        if (item instanceof Ampp) {
            return findRelatedItems((Ampp) item);
        }
        return new ArrayList<>();
    }

    public PharmacyBundle fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionType(Date fromDate, Date toDate, Institution institution, Institution site, Department department, WebUser webUser, AdmissionType admissionType, PaymentScheme paymentScheme) {
        PharmacyBundle bundle;
        System.out.println("processPharmacyIncomeReportByBillTypeAndDiscountTypeAndAdmissionType");

        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

        List<Bill> pharmacyIncomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new PharmacyBundle(pharmacyIncomeBills);

        for (PharmacyRow r : bundle.getRows()) {
            Bill b = r.getBill();
            if (b == null || b.getPaymentMethod() == null) {
                continue;
            }
            if (b.getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                r.setPayments(billService.fetchBillPayments(b));
            }
        }

        bundle.generatePaymentDetailsGroupedByBillTypeAndDiscountSchemeAndAdmissionType();

        return bundle;

    }

    public PharmacyBundle fetchPharmacyStockPurchaseValueByBillType(Date fromDate, Date toDate, Institution institution, Institution site, Department department, WebUser webUser, AdmissionType admissionType, PaymentScheme paymentScheme) {
        PharmacyBundle bundle;
        List<BillTypeAtomic> billTypeAtomics = getPharmacyPurchaseBillTypes();
        List<Bill> pharmacyIncomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new PharmacyBundle(pharmacyIncomeBills);
        bundle.generatePharmacyPurchaseGroupedByBillType();
        return bundle;
    }

    public PharmacyBundle fetchPharmacyTransferValueByBillType(Date fromDate, Date toDate, Institution institution, Institution site, Department department, WebUser webUser, AdmissionType admissionType, PaymentScheme paymentScheme) {
        PharmacyBundle bundle;
        List<BillTypeAtomic> billTypeAtomics = getPharmacyInternalTransferBillTypes();
        List<Bill> pharmacyIncomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new PharmacyBundle(pharmacyIncomeBills);
        bundle.generatePharmacyPurchaseGroupedByBillType();
        return bundle;
    }

    public PharmacyBundle fetchPharmacyAdjustmentValueByBillType(Date fromDate, Date toDate, Institution institution, Institution site, Department department, WebUser webUser, AdmissionType admissionType, PaymentScheme paymentScheme) {
        PharmacyBundle bundle;
        List<BillTypeAtomic> billTypeAtomics = getPharmacyAdjustmentBillTypes();
        List<Bill> pharmacyIncomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new PharmacyBundle(pharmacyIncomeBills);
        bundle.generatePharmacyPurchaseGroupedByBillType();
        return bundle;
    }

    public List<BillTypeAtomic> getPharmacyIncomeBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
                BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED,
                BillTypeAtomic.PHARMACY_WHOLESALE_PRE,
                BillTypeAtomic.PHARMACY_WHOLESALE_REFUND,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE
        );
    }

    public List<BillTypeAtomic> getPharmacyPurchaseBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED,
                BillTypeAtomic.PHARMACY_GRN,
                BillTypeAtomic.PHARMACY_GRN_RETURN,
                BillTypeAtomic.PHARMACY_GRN_CANCELLED
        );
    }

    public List<BillTypeAtomic> getPharmacyInternalTransferBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_ISSUE,
                BillTypeAtomic.PHARMACY_RECEIVE
        );
    }

    public List<BillTypeAtomic> getPharmacyAdjustmentBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_ADJUSTMENT,
                BillTypeAtomic.PHARMACY_ADJUSTMENT_CANCELLED
        );
    }

}
