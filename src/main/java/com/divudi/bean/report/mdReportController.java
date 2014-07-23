/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ServiceFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@RequestScoped
public class mdReportController implements Serializable {

    private Date fromDate;
    private Date toDate;
    List<Bill> bills;
    private List<Bill> fillterBill;
    private List<ItemWithFee> itemWithFees;
    private List<ItemWithFee> fillterItemWithFees;
    private PaymentMethod paymentMethod;
    ////////////////////////////////////
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    ///////////////////////////////
    @Inject
    private SessionController sessionController;

    public PaymentMethod[] getPaymentMethods() {

        return PaymentMethod.values();
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        bills = null;
        fillterBill = null;
        itemWithFees = null;
        fillterItemWithFees = null;
        paymentMethod = null;
    }

    public double getHospitalTotal() {
        double tmp = 0.0;
        List<Bill> list;
        if (fillterBill == null) {
            list = bills;
        } else {
            list = fillterBill;
        }

        if (list != null) {
            for (Bill b : list) {
                tmp += b.getHospitalFee();
            }
        }

        //System.err.println("Hos : " + tmp);

        return tmp;
    }

    public double getItemHospitalTotal() {
        double tmp = 0.0;
        List<ItemWithFee> list;
        if (fillterItemWithFees == null) {
            list = itemWithFees;
        } else {
            list = fillterItemWithFees;
        }

        if (list != null) {
            for (ItemWithFee b : list) {
                tmp += b.getHospitalFee();

            }
        }
        return tmp;
    }

    public double getItemProfessionalTotal() {
        double tmp = 0.0;
        List<ItemWithFee> list;
        if (fillterItemWithFees == null) {
            list = itemWithFees;
        } else {
            list = fillterItemWithFees;
        }

        if (list != null) {
            for (ItemWithFee b : list) {
                tmp += b.getProFee();
            }
        }
        return tmp;
    }

    public double getProfessionalTotal() {
        double tmp = 0.0;
        List<Bill> list;
        if (fillterBill == null) {
            list = bills;
        } else {
            list = fillterBill;
        }

        if (list != null) {
            for (Bill b : list) {
                tmp += b.getProfessionalFee();
            }
        }
        //System.err.println("Pro : " + tmp);
        return tmp;
    }

    private List<Bill> bills() {
        List<Bill> tmp;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where b.billType = :billType and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate and b.retired=false";

        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());

        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<>();

        }

        return tmp;

    }

    public List<Bill> getBills() {

        if (bills == null) {
            bills = bills();
            for (Bill b : bills) {
                String sql = "Select b From BillFee b where b.retired=false and b.bill=:bb";
                HashMap hm = new HashMap();
                hm.put("bb", b);
                List<BillFee> bflist = getBillFeeFacade().findBySQL(sql, hm);
                for (BillFee bf : bflist) {
                    if (bf.getFee() != null && (bf.getFee().getFeeType() == FeeType.OwnInstitution)) {
                        b.setHospitalFee(b.getHospitalFee() + bf.getFeeValue());
                    } else if (bf.getFee() != null && (bf.getFee().getFeeType() == FeeType.Staff)) {
                        b.setProfessionalFee(b.getProfessionalFee() + bf.getFeeValue());
                    }
                }
            }
        }

        return bills;
    }

    public mdReportController() {
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void makeBillNull() {
        bills = null;
        fillterBill = null;
        fillterItemWithFees = null;
        itemWithFees = null;
    }

    public void setToDate(Date toDate) {

        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(ServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public List<Bill> getFillterBill() {
        //  getHospitalTotal();
        // getProfessionalTotal();
        return fillterBill;
    }

    public void setFillterBill(List<Bill> fillterBill) {
        this.fillterBill = fillterBill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
    
    
    private List<Item> item() {
        String sql;
        List<Item> tmp;
        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());

        if (getPaymentMethod() == null) {
            sql = "select distinct(bi.item) FROM BillItem bi where bi.retired=false "
                    + "  and "
                    + " bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate ";

        } else {
            sql = "select distinct(bi.item) FROM BillItem bi where  "
                    + " bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";

            temMap.put("p", getPaymentMethod());

        }

        tmp = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        if (tmp == null) {
            tmp = new ArrayList<>();
        }

        return tmp;

    }

    public List<ItemWithFee> getItemWithFees() {
       
        if (itemWithFees == null) {
            itemWithFees = new ArrayList<>();
            for (Item i : item()) {
                ItemWithFee iwf = new ItemWithFee();
                iwf.setItem(i);
                setCount(iwf);
                setFee(iwf);
                itemWithFees.add(iwf);
            }

        }

        return itemWithFees;
    }

    private List<BillItem> billItemForCount(Bill bill, Item i) {
        if (i == null) {
            return new ArrayList<BillItem>();
        }

        Map temMap = new HashMap();
        String sql;

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i);

        if (getPaymentMethod() == null) {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and type(bi.bill)=:billClass and bi.bill.billType=:btp and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

        } else {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and bi.bill.paymentMethod = :pm and bi.bill.billType=:btp and type(bi.bill)=:billClass and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

            temMap.put("pm", getPaymentMethod());

        }

        return getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private void setCount(ItemWithFee i) {
        double billed, cancelled, refunded;
        billed = cancelled = refunded = 0.0;

        List<BillItem> temps = billItemForCount(new BilledBill(), i.getItem());

        for (BillItem b : temps) {
            billed++;
        }

        temps = billItemForCount(new CancelledBill(), i.getItem());

        for (BillItem b : temps) {
            cancelled++;
        }

        temps = billItemForCount(new RefundBill(), i.getItem());

        for (BillItem b : temps) {
            refunded++;
        }

        i.setCount(billed - cancelled - refunded);

    }

    private void setFee(ItemWithFee i) {
        if (i.getItem() == null) {
            return;
        }

        double hospiatalFee = 0.0;
        double staffFee = 0.0;
        String sql;
        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i.getItem());

        if (getPaymentMethod() == null) {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.createdAt between :fromDate and :toDate  and b.item=:item)";

        } else {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.createdAt between :fromDate and :toDate  and b.item=:item"
                    + " and  b.bill.paymentMethod = :pm)";

            temMap.put("pm", getPaymentMethod());
        }

        List<BillFee> billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

//        for (BillFee b : billFees) {
//            if (b.getStaff() != null) {
//                staffFee += b.getFeeValue();
//            } else {
//                hospiatalFee += b.getFeeValue();
//            }
//        }
        for (BillFee b : billFees) {
            if (b.getFee().getFeeType() == FeeType.Staff) {
                staffFee += b.getFeeValue();
            } else if (b.getFee().getFeeType() == FeeType.OwnInstitution) {
                hospiatalFee += b.getFeeValue();
            }
        }

        i.setHospitalFee(hospiatalFee);
        i.setProFee(staffFee);
        i.setTotal(hospiatalFee + staffFee);

    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
//        itemWithFees = null;
//        fillterItemWithFees = null;
        this.paymentMethod = paymentMethod;
    }

    public List<ItemWithFee> getFillterItemWithFees() {
        return fillterItemWithFees;
    }

    public void setFillterItemWithFees(List<ItemWithFee> fillterItemWithFees) {
        this.fillterItemWithFees = fillterItemWithFees;
    }
}
