package com.divudi.bean.common;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class CollectingCentrePaymentController implements Serializable {

// <editor-fold defaultstate="collapsed" desc="Ejbs">
    @EJB
    BillFacade billFacade;
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Controllers">
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Variables">
    
    private boolean ccPaymentSettlingStarted = false;
    private Date fromDate;
    private Date toDate;

    private Institution currentCollectingCentre;
    
    private List<BillLight> pandingCCpaymentBills;
    private List<BillLight> selectedCCpaymentBills;
    
    private double totalCCAmount;
    private double totalHospitalAmount;
    
    
    
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Navigation Method">
    
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Functions">
    public CollectingCentrePaymentController() {
    }
    
    public void processCollectingCentrePayment(){
        if(currentCollectingCentre == null){
            JsfUtil.addErrorMessage("Select Collecting Centre");
            return ;
        }
        
        findPendingCCBills();
    }
    
    public void makeNull(){
        totalHospitalAmount = 0.0;
        totalCCAmount = 0.0;
        fromDate =null;
        toDate =null;
        selectedCCpaymentBills =null;
        pandingCCpaymentBills =null;
        ccPaymentSettlingStarted = false;
        currentCollectingCentre =null;
    }
    
    public void findPendingCCBills(){
        System.out.println("findPendingCCBills");
        System.out.println("currentCollectingCentre = " + currentCollectingCentre);
        System.out.println("fromDate = " + fromDate);
        System.out.println("toDate = " + toDate);
        
        pandingCCpaymentBills = new ArrayList<>();
        String jpql;
        Map temMap = new HashMap();
        
        jpql = "select new com.divudi.core.light.common.BillLight(bill.id, bill.deptId, bill.createdAt, bill.patient.person.title, bill.patient.person.name,  bill.totalCenterFee, bill.totalHospitalFee ) "
                + " from Bill bill "
                + " where bill.collectingCentre=:cc "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired=false ";
        
        jpql += " order by bill.createdAt asc ";
        temMap.put("cc", currentCollectingCentre);
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        
        pandingCCpaymentBills =  billFacade.findLightsByJpql(jpql, temMap, TemporalType.TIMESTAMP);
    
        System.out.println("pandingCCpaymentBills = " + pandingCCpaymentBills.size());
        
        totalHospitalAmount = 0.0;
        totalCCAmount = 0.0;
        
        for(BillLight bl : pandingCCpaymentBills){
            totalHospitalAmount += bl.getHospitalTotal();
            totalCCAmount+= bl.getCcTotal();
        }
        
        System.out.println("totalHospitalAmount = " + totalHospitalAmount);
        System.out.println("totalCCAmount = " + totalCCAmount);
        
    }
    
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfMonth();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public boolean isCcPaymentSettlingStarted() {
        return ccPaymentSettlingStarted;
    }

    public void setCcPaymentSettlingStarted(boolean ccPaymentSettlingStarted) {
        this.ccPaymentSettlingStarted = ccPaymentSettlingStarted;
    }

    public Institution getCurrentCollectingCentre() {
        return currentCollectingCentre;
    }

    public void setCurrentCollectingCentre(Institution currentCollectingCentre) {
        this.currentCollectingCentre = currentCollectingCentre;
    }

// </editor-fold>

    public List<BillLight> getPandingCCpaymentBills() {
        return pandingCCpaymentBills;
    }

    public void setPandingCCpaymentBills(List<BillLight> pandingCCpaymentBills) {
        this.pandingCCpaymentBills = pandingCCpaymentBills;
    }

    public List<BillLight> getSelectedCCpaymentBills() {
        return selectedCCpaymentBills;
    }

    public void setSelectedCCpaymentBills(List<BillLight> selectedCCpaymentBills) {
        this.selectedCCpaymentBills = selectedCCpaymentBills;
    }

    public double getTotalCCAmount() {
        return totalCCAmount;
    }

    public void setTotalCCAmount(double totalCCAmount) {
        this.totalCCAmount = totalCCAmount;
    }

    public double getTotalHospitalAmount() {
        return totalHospitalAmount;
    }

    public void setTotalHospitalAmount(double totalHospitalAmount) {
        this.totalHospitalAmount = totalHospitalAmount;
    }

  
}
