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
 * Creates ordinary, fully-editable billing rows (service, timed item,
 * professional-fee role, outside charge) from a package's components as
 * soon as a package-linked admission is saved. These rows carry no special
 * lock — they are ordered normally and can be edited/removed like any other
 * bill row; sourcePackageItem is kept purely for traceability. The room
 * charges normally too (no package-specific handling at all). Pharmacy-item
 * components are intentionally NOT created here — they are consumed
 * progressively via normal pharmacy issue, at normal rates.
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
    @EJB
    private com.divudi.service.inward.InwardProfessionalFeeClassificationService professionalFeeClassificationService;

    public void applyPackageToAdmission(Admission admission, PatientRoom patientRoom, WebUser loggedUser) {
        InpatientPackage inpatientPackage = admission.getInpatientPackage();
        if (inpatientPackage == null) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);

        for (InpatientPackageItem component : components) {
            switch (component.getComponentType()) {
                case SERVICE:
                    createServiceBillItemFromComponent(admission, component, loggedUser);
                    break;
                case TIMED_ITEM:
                    createTimedItemFromComponent(admission, component, loggedUser);
                    break;
                case PROFESSIONAL_FEE_ROLE:
                    createProfessionalFeeFromComponent(admission, component, loggedUser);
                    break;
                case OUTSIDE_CHARGE:
                    createOutsideChargeFromComponent(admission, component, loggedUser);
                    break;
                case PHARMACY_ITEM:
                    // Not pre-created — consumed progressively via pharmacy issue.
                    break;
                default:
                    break;
            }
        }
    }

    private BilledBill createBillForComponent(Admission admission, BillType billType, BillTypeAtomic billTypeAtomic, BillNumberSuffix suffix, Department toDepartment, WebUser loggedUser) {
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

    private void createServiceBillItemFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createBillForComponent(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }

    private void createTimedItemFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createBillForComponent(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
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

    private void createProfessionalFeeFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createBillForComponent(admission, BillType.InwardProfessional, BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setPatienEncounter(admission);
        billFee.setSpeciality(component.getSpeciality());
        billFee.setStaff(null); // Assigned later via InwardProfessionalBillController.assignStaffToPackageFee
        billFee.setFeeValue(component.getFixedPrice());
        billFee.setFeeGrossValue(component.getFixedPrice());
        billFee.setOverriddenRate(component.getFixedPrice());
        billFee.setProfessionalFeeCategory(professionalFeeClassificationService.defaultCategoryFor(null, component.getSpeciality()));
        billFee.setSourcePackageItem(component);
        billFee.setFeeAt(new Date());
        billFee.setCreatedAt(new Date());
        billFee.setCreater(loggedUser);
        billFeeFacade.create(billFee);
    }

    private void createOutsideChargeFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createBillForComponent(admission, BillType.InwardOutSideBill, BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setDescreption(component.getItem() != null ? component.getItem().getName() : component.getRoleLabel());
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }
}
