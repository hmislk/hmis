/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Fee;
import com.divudi.entity.PatientEncounter;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BilledBillFacade;
import com.divudi.facade.FeeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardAdditionalChargeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillNumberGenerator billNumberBean;
    private InwardChargeType inwardChargeType;
    //////////////
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    InwardBeanController inwardBean;
    //////////////
    @Inject
    private SessionController sessionController;
    //////////////
    private BilledBill current;
    private List<BillItem> billItemList;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

      public String navigateToAddOutsideChargeFromMenu() {
        makeNull();
        return "/inward/inward_bill_outside_charge?faces-redirect=true";
    }
    
    public String navigateToAddOutsideChargeFromInpatientProfile() {
        return "/inward/inward_bill_outside_charge?faces-redirect=true";
    }
    
    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Where Item From");
            return true;
        }

        if (getInwardChargeType() == null) {
            return true;
        }

        if (getCurrent().getTotal() < 1) {
            JsfUtil.addErrorMessage("Enter Added Charge Correctly");
            return true;
        }

        if (getCurrent().getComments().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Discription");
            return true;
        }

        return false;

    }

    public void addCharge() {
        if (errorCheck()) {
            return;
        }
        saveBill();
        BillItem b = saveBillItem();
        billItemList.add(b);
        getCurrent().setSingleBillItem(b);
        getBilledBillFacade().edit(current);

        JsfUtil.addSuccessMessage("Additional Charges Added");
        
    }

    public void makeNull() {
        current = null;
        billItemList = null;
        inwardChargeType = null;
    }
    
    public void reset(){
        PatientEncounter p = current.getPatientEncounter();
        current = null;
        billItemList =null;
        getCurrent().setPatientEncounter(p);
        inwardChargeType = null;
        JsfUtil.addSuccessMessage("Cleared Successfully");
    }
    
    public void makeChargesNull() {
        inwardChargeType=null;
        current.setFromInstitution(null);
        current.setTotal(null);
        current.setComments(null);
    }

    private void saveBill() {
        getCurrent().setBillType(BillType.InwardOutSideBill);
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setNetTotal(getCurrent().getTotal());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getCurrent().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getCurrent().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWSER));

        if (getCurrent().getId() == null) {
            getBilledBillFacade().create(getCurrent());
        }
        else{
            getBilledBillFacade().edit(getCurrent());
        }
    }

    private BillItem saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setInwardChargeType(inwardChargeType);
        temBi.setGrossValue(getCurrent().getTotal());
        temBi.setNetValue(getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());
       
        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }
        saveBillFee(temBi);

        return temBi;

    }

    private void saveBillFee(BillItem bt) {
        BillFee bf = new BillFee();
        Fee additional = getInwardBean().createAdditionalFee();

        bf.setPatienEncounter(getCurrent().getPatientEncounter());
        bf.setBill(getCurrent());
        bf.setFee(additional);
        bf.setBillItem(bt);
        bf.setCreatedAt(new Date());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setFeeGrossValue(getCurrent().getTotal());
        bf.setFeeValue(getCurrent().getTotal());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BilledBill getCurrent() {
        if (current == null) {
            current = new BilledBill();
            current.setBillType(BillType.InwardBill);
        }

        return current;
    }

    public void setCurrent(BilledBill current) {
        this.current = current;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public List<BillItem> getBillItemList() {
        if(billItemList==null){
            billItemList = new ArrayList<>();
        }
        return billItemList;
    }

    public void setBillItemList(List<BillItem> billItemList) {
        this.billItemList = billItemList;
    }
}
