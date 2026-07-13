package com.divudi.ejb;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PatientRoomFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Creates the locked, package-derived billing rows (service, timed item,
 * professional-fee role, outside charge) as soon as a package-linked
 * admission is saved, and locks the room charge on the newly created
 * PatientRoom. Pharmacy-item components are intentionally NOT created here
 * — they are consumed progressively via pharmacy issue (see Task 16).
 *
 * Bill numbering for all four locked-bill categories uses a single,
 * simplified BillNumberGenerator call pattern (departmentBillNumberGenerator
 * / institutionBillNumberGenerator taking Department/Institution, BillType,
 * BillClassType, BillNumberSuffix) rather than replicating each category's
 * more elaborate, differing numbering strategy used elsewhere
 * (BillBhtController, InwardAdditionalChargeController,
 * InwardProfessionalBillController). This was a deliberate v1 simplification.
 */
@Stateless
public class InpatientPackageApplicationBean {

    @EJB
    private InpatientPackageItemFacade inpatientPackageItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private BillNumberGenerator billNumberBean;

    public void applyPackageToAdmission(Admission admission, PatientRoom patientRoom, WebUser loggedUser) {
        InpatientPackage inpatientPackage = admission.getInpatientPackage();
        if (inpatientPackage == null) {
            return;
        }

        lockRoomCharge(inpatientPackage, patientRoom);

        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);

        for (InpatientPackageItem component : components) {
            switch (component.getComponentType()) {
                case SERVICE:
                    createLockedServiceBillItem(admission, component, loggedUser);
                    break;
                case TIMED_ITEM:
                    createLockedTimedItem(admission, component, loggedUser);
                    break;
                case PROFESSIONAL_FEE_ROLE:
                    createLockedProfessionalFee(admission, component, loggedUser);
                    break;
                case OUTSIDE_CHARGE:
                    createLockedOutsideCharge(admission, component, loggedUser);
                    break;
                case PHARMACY_ITEM:
                    // Not pre-created — consumed progressively via pharmacy issue, see Task 16.
                    break;
                default:
                    break;
            }
        }
    }

    private void lockRoomCharge(InpatientPackage inpatientPackage, PatientRoom patientRoom) {
        if (patientRoom == null || patientRoom.getId() == null) {
            return;
        }
        patientRoom.setFromPackage(true);
        patientRoom.setIncludedRoomDurationHours(inpatientPackage.getIncludedRoomDurationHours());
        patientRoom.setCurrentRoomCharge(inpatientPackage.getFixedRoomCharge() != null ? inpatientPackage.getFixedRoomCharge() : 0.0);
        patientRoom.setCurrentMaintananceCharge(0.0);
        patientRoom.setCurrentNursingCharge(0.0);
        patientRoom.setCurrentMoCharge(0.0);
        patientRoom.setCurrentMoChargeForAfterDuration(0.0);
        patientRoom.setCurrentLinenCharge(0.0);
        patientRoom.setCurrentAdministrationCharge(0.0);
        patientRoom.setCurrentMedicalCareCharge(0.0);
        patientRoomFacade.edit(patientRoom);
    }

    private BilledBill createLockedBill(Admission admission, BillType billType, BillTypeAtomic billTypeAtomic, BillNumberSuffix suffix, Department toDepartment, WebUser loggedUser) {
        BilledBill bill = new BilledBill();
        bill.setBillType(billType);
        bill.setBillTypeAtomic(billTypeAtomic);
        bill.setDepartment(admission.getDepartment());
        bill.setInstitution(admission.getInstitution());
        bill.setPatient(admission.getPatient());
        bill.setPatientEncounter(admission);
        bill.setPaymentScheme(admission.getPaymentScheme());
        bill.setPaymentMethod(admission.getPaymentMethod());
        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        bill.setCreater(loggedUser);
        if (toDepartment != null) {
            bill.setToDepartment(toDepartment);
            bill.setToInstitution(toDepartment.getInstitution());
        }
        bill.setDeptId(billNumberBean.departmentBillNumberGenerator(bill.getDepartment(), billType, BillClassType.BilledBill, suffix));
        bill.setInsId(billNumberBean.institutionBillNumberGenerator(bill.getInstitution(), billType, BillClassType.BilledBill, suffix));
        billFacade.create(bill);
        return bill;
    }

    private void createLockedServiceBillItem(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createLockedBill(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }

    private void createLockedTimedItem(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createLockedBill(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);

        PatientItem patientItem = new PatientItem();
        patientItem.setPatient(admission.getPatient());
        patientItem.setPatientEncounter(admission);
        patientItem.setBill(bill);
        patientItem.setBillItem(billItem);
        patientItem.setItem(component.getItem());
        patientItem.setServiceValue(component.getFixedPrice());
        patientItem.setDiscount(0.0);
        patientItem.setCreatedAt(new Date());
        patientItem.setCreater(loggedUser);
        patientItemFacade.create(patientItem);
    }

    private void createLockedProfessionalFee(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createLockedBill(admission, BillType.InwardProfessional, BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setPatienEncounter(admission);
        billFee.setSpeciality(component.getSpeciality());
        billFee.setStaff(null); // Assigned later — see Task 15
        billFee.setFeeValue(component.getFixedPrice());
        billFee.setFeeGrossValue(component.getFixedPrice());
        billFee.setOverriddenRate(component.getFixedPrice());
        billFee.setFromPackage(true);
        billFee.setSourcePackageItem(component);
        billFee.setFeeAt(new Date());
        billFee.setCreatedAt(new Date());
        billFee.setCreater(loggedUser);
        billFeeFacade.create(billFee);
    }

    private void createLockedOutsideCharge(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createLockedBill(admission, BillType.InwardOutSideBill, BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setPatientEncounter(admission);
        billItem.setDescreption(component.getItem() != null ? component.getItem().getName() : component.getRoleLabel());
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }
}
