/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.DepartmentPayment;
import com.divudi.data.dataStructure.PharmacySummery;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String1Value3;
import com.divudi.data.table.String3Value2;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Speciality;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.ItemFacade;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BookKeepingSummery implements Serializable {

    Date fromDate;
    Date toDate;
    private PaymentMethod paymentMethod;
    Institution institution;
    Institution loggediInstitution;
    private Institution incomeInstitution;
    @EJB
    CommonFunctions commonFunctions;
    @Inject
    BillBeanController billBean;
    @EJB
    BillFacade billFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    //List
    private List<String1Value3> opdList;
    List<String1Value2> outSideFees;
    private List<String1Value2> pharmacySales;
    private List<String1Value2> channelBills;
    List<String1Value2> collections2Hos;
    List<String1Value2> finalValues;
    List<String3Value2> inwardCollections;
    private List<Bill> agentCollections;
    List<Bill> creditCardBill;
    List<Bill> slipBill;
    List<Bill> chequeBill;
    List<BillItem> creditCompanyCollections;
    List<BillItem> creditCompanyCollectionsInward;
    List<DepartmentPayment> departmentProfessionalPayments;
    List<DepartmentPayment> channellingProfessionalPayments;
    List<String1Value2> inwardProfessionalPayments;
    bookKeepingSummeryRow bksr = new bookKeepingSummeryRow();
    //Value
    double opdHospitalTotal;
    double opdStaffTotal;
    double opdRegentTotal;
    double opdRegentTotalWithCredit;
    double opdRegentTotalByPayMethod;
    double outSideFeeTotal;
    double pharmacyTotal;
    double inwardPaymentTotal;
    double agentPaymentTotal;
    double creditCompanyTotal;
    double creditCompanyTotalInward;
    double pettyCashTotal;
    double channellingProfessionalPaymentTotal;
    double departmentProfessionalPaymentTotal;
    double inwardProfessionalPaymentTotal;
    double creditCardTotal;
    double chequeTotal;
    double slipTotal;
    double grantTotal;
    double channelTotal;
    long countTotals;
    @Inject
    SessionController sessionController;

    public void makeNull() {
        //List
        opdList = null;
        outSideFees = null;
        pharmacySales = null;
        channelBills = null;
        collections2Hos = null;
        finalValues = null;
        inwardCollections = null;
        agentCollections = null;
        creditCardBill = null;
        slipBill = null;
        chequeBill = null;
        creditCompanyCollections = null;
        departmentProfessionalPayments = null;
        channellingProfessionalPayments = null;
        inwardProfessionalPayments = null;
        //Value
        opdHospitalTotal = 0;
        opdStaffTotal = 0;
        outSideFeeTotal = 0;
        pharmacyTotal = 0;
        inwardPaymentTotal = 0;
        agentPaymentTotal = 0;
        creditCompanyTotal = 0;
        creditCompanyTotalInward = 0;
        pettyCashTotal = 0;
        departmentProfessionalPaymentTotal = 0;
        inwardProfessionalPaymentTotal = 0;
        channellingProfessionalPaymentTotal = 0;
        creditCardTotal = 0;
        chequeTotal = 0;
        slipTotal = 0;
        grantTotal = 0;
        channelTotal = 0;

    }

    public double getInwardProfessionalPaymentTotal() {
        return inwardProfessionalPaymentTotal;
    }

    public void setInwardProfessionalPaymentTotal(double inwardProfessionalPaymentTotal) {
        this.inwardProfessionalPaymentTotal = inwardProfessionalPaymentTotal;
    }

    public double getDepartmentProfessionalPaymentTotal() {
        return departmentProfessionalPaymentTotal;
    }

    public void setDepartmentProfessionalPaymentTotal(double departmentProfessionalPaymentTotal) {
        this.departmentProfessionalPaymentTotal = departmentProfessionalPaymentTotal;
    }

    public double getChannellingProfessionalPaymentTotal() {
        return channellingProfessionalPaymentTotal;
    }

    public void setChannellingProfessionalPaymentTotal(double channellingProfessionalPaymentTotal) {
        this.channellingProfessionalPaymentTotal = channellingProfessionalPaymentTotal;
    }

    public List<DepartmentPayment> getChannellingProfessionalPayments() {
        if (channellingProfessionalPayments == null) {
            channellingProfessionalPayments = new ArrayList<>();
        }
        return channellingProfessionalPayments;
    }

    public void setChannellingProfessionalPayments(List<DepartmentPayment> channellingProfessionalPayments) {
        this.channellingProfessionalPayments = channellingProfessionalPayments;
    }

    public List<DepartmentPayment> getDepartmentPayments() {
        if (departmentProfessionalPayments == null) {
            departmentProfessionalPayments = new ArrayList<>();
        }
        return departmentProfessionalPayments;
    }

    public bookKeepingSummeryRow getBksr() {
        return bksr;
    }

    public void setBksr(bookKeepingSummeryRow bksr) {
        this.bksr = bksr;
    }

    public void setDepartmentPayments(List<DepartmentPayment> departmentPayments) {
        this.departmentProfessionalPayments = departmentPayments;
    }

    public List<String1Value2> getFinalValues() {
        if (finalValues == null) {
            finalValues = new ArrayList<>();
        }
        return finalValues;
    }

    public void setFinalValues(List<String1Value2> finalValues) {
        this.finalValues = finalValues;
    }

    public double getCreditCardTotal() {
        return creditCardTotal;
    }

    public void setCreditCardTotal(double creditCardTotal) {
        this.creditCardTotal = creditCardTotal;
    }

    public double getChequeTotal() {
        return chequeTotal;
    }

    public void setChequeTotal(double chequeTotal) {
        this.chequeTotal = chequeTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public List<Bill> getCreditCardBill() {
        return creditCardBill;
    }

    public void setCreditCardBill(List<Bill> creditCardBill) {
        this.creditCardBill = creditCardBill;
    }

    public List<Bill> getSlipBill() {
        return slipBill;
    }

    public void setSlipBill(List<Bill> slipBill) {
        this.slipBill = slipBill;
    }

    public List<Bill> getChequeBill() {
        return chequeBill;
    }

    public void setChequeBill(List<Bill> chequeBill) {
        this.chequeBill = chequeBill;
    }

    public double getPettyCashTotal() {
        return pettyCashTotal;
    }

    public void setPettyCashTotal(double pettyCashTotal) {
        this.pettyCashTotal = pettyCashTotal;
    }

    public double getPettyTotal() {
        return pettyCashTotal;
    }

    public void setPettyTotal(double pettyTotal) {
        this.pettyCashTotal = pettyTotal;
    }

    public double getCreditCompanyTotal() {
        return creditCompanyTotal;
    }

    public void setCreditCompanyTotal(double creditCompanyTotal) {
        this.creditCompanyTotal = creditCompanyTotal;
    }

    public double getAgentPaymentTotal() {
        return agentPaymentTotal;
    }

    public void setAgentPaymentTotal(double agentPaymentTotal) {
        this.agentPaymentTotal = agentPaymentTotal;
    }

    public double getInwardPaymentTotal() {
        return inwardPaymentTotal;
    }

    public void setInwardPaymentTotal(double inwardPaymentTotal) {
        this.inwardPaymentTotal = inwardPaymentTotal;
    }

    public double getPharmacyTotal() {
        return pharmacyTotal;
    }

    public void setPharmacyTotal(double pharmacyTotal) {
        this.pharmacyTotal = pharmacyTotal;
    }

    public double getOutSideFeeTotal() {
        return outSideFeeTotal;
    }

    public void setOutSideFeeTotal(double outSideFeeTotal) {
        this.outSideFeeTotal = outSideFeeTotal;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public double getChannelTotal() {
        return channelTotal;
    }

    public void setChannelTotal(double channelTotal) {
        this.channelTotal = channelTotal;
    }

    public double getOpdRegentTotal() {
        return opdRegentTotal;
    }

    public void setOpdRegentTotal(double opdRegentTotal) {
        this.opdRegentTotal = opdRegentTotal;
    }

    public long getCountTotals() {
        return countTotals;
    }

    public void setCountTotals(long countTotals) {
        this.countTotals = countTotals;
    }

    public List<String1Value2> getCollections2Hos() {
        if (collections2Hos == null) {
            collections2Hos = new ArrayList<>();
        }
        return collections2Hos;
    }

    public void setCollections2Hos(List<String1Value2> collections2Hos) {
        this.collections2Hos = collections2Hos;
    }

    public List<BillItem> getCreditCompanyCollections() {
        if (creditCompanyCollections == null) {
            creditCompanyCollections = new ArrayList<>();
        }
        return creditCompanyCollections;
    }

    public double getOpdStaffTotal() {
        return opdStaffTotal;
    }

    public void setOpdStaffTotal(double opdStaffTotal) {
        this.opdStaffTotal = opdStaffTotal;
    }

    public void setCreditCompanyCollections(List<BillItem> creditCompanyCollections) {
        this.creditCompanyCollections = creditCompanyCollections;
    }

    public List<Bill> getAgentCollections() {
        if (agentCollections == null) {
            agentCollections = new ArrayList<>();
        }
        return agentCollections;
    }

    public void setAgentCollections(List<Bill> agentCollections) {
        this.agentCollections = agentCollections;
    }

    public List<String3Value2> getInwardCollections() {
        if (inwardCollections == null) {
            inwardCollections = new ArrayList<>();
        }
        return inwardCollections;
    }

    public void setInwardCollections(List<String3Value2> inwardCollections) {
        this.inwardCollections = inwardCollections;
    }

    public List<String1Value2> getOutSideFees() {
        if (outSideFees == null) {
            outSideFees = new ArrayList<>();
        }
        return outSideFees;
    }

    public void setOutSideFees(List<String1Value2> outSideFees) {
        this.outSideFees = outSideFees;
    }

    public List<String1Value2> getPharmacySales() {
        if (pharmacySales == null) {
            pharmacySales = new ArrayList<>();
        }
        return pharmacySales;
    }

    public void setPharmacySales(List<String1Value2> pharmacySales) {
        this.pharmacySales = pharmacySales;
    }

    public List<String1Value2> getChannelBills() {
        if (channelBills == null) {
            channelBills = new ArrayList<>();
        }
        return channelBills;
    }

    public void setChannelBills(List<String1Value2> channelBills) {
        this.channelBills = channelBills;
    }

    public double getOpdHospitalTotal() {
        return opdHospitalTotal;
    }

    public void setOpdHospitalTotal(double opdHospitalTotal) {
        this.opdHospitalTotal = opdHospitalTotal;
    }

    public List<String1Value3> getOpdList() {
        if (opdList == null) {
            opdList = new ArrayList<>();
        }
        return opdList;
    }

    public void setOpdList(List<String1Value3> opdList) {
        this.opdList = opdList;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

//    public void createOPdListDayEndTable() {
//        System.err.println("createOPdCategoryTable");
//        opdList = new ArrayList<>();
//        for (Category cat : getBillBean().fetchBilledOpdCategory(fromDate, toDate, institution)) {
//            System.err.println("Cat " + cat.getName() + " TIME " + new Date());
//            System.err.println("##################");
//            for (Item i : getBillBean().fetchBilledOpdItem(cat, fromDate, toDate, institution)) {
//                //   System.err.println("Item " + i.getName() + " TIME " + new Date());
//            
//                double count = getBillBean().calBilledIcount(i, getFromDate(), getToDate(), getInstitution());
//                double hos = getBillBean().calFeeValue(i, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());
//                //     double pro = getFee(i, FeeType.Staff);
//
//                if (count != 0) {
//                    String1Value2 newD = new String1Value2();
//                    newD.setString(i.getName());
//                    newD.setValue1(count);
//                    newD.setValue2(hos);
//                    newD.setSummery(false);
//                    opdList.add(newD);
//                }
//            }
//
//            double catDbl = getBillBean().calFeeValue(cat, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());
//
//            if (catDbl != 0) {
//                String1Value2 newD = new String1Value2();
//                newD.setString(cat.getName() + " Total : ");
//                newD.setValue2(catDbl);
//                newD.setSummery(true);
//                opdList.add(newD);
//            }
//
//        }
//
//    }
    public void createOPdListDayEndTable() {
        System.err.println("createOPdCategoryTable");
        opdList = new ArrayList<>();
        for (Category cat : getBillBean().fetchBilledOpdCategory(fromDate, toDate, institution)) {
            System.err.println("Cat " + cat.getName() + " TIME " + new Date());
            System.err.println("##################");

            List<Object[]> list = getBillBean().fetchBilledOpdItem(cat, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());

            if (list == null) {
                continue;
            }

            for (Object[] obj : list) {
                Item item = (Item) obj[0];
                Double dbl = (Double) obj[1];
                double count = 0;

                if (dbl != null && dbl != 0) {
                    count = getBillBean().calBilledItemCount(item, getFromDate(), getToDate(), getInstitution());
                }

                String1Value3 newD = new String1Value3();
                newD.setString(item.getName());
                newD.setValue1(count);
                newD.setValue2(dbl);
                newD.setSummery(false);
                opdList.add(newD);

            }

            double catDbl = getBillBean().calFeeValue(cat, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());

            if (catDbl != 0) {
                String1Value3 newD = new String1Value3();
                newD.setString(cat.getName() + " Total : ");
                newD.setValue2(catDbl);
                newD.setSummery(true);
                opdList.add(newD);
            }

        }

    }

    public List<bookKeepingSummeryRow> getBookKeepingSummeryRows() {
        return bookKeepingSummeryRows;
    }

    public void setBookKeepingSummeryRows(List<bookKeepingSummeryRow> bookKeepingSummeryRows) {
        this.bookKeepingSummeryRows = bookKeepingSummeryRows;
    }

    List<bookKeepingSummeryRow> bookKeepingSummeryRows;

    public void createOPdLabListWithProDayEndTable() {

        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();

        List t = new ArrayList();

        String jpql = "select c.name, i.name, count(bi.bill), sum(bf.feeValue), bf.fee.feeType, bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c  "
                + " where bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.paymentMethod = :pm1 "
                + " and  bi.bill.paymentMethod = :pm2 "
                + " and  bi.bill.paymentMethod = :pm3 "
                + " and  bi.bill.paymentMethod = :pm4)"
                + " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        double rf = 0;

        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (Exception e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }

                sr.setTotal(sf + hf + rf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setReagentFee(rf);
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;
                rf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(pre.getReagentFee() + Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());

                    }
                    sr.setTotal(hf + sf + rf);
                    t.add(sr);
                    pre = sr;
                }

            }
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setReagentFee(rf);
        sr.setCatCount(countBilled - countCancelled);

        sr.setTotal(hf + sf + rf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
        //opdHospitalTotal = 0.0;
//        for (bookKeepingSummeryRow bksr : bookKeepingSummeryRows) {
//            opdHospitalTotal += bksr.getTotal();
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        opdHospitalTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.OwnInstitution, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdStaffTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Staff, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdRegentTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), Arrays.asList(paymentMethods));

    }

    public void createOPdLabListWithProDayEndTableWithCredit() {

        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();
        BillType[] btps = new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill};
        List<BillType> lbs = Arrays.asList(btps);

        List t = new ArrayList();

        String jpql = "select c.name, i.name, count(bi.bill), sum(bf.feeValue), bf.fee.feeType, bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c  "
                + " where bi.item.department.institution=:ins "
                + " and  bi.bill.billType in :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.paymentMethod = :pm1 "
                + " or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 "
                + " or  bi.bill.paymentMethod = :pm4 "
                + " or  bi.bill.paymentMethod = :pm5)"
                + " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", lbs);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("pm5", PaymentMethod.Credit);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        double rf = 0;
        countTotals = 0l;

        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (Exception e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }

                sr.setTotal(sf + hf + rf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setReagentFee(rf);
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;
                rf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(pre.getReagentFee() + Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());

                    }
                    sr.setTotal(hf + sf + rf);
                    t.add(sr);
                    pre = sr;
                }

            }

            System.out.println("sr.getCatTotal() = " + sr.getCatCount());
            System.out.println("sr.getCountTotal() = " + sr.getCountTotal());

            calCountTotal(sr.getCatCount());
            System.out.println("End");
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setReagentFee(rf);
        sr.setCatCount(countBilled - countCancelled);        
//        System.out.println("sr.setCatCount = " + sr.getCatCount());
//        countTotal=calCountTotal(sr.getCatCount());
//        sr.setCountTotal(countTotal);
//        System.out.println("sr.setCountTotal = " + sr.getCountTotal());

        sr.setTotal(hf + sf + rf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
        //opdHospitalTotal = 0.0;
//        for (bookKeepingSummeryRow bksr : bookKeepingSummeryRows) {
//            opdHospitalTotal += bksr.getTotal();
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card, PaymentMethod.Credit};
        opdHospitalTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.OwnInstitution, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdStaffTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Staff, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdRegentTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdRegentTotalWithCredit = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), Arrays.asList(paymentMethods));

    }

    public long calCountTotal(long count) {
        System.out.println("countTotals = " + countTotals);
        countTotals += count;
        System.out.println("countTotals = " + countTotals);
        return countTotals;
    }

    @EJB
    ItemFacade itemfacade;

    double totalRegentFee;

    public double getTotalRegentFee() {
        return totalRegentFee;
    }

    public void setTotalRegentFee(double totalRegentFee) {
        this.totalRegentFee = totalRegentFee;
    }

    public List<Item> getItems() {
        Map hm = new HashMap();
        String sql;
        bookKeepingSummeryRows = new ArrayList<>();

        sql = " select distinct(bf.billItem.item) from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.createdAt between :fd and :td "
                + " and bf.bill.billClassType=:class "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.refunded=false "
                + " and bf.bill.billType= :bTp "
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins "
                + " and bf.bill.department=:dep ";

        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("bTp", BillType.InwardBill);
        hm.put("ftp", FeeType.Chemical);
        hm.put("class", BillClassType.BilledBill);
        hm.put("ins", institution);
        hm.put("dep", getSessionController().getDepartment());

        List<Item> itm = itemfacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

        for (Item it : itm) {
            //System.out.println("item" + it.getName());
        }

        return itm;

    }

    public void createInwardFee() {
        Map hm = new HashMap();
        String sql;
        bookKeepingSummeryRows = new ArrayList<>();
        totalRegentFee = 0;

        for (Item item : getItems()) {

            sql = " select sum(bf.feeValue),count(bf.billItem.bill) from BillFee bf "
                    + " where bf.retired=false "
                    + " and bf.bill.createdAt between :fd and :td "
                    + " and bf.billItem.item=:itm "
                    + " and bf.bill.billClassType=:class "
                    + " and bf.bill.cancelled=false "
                    + " and bf.bill.refunded=false "
                    + " and bf.bill.billType= :bTp "
                    + " and bf.fee.feeType=:ftp "
                    + " and bf.bill.institution=:ins "
                    + " and bf.bill.department=:dep ";

            hm.put("fd", fromDate);
            hm.put("td", toDate);
            hm.put("bTp", BillType.InwardBill);
            hm.put("ftp", FeeType.Chemical);
            hm.put("class", BillClassType.BilledBill);
            hm.put("ins", institution);
            hm.put("itm", item);
            hm.put("dep", getSessionController().getDepartment());

            Object[] obj = getBillFeeFacade().findAggregate(sql, hm, TemporalType.TIMESTAMP);

            bookKeepingSummeryRow bkr = new bookKeepingSummeryRow();

            //System.out.println("item" + item);
            bkr.setItemName(item.getName());

            if (obj[0] != null) {
                //System.out.println("ob[1]" + obj[0]);
                double feeTotal = (double) obj[0];
                //System.out.println("feevalue" + feeTotal);
                bkr.setReagentFee(feeTotal);
                totalRegentFee += feeTotal;
            }

            if (obj[1] != null) {
                //System.out.println("ob[1]" + obj[1]);
                long count = (long) obj[1];
                //System.out.println("count" + count);
                bkr.setCatCount(count);
            }

            if (bkr.getCatCount() > 0) {
                bookKeepingSummeryRows.add(bkr);

            }

        }

    }

    public void createOPdLabListWithProDayEndTablebyPaymentMethod() {

        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();

        List t = new ArrayList();

        String jpql = "select c.name, i.name, count(bi.bill), sum(bf.feeValue), bf.fee.feeType, bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c  "
                + " where bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and  bi.bill.paymentMethod = :pm "
                + " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", paymentMethod);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        double rf = 0;

        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (Exception e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }

                sr.setTotal(sf + hf + rf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setReagentFee(rf);
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;
                rf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(pre.getReagentFee() + Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());

                    }
                    sr.setTotal(hf + sf + rf);
                    t.add(sr);
                    pre = sr;
                }

            }
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setReagentFee(rf);
        sr.setCatCount(countBilled - countCancelled);

        sr.setTotal(hf + sf + rf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
        //opdHospitalTotal = 0.0;
//        for (bookKeepingSummeryRow bksr : bookKeepingSummeryRows) {
//            opdHospitalTotal += bksr.getTotal();
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        opdHospitalTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.OwnInstitution, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdStaffTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Staff, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdRegentTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), Arrays.asList(paymentMethods));
        opdRegentTotalByPayMethod = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), paymentMethod);

    }

    public void createOPdLabListWithProDayEndTablebyInward() {

        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();

        List t = new ArrayList();

        String jpql = "select c.name, i.name, count(bi.bill), sum(bf.feeValue), bf.fee.feeType, bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c  "
                + " where bi.item.department.institution=:ins "
                + " and bi.item.department=:dep"
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                //+ " and  bi.bill.paymentMethod = :pm "
                + " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.InwardBill);
        //temMap.put("pm", paymentMethod);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        double rf = 0;

        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (Exception e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }

                sr.setTotal(sf + hf + rf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setReagentFee(rf);
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;
                rf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.OwnInstitution) {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                } else if (ft == FeeType.Chemical) {
                    sr.setReagentFee(Double.valueOf(r[3].toString()));
                    rf += Double.valueOf(r[3].toString());

                }
                sr.setTotal(hf + sf + rf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(pre.getReagentFee() + Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.OwnInstitution) {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    } else if (ft == FeeType.Chemical) {
                        sr.setReagentFee(Double.valueOf(r[3].toString()));
                        rf += Double.valueOf(r[3].toString());

                    }
                    sr.setTotal(hf + sf + rf);
                    t.add(sr);
                    pre = sr;
                }

            }
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setReagentFee(rf);
        sr.setCatCount(countBilled - countCancelled);

        sr.setTotal(hf + sf + rf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
        //opdHospitalTotal = 0.0;
//        for (bookKeepingSummeryRow bksr : bookKeepingSummeryRows) {
//            opdHospitalTotal += bksr.getTotal();
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        opdHospitalTotal = getBillBean().calFeeValueInward(getFromDate(), getToDate(), FeeType.OwnInstitution, sessionController.getInstitution(), getSessionController().getDepartment());
        opdStaffTotal = getBillBean().calFeeValueInward(getFromDate(), getToDate(), FeeType.Staff, sessionController.getInstitution(), getSessionController().getDepartment());
        opdRegentTotal = getBillBean().calFeeValueInward(getFromDate(), getToDate(), FeeType.Chemical, sessionController.getInstitution(), getSessionController().getDepartment());
        //opdRegentTotalByPayMethod = getBillBean().calFeeValue(getFromDate(), getToDate(),FeeType.Chemical, sessionController.getInstitution(),paymentMethod);

    }

    public void createOPdListWithProDayEndTable(List<PaymentMethod> paymentMethods) {
        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();

        List t = new ArrayList();

        String jpql = "select c.name, "
                + " i.name, "
                + " count(bi.bill), "
                + " sum(bf.feeValue), "
                + " bf.fee.feeType, "
                + " bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.institution=:ins "
                + " and bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms";

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        jpql += " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pms", paymentMethods);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (NumberFormatException e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                }

                sr.setTotal(sf + hf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setTotal(hf + sf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                }
                sr.setTotal(hf + sf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else {
                        pre.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    }
                    sr.setTotal(hf + sf);
                    t.add(sr);
                    pre = sr;
                }

            }
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setCatCount(countBilled - countCancelled);
        sr.setTotal(hf + sf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
    }

    private double calBillFee(Date date, FeeType fTy, BillType bty) {

        String sql;

        sql = "select sum(f.feeGrossValue) "
                + " from BillFee f "
                + " where f.bill.retired=false "
                + " and f.bill.billType = :billType "
                + " and f.bill.createdAt between :fd and :td "
                + " and f.bill.toInstitution=:ins "
                + " and f.fee.feeType=:ft";

        Date fd = getCommonFunctions().getStartOfDay(date);
        Date td = getCommonFunctions().getEndOfDay(date);

        System.err.println("From " + fd);
        System.err.println("To " + td);

        Map m = new HashMap();
        m.put("fd", fd);
        m.put("td", td);
        m.put("billType", bty);
        m.put("ins", institution);
        m.put("ft", fTy);
        //    m.put("ins", getSessionController().getInstitution());        

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createLabSummeryInward() {
        bksr = new bookKeepingSummeryRow();

        bksr.setBills(new ArrayList<String1Value3>());

        Date nowDate = getFromDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(nowDate);

        double hospitalFeeTot = 0.0;
        double profeTotal = 0.0;
        double regentTot = 0.0;

        //double regentFee;
        while (nowDate.before(getToDate())) {

            DateFormat df = new SimpleDateFormat("dd MMMM yyyy");
            String formattedDate = df.format(nowDate);

            String1Value3 newRow = new String1Value3();
            newRow.setString(formattedDate);

            double hospitalFeeCash = calBillFee(nowDate, FeeType.OwnInstitution, BillType.InwardBill);

            double proTotCash = calBillFee(nowDate, FeeType.Staff, BillType.InwardBill);

            double regentFeeCash = calBillFee(nowDate, FeeType.Chemical, BillType.InwardBill);

//            //inward bills
//            double hospitaFeeInward = calBillFee(nowDate, FeeType.OwnInstitution, BillType.InwardBill);
//            //double 
            newRow.setValue1(hospitalFeeCash);
            newRow.setValue2(regentFeeCash);
            newRow.setValue3(proTotCash);

            hospitalFeeTot += hospitalFeeCash;
            profeTotal += proTotCash;
            regentTot += regentFeeCash;

            bksr.getBills().add(newRow);

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

        }

        bksr.setHosFee(hospitalFeeTot);
        bksr.setProFee(profeTotal);
        bksr.setReagentFee(regentTot);

    }

    public void createOPdListWithCreditPaid() {
        Map temMap = new HashMap();
        bookKeepingSummeryRows = new ArrayList<>();

        List t = new ArrayList();

        String jpql = "select c.name, "
                + " i.name, "
                + " count(bi.bill), "
                + " sum(bf.feeValue), "
                + " bf.fee.feeType, "
                + " bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.billType= :bTp  "
                + " and bi.bill.id in "
                + " (select paidBillItem.referenceBill.id "
                + " from BillItem paidBillItem"
                + "  where paidBillItem.retired=false "
                + " and paidBillItem.bill.cancelled=false"
                + " and type(paidBillItem.bill)=:class"
                + "  and  paidBillItem.createdAt between :fromDate and :toDate "
                + " and paidBillItem.bill.billType=:paidBtp)  ";

        temMap.put("class", BilledBill.class);

        if (institution != null) {
            jpql += " and bi.bill.institution=:ins ";
            temMap.put("ins", institution);
        }

        if (incomeInstitution != null) {
            jpql += " and bi.item.department.institution=:inIns ";
            temMap.put("inIns", incomeInstitution);
        }

        temMap.put("class", BilledBill.class);

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        jpql += " group by c.name, i.name,  bf.fee.feeType,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("paidBtp", BillType.CashRecieveBill);
//        temMap.put("pms", paymentMethods);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        bookKeepingSummeryRow pre = null;
        int n = 0;

        double sf = 0;
        double hf = 0;
        long icount = 0l;
        bookKeepingSummeryRow sr = null;

        long countBilled = 0l;
        long countCancelled = 0l;
        String itemOuter = "";

        for (Object[] r : lobjs) {
            String category = r[0].toString();
            String item = r[1].toString();
            FeeType ft = (FeeType) r[4];
            BillClassType bct = (BillClassType) r[5];
            long count = 0l;
            try {
                count = Long.valueOf(r[2].toString());
            } catch (NumberFormatException e) {
                //System.out.println("e = " + e);
                count = 0l;
            }

            System.err.println("********************************");
            System.err.println("Category = " + category);
            System.err.println("Item Name = " + item);
            if (!item.equals(itemOuter)) {
                System.err.println("____FIRST");
                itemOuter = item;
                if (bct == BillClassType.BilledBill) {
                    countBilled = count;
                    countCancelled = 0l;
                    //System.out.println("billed = " + countBilled);
                } else {
                    countCancelled = count;
                    countBilled = 0l;
                    //System.out.println("cancelled = " + countCancelled);
                }

            } else {
                System.err.println("___SECOND");
                if (bct == BillClassType.BilledBill) {
                    if (countBilled == 0) {
                        countBilled = count;
                    }
                    //System.out.println("billed = " + countBilled);
                } else {
                    if (countCancelled == 0) {
                        countCancelled = count;
                    }
                    //System.out.println("cancelled = " + countCancelled);
                }

            }

            System.err.println("Count " + count);
            System.err.println("Fee Value " + r[3].toString());
            if (r[4] != null) {
                System.err.println("Fee Type = " + ft);
            }
            System.err.println("Bill Class Type = " + bct);
            System.err.println("********************************");

            if (pre == null) {
                //First Time in the Loop
//                //System.out.println("first row  ");
                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("First time cat row added.");
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                }

                sr.setTotal(sf + hf);
                sr.setCatCount(countBilled - countCancelled);

                t.add(sr);
                pre = sr;

            } else if (!pre.getCategoryName().equals(category)) {
                //Create Total Row
//                //System.out.println("different cat");
                sr = new bookKeepingSummeryRow();
                sr.setTotalRow(true);
                sr.setCategoryName(pre.getCategoryName());
                sr.setSerialNo(n);
                sr.setHosFee(hf);
                sr.setProFee(sf);
                sr.setTotal(hf + sf);
                t.add(sr);
//                //System.out.println("previous tot row added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                hf = 0.0;
                sf = 0.0;

                sr = new bookKeepingSummeryRow();
                sr.setCatRow(true);
                sr.setCategoryName(category);
                sr.setSerialNo(n);
                t.add(sr);
//                //System.out.println("cat title added - " + sr.getCategoryName());
//                //System.out.println("n = " + n);
                n++;

                sr = new bookKeepingSummeryRow();
                sr.setSerialNo(n);
                sr.setCategoryName(category);
                sr.setItemName(item);

                sr.setCatCount(countBilled - countCancelled);
                sr.setBillClassType(bct);

                if (ft == FeeType.Staff) {
                    sr.setProFee(Double.valueOf(r[3].toString()));
                    sf += Double.valueOf(r[3].toString());
                } else {
                    sr.setHosFee(Double.valueOf(r[3].toString()));
                    hf += Double.valueOf(r[3].toString());
                }
                sr.setTotal(hf + sf);
                t.add(sr);
//                //System.out.println("item row added - " + sr.getItemName());
                pre = sr;

            } else {
//                //System.out.println("same cat");
                if (pre.getItemName().equals(item)) {
//                    //System.out.println("same name");

                    if (ft == FeeType.Staff) {
                        pre.setProFee(pre.getProFee() + Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else {
                        pre.setHosFee(pre.getHosFee() + Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    }
                    pre.setCatCount(countBilled - countCancelled);

                } else {
//                    //System.out.println("different name");
                    sr = new bookKeepingSummeryRow();
                    sr.setSerialNo(n);
                    sr.setCategoryName(category);
                    sr.setItemName(item);
                    sr.setCatCount(countBilled - countCancelled);

                    if (ft == FeeType.Staff) {
                        sr.setProFee(Double.valueOf(r[3].toString()));
                        sf += Double.valueOf(r[3].toString());
                    } else {
                        sr.setHosFee(Double.valueOf(r[3].toString()));
                        hf += Double.valueOf(r[3].toString());
                    }
                    sr.setTotal(hf + sf);
                    t.add(sr);
                    pre = sr;
                }

            }
//            //System.out.println("n = " + n);
            n++;
        }

        //Create Total Row
//        //System.out.println("Last cat");
        sr = new bookKeepingSummeryRow();
        sr.setTotalRow(true);
        if (pre != null) {
            sr.setCategoryName(pre.getCategoryName());
        }
        sr.setSerialNo(n);
        sr.setHosFee(hf);
        sr.setProFee(sf);
        sr.setCatCount(countBilled - countCancelled);
        sr.setTotal(hf + sf);
        t.add(sr);
//        //System.out.println("previous tot row added - " + sr.getCategoryName());
//        //System.out.println("n = " + n);
        n++;

        bookKeepingSummeryRows.addAll(t);
    }

    public void createOPdListWithProDayEndTableOld() {
        System.err.println("createOPdCategoryTable");
        opdList = new ArrayList<>();
        for (Category cat : getBillBean().fetchBilledOpdCategory(fromDate, toDate, institution)) {
            System.err.println("Cat " + cat.getName() + " TIME " + new Date());
            System.err.println("##################");

            List<Item> list = getBillBean().fetchBilledOpdItem(cat, getFromDate(), getToDate(), getInstitution());

            if (list == null) {
                continue;
            }

            for (Item item : list) {

                double hosFee = 0;
                double proFee = 0;
                double count = 0;

                hosFee = getBillBean().calFeeValue(item, FeeType.OwnInstitution, fromDate, toDate, institution);
                proFee = getBillBean().calFeeValue(item, FeeType.Staff, fromDate, toDate, institution);
                count = getBillBean().calBilledItemCount(item, getFromDate(), getToDate(), getInstitution());

                String1Value3 newD = new String1Value3();
                newD.setString(item.getName());
                newD.setValue1(count);
                newD.setValue2(hosFee);
                newD.setValue3(proFee);
                newD.setSummery(false);
                opdList.add(newD);

            }

            double catDbl = getBillBean().calFeeValue(cat, getFromDate(), getToDate(), getInstitution());

            if (catDbl != 0) {
                String1Value3 newD = new String1Value3();
                newD.setString(cat.getName() + " Total : ");
                newD.setValue3(catDbl);
                newD.setSummery(true);
                opdList.add(newD);
            }

        }

    }

    public void createOPdListMonthEndTable() {
        System.err.println("createOPdCategoryTable");
        opdList = new ArrayList<>();

        List<Object[]> list = getBillBean().fetchFeeValue(FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());

        if (list == null || list.isEmpty()) {
            return;
        }

        for (Object[] obj : list) {
            Category cat = (Category) obj[0];
            Double dbl = (Double) obj[1];

            String1Value3 newD = new String1Value3();
            newD.setString(cat.getName());
            newD.setValue2(dbl);
            opdList.add(newD);

        }
    }

    public void createOPdListMonthEndTableWithPro() {
        System.err.println("createOPdCategoryTable");
        opdList = new ArrayList<>();

        List<Object[]> list = getBillBean().fetchFeeValue(getFromDate(), getToDate(), getInstitution());

        if (list == null || list.isEmpty()) {
            return;
        }

        for (Object[] obj : list) {
            Category cat = (Category) obj[0];
            Double dbl = (Double) obj[1];

            String1Value3 newD = new String1Value3();
            newD.setString(cat.getName());
            newD.setValue2(dbl);
            opdList.add(newD);

        }
    }

    public void createOutSideFee() {
        System.err.println("Out side Fees");
        outSideFees = new ArrayList<>();
        List<Object[]> list = getBillBean().fetchOutSideInstitutionFees(getFromDate(), getToDate(), getInstitution());

        for (Object[] obj : list) {
            String1Value2 newRow = new String1Value2();
            Department dep = ((Department) obj[0]);
            Double value = (Double) obj[1];
            System.err.println("ins " + dep);
            System.err.println("VAL " + value);
            newRow.setString(dep.getName());
            newRow.setValue1(value);

            if (value != 0) {
                getOutSideFees().add(newRow);
            }
        }
    }

    public void createOutSideFeeWithPro() {
        System.err.println("Out side Fees");
        outSideFees = new ArrayList<>();
        List<Object[]> list = getBillBean().fetchOutSideDepartment(getFromDate(), getToDate(), getInstitution());

        for (Object[] obj : list) {
            String1Value2 newRow = new String1Value2();
            Department department = (Department) obj[0];
            double hosFee = (double) obj[1];
            double proFee = (double) obj[2];

            newRow.setString(department.getName());
            newRow.setValue1(hosFee);
            newRow.setValue2(proFee);

            if (proFee != 0 || hosFee != 0) {
                getOutSideFees().add(newRow);
            }
        }
    }

    public void viewOutSideDepartmentBills() {
        getBillBean().createOutSideDepartment(getFromDate(), getToDate(), getInstitution());
    }

    @Inject
    DepartmentController departmentController;

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    private void createPharmacySale() {
        System.err.println("createPharmacySale");
        pharmacySales = new ArrayList<>();

        //System.err.println("DEP " + d.getName());
        List<Object[]> list = getBillBean().fetchDepartmentSale(getFromDate(), getToDate(), getInstitution());

        for (Object[] obj : list) {
            String1Value2 newRow = new String1Value2();
            Department department = ((Department) obj[0]);
            Double value = (Double) obj[1];

            if (department != null) {
                newRow.setString(department.getName());
            }

            if (value != null) {
                newRow.setValue1(value);
            }

            if (value != null) {
                getPharmacySales().add(newRow);
            }
        }

    }

    private void createChannelBill() {
        System.err.println("createChannellBill");
        channelBills = new ArrayList<>();

        //System.err.println("DEP " + d.getName());
        List<Object[]> list = getBillBean().fetchChannelBills(getFromDate(), getToDate(), getInstitution());

        for (Object[] obj : list) {
            String1Value2 newRow = new String1Value2();
            Department department = ((Department) obj[0]);
            Double value = (Double) obj[1];

            if (department != null) {
                newRow.setString(department.getName());
            }

            if (value != null) {
                newRow.setValue1(value);
            }

            if (value != null) {
                getChannelBills().add(newRow);
            }
        }

    }

    @Inject
    AdmissionTypeController admissionTypeController;

    public AdmissionTypeController getAdmissionTypeController() {
        return admissionTypeController;
    }

    public void setAdmissionTypeController(AdmissionTypeController admissionTypeController) {
        this.admissionTypeController = admissionTypeController;
    }

    public void createInwardCollectionMonth() {
        System.err.println("createInwardCollection");
        inwardCollections = new ArrayList<>();

        List<Object[]> list = getBillBean().calInwardPaymentTotal(fromDate, toDate, institution);

//        AdmissionType headAdmissionType = null;
        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];
            PaymentMethod paymentMethod = (PaymentMethod) obj[1];
            double grantDbl = (Double) obj[2];

            System.err.println("Adm Tp " + admissionType.getName());
            System.err.println("Paym " + paymentMethod);
            System.err.println("Value " + grantDbl);
//            List<Bill> bills = (List<Bill>) (Object) obj[2];

            //HEADER
//            String3Value2 newRow = new String3Value2();
//            newRow.setString1(admissionType.getName() + " : ");
//            newRow.setSummery(true);
////            newRow.setBills(bills);
//            getInwardCollections().add(newRow);
            //Value
            String3Value2 newRow = new String3Value2();
            newRow.setString1(admissionType.getName() + " " + paymentMethod + " : ");
//            newRow.setSummery(true);
            newRow.setValue2(grantDbl);

            getInwardCollections().add(newRow);

        }
    }

    public void createInwardCollection() {
        System.err.println("createInwardCollection");
        inwardCollections = new ArrayList<>();

        List<Object[]> list = getBillBean().calInwardPaymentTotal(fromDate, toDate, institution);

        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];
            PaymentMethod paymentMethod = (PaymentMethod) obj[1];
            double grantDbl = (Double) obj[2];
            //HEADER
            String3Value2 newRow = new String3Value2();
            newRow.setString1(admissionType.getName() + " " + paymentMethod + " : ");
            newRow.setSummery(true);

//            if (grantDbl != 0) {
            getInwardCollections().add(newRow);
//            }

            //BILLS
            for (Bill b : getBillBean().fetchInwardPaymentBills(admissionType, paymentMethod, fromDate, toDate, institution)) {
//                System.err.println("Bills "+b);
                newRow = new String3Value2();
                newRow.setString1(b.getPatientEncounter().getBhtNo());
                newRow.setString2(b.getInsId());
                newRow.setString3(b.getPatientEncounter().getPatient().getPerson().getName());

                Double dbl = b.getNetTotal();
                newRow.setValue1(dbl);

                getInwardCollections().add(newRow);
            }

            //FOOTER
            newRow = new String3Value2();
            newRow.setString1(admissionType.getName() + " " + paymentMethod + " Total : ");
            newRow.setSummery(true);

            newRow.setValue2(grantDbl);

//            if (grantDbl != 0) {
            getInwardCollections().add(newRow);
//            }

        }
    }

//    public void createInwardCollectionMonth() {
//        System.err.println("createInwardCollection");
//        inwardCollections = new ArrayList<>();
//        for (AdmissionType at : getAdmissionTypeController().getItems()) {
//            System.err.println("1 " + at.getName());
//            String3Value2 newRow = new String3Value2();
//            newRow.setString1(at.getName());
//            //    newRow.setSummery(true);
//            Double grantDbl = getBillBean().calInwardPaymentTotal1(at, getFromDate(), getToDate(), getInstitution());
//            newRow.setValue2(grantDbl);
//
//            if (grantDbl != 0) {
//                getInwardCollections().add(newRow);
//            }
//
//        }
//    }
    public void calGrantTotal2HosWithoutPro() {
        grantTotal = 0.0;

        grantTotal = opdHospitalTotal
                + outSideFeeTotal
                + pharmacyTotal
                + inwardPaymentTotal
                + agentPaymentTotal
                + creditCompanyTotal
                + creditCompanyTotalInward
                + pettyCashTotal;

    }

    public void calGrantTotal2HosWithPro() {
        grantTotal = 0.0;

        grantTotal = opdHospitalTotal
                + opdStaffTotal
                + outSideFeeTotal
                + pharmacyTotal
                + channelTotal
                + inwardPaymentTotal
                + agentPaymentTotal
                + creditCompanyTotal
                + creditCompanyTotalInward
                + pettyCashTotal;

    }

    private void createCollections2Hos() {
        System.err.println("createCollections2Hos");
        collections2Hos = new ArrayList<>();
        String1Value2 dd;
        /////////////////
        dd = new String1Value2();
        dd.setString("Collection For the Day");
        calGrantTotal2HosWithPro();
        Double dbl = getGrantTotal();
        System.out.println("dbl = " + dbl);
        dbl = dbl - pettyCashTotal;
        dd.setValue1(dbl);
        collections2Hos.add(dd);
        //////////////////////
        dd = new String1Value2();
        dd.setString("Petty cash Payments");
        dbl = pettyCashTotal;
        dd.setValue1(dbl);
        collections2Hos.add(dd);

    }

    private void createCollections2HosMonth() {
        System.err.println("createCollections2Hos");
        collections2Hos = new ArrayList<>();
        String1Value2 dd;
        ////////////////
        dd = new String1Value2();
        dd.setString("Agent Collections ");
        dd.setValue1(agentPaymentTotal);
        collections2Hos.add(dd);
        //////////////////
        dd = new String1Value2();
        dd.setString("Credit Company Opd Collections ");
        dd.setValue1(creditCompanyTotal);
        collections2Hos.add(dd);
        //////////////////
        dd = new String1Value2();
        dd.setString("Credit Company Inward Collections ");
        dd.setValue1(creditCompanyTotalInward);
        collections2Hos.add(dd);
        /////////////////
        dd = new String1Value2();
        dd.setString("Collection For the Day");
        calGrantTotal2HosWithoutPro();
        Double dbl = getGrantTotal();
        dbl = dbl - pettyCashTotal;
        dd.setValue1(dbl);
        collections2Hos.add(dd);
        //////////////////////
        dd = new String1Value2();
        dd.setString("Petty cash Payments");
        dbl = pettyCashTotal;
        dd.setValue1(dbl);
        collections2Hos.add(dd);

    }

    public void createCashCategoryWithoutProDay() {
        makeNull();
        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());

//        if (Math.abs(lng) > 2) {
//            UtilityController.addErrorMessage("Date Range is too Long");
//            return;
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        createOPdListWithProDayEndTable(Arrays.asList(paymentMethods));
        createOutSideFee();
        createPharmacySale();
        createInwardCollection();
        agentCollections = agentCollections = getBillBean().fetchBills(BillType.AgentPaymentReceiveBill, getFromDate(), getToDate(), getInstitution());
        creditCompanyCollections = getBillBean().fetchBillItems(BillType.CashRecieveBill, true, fromDate, toDate, institution);
        creditCompanyCollectionsInward = getBillBean().fetchBillItems(BillType.CashRecieveBill, false, fromDate, toDate, institution);
        ///////////////////
        opdHospitalTotal = getBillBean().calFeeValue(FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());
        outSideFeeTotal = getBillBean().calOutSideInstitutionFees(fromDate, toDate, institution);
        pharmacyTotal = getBillBean().calInstitutionSale(fromDate, toDate, institution);
        inwardPaymentTotal = getBillBean().calInwardPaymentTotalValue(fromDate, toDate, institution);
        agentPaymentTotal = getBillBean().calBillTotal(BillType.AgentPaymentReceiveBill, fromDate, toDate, institution);
        creditCompanyTotal = getBillBean().calBillTotal(BillType.CashRecieveBill, true, fromDate, toDate, institution);
        creditCompanyTotalInward = getBillBean().calBillTotal(BillType.CashRecieveBill, false, fromDate, toDate, institution);
        pettyCashTotal = getBillBean().calBillTotal(BillType.PettyCash, fromDate, toDate, institution);
        createCollections2Hos();

//        createDoctorPaymentInward();
        createDoctorPaymentInwardByCategoryAndSpeciality();

        creditCardBill = getBillBean().fetchBills(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeBill = getBillBean().fetchBills(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipBill = getBillBean().fetchBills(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        /////////////////
        inwardProfessionalPaymentTotal = getBillBean().calDoctorPaymentInward(fromDate, toDate, institution);
        creditCardTotal = getBillBean().calBillTotal(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeTotal = getBillBean().calBillTotal(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipTotal = getBillBean().calBillTotal(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        createFinalSummery();
    }

    public void createDoctorPaymentChannelling() {
        System.err.println("Doctor Payment Channelling");
        channellingProfessionalPayments = new ArrayList<>();
        List<BillType> bts = new ArrayList<>();

        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);
        bts.add(BillType.ChannelAgent);

        //System.out.println("fetching channeling payments");
        List<Object[]> list = getBillBean().fetchDoctorPayment(fromDate, toDate, bts);
        //System.out.println("list = " + list);

        for (Object[] obj : list) {

            //System.out.println("obj = " + obj);
            Department department = (Department) obj[0];
            double dbl = (Double) obj[1];

            DepartmentPayment newRow = new DepartmentPayment();
            newRow.setDepartment(department);
            newRow.setTotalPayment(dbl);

            //System.out.println("newRow = " + newRow);
            if (dbl != 0) {
                channellingProfessionalPayments.add(newRow);
            }

        }

    }

    public void createDoctorPaymentOpd() {
        System.err.println("Doctor Payment OPD");
        departmentProfessionalPayments = new ArrayList<>();
        List<Object[]> list = getBillBean().fetchDoctorPayment(fromDate, toDate, BillType.OpdBill,institution);

        for (Object[] obj : list) {
            Department department = (Department) obj[0];
            double dbl = (Double) obj[1];

            DepartmentPayment newRow = new DepartmentPayment();
            newRow.setDepartment(department);
            newRow.setTotalPayment(dbl);

            if (dbl != 0) {
                departmentProfessionalPayments.add(newRow);
            }

        }

    }

    public List<DepartmentPayment> getDepartmentProfessionalPayments() {
        return departmentProfessionalPayments;
    }

    public void setDepartmentProfessionalPayments(List<DepartmentPayment> departmentProfessionalPayments) {
        this.departmentProfessionalPayments = departmentProfessionalPayments;
    }

    public List<String1Value2> getInwardProfessionalPayments() {
        return inwardProfessionalPayments;
    }

    public void setInwardProfessionalPayments(List<String1Value2> inwardProfessionalPayments) {
        this.inwardProfessionalPayments = inwardProfessionalPayments;
    }

    public void createDoctorPaymentInwardByCategoryAndSpeciality() {
        professionalPaymentsByAdmissionTypeAndCategorys = new ArrayList<>();
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", institution);
//        hm.put("bclass", BilledBill.class);
        String sql = "Select b.paidForBillFee.bill.patientEncounter.admissionType.name,"
                + " b.paidForBillFee.staff.speciality.name , sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.bill.institution=:ins "
                //                + " and b.bill.cancelled=false"
                //                + " and type(b.bill)=:bclass"
                + " and (b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " group by b.paidForBillFee.bill.patientEncounter.admissionType.name, b.paidForBillFee.staff.speciality.name "
                + " order by b.paidForBillFee.bill.patientEncounter.admissionType.name, b.paidForBillFee.staff.speciality.name ";

        //   //System.out.println("hm = " + hm);
        //   //System.out.println("sql = " + sql);
        List<Object[]> objs = getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);
        //   //System.out.println("objs = " + objs);
        ProfessionalPaymentsByAdmissionTypeAndCategory thisPro = null;
        ProfessionalPaymentsByAdmissionTypeAndCategory prePro = null;
        ProfessionalPaymentsByAdmissionTypeAndCategory addPro = null;

        double admittionTypeTptal = 0.0;
        double totalProfessinal = 0.0;

        if (objs == null) {
            return;
        }

        for (Object[] o : objs) {
            thisPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
            thisPro.setAdmissionType(o[0].toString());
            thisPro.setSpeciality(o[1].toString());

            try {
                thisPro.setSpecialityValie(Double.valueOf(o[2].toString()));
            } catch (NumberFormatException e) {
                thisPro.setSpecialityValie(0.0);
            }

            totalProfessinal = totalProfessinal + thisPro.getSpecialityValie();

            if (prePro == null) {
                admittionTypeTptal = admittionTypeTptal + thisPro.getSpecialityValie();
                prePro = thisPro;

                addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
                addPro.setAdmissionType(thisPro.getAdmissionType());
                professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);

                addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
                addPro.setSpeciality(thisPro.getSpeciality());
                addPro.setSpecialityValie(thisPro.getSpecialityValie());
                professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);
            } else {
                if (prePro.getAdmissionType().equals(thisPro.getAdmissionType())) {
                    admittionTypeTptal = admittionTypeTptal + thisPro.getSpecialityValie();

                    addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
                    addPro.setSpeciality(thisPro.getSpeciality());
                    addPro.setSpecialityValie(thisPro.getSpecialityValie());
                    professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);

                    prePro = thisPro;
                } else {

                    addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
                    addPro.setAdmissionType(thisPro.getAdmissionType());
                    addPro.setAdmissionTypeValue(admittionTypeTptal);
                    professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);

                    admittionTypeTptal = thisPro.getSpecialityValie();

                    addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
                    addPro.setSpeciality(thisPro.getSpeciality());
                    addPro.setSpecialityValie(thisPro.getSpecialityValie());
                    professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);

                    prePro = thisPro;
                }
            }

        }

        addPro = new ProfessionalPaymentsByAdmissionTypeAndCategory();
        addPro.setAdmissionTypeValue(admittionTypeTptal);
        professionalPaymentsByAdmissionTypeAndCategorys.add(addPro);

        inwardProfessionalPaymentTotal = totalProfessinal;

    }

//    
//    public void createDoctorPaymentInwardTemporaryTesting() {
//        System.err.println("Doctor Payment Inward");
//        inwardProfessionalPayments = new ArrayList<>();
//        List<Object[]> list = getBillBean().fetchDoctorPaymentInwardTemporaryTesting(fromDate, toDate);
//
//        for (Object[] obj : list) {
//            AdmissionType admissionType = (AdmissionType) obj[0];
//            double dbl = (Double) obj[1];
//
//            String1Value2 header = new String1Value2();
//            header.setSummery(true);
//            header.setString(admissionType.getName());
//
//            if (dbl != 0) {
//                inwardProfessionalPayments.add(header);
//            }
//
//            List<Object[]> listInner = getBillBean().fetchDoctorPaymentInward(admissionType, fromDate, toDate);
//
//            for (Object[] objIn : listInner) {
//                Speciality speciality = (Speciality) objIn[0];
//                dbl = (Double) objIn[1];
//
//                if (dbl != 0) {
//                    String1Value2 data = new String1Value2();
//                    data.setString(speciality.getName());
//                    data.setValue1(dbl);
//                    inwardProfessionalPayments.add(data);
//                }
//            }
//
//            String1Value2 footer = new String1Value2();
//            footer.setSummery(true);
//            footer.setString(admissionType.getName() + " Total : ");
//            footer.setValue2(dbl);
//
//            if (dbl != 0) {
//                inwardProfessionalPayments.add(footer);
//            }
//
//        }
//
//    }
//
    public void createDoctorPaymentInward() {
        System.err.println("Doctor Payment Inward");
        inwardProfessionalPayments = new ArrayList<>();
        List<Object[]> list = getBillBean().fetchDoctorPaymentInward(fromDate, toDate);

        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];
            double dbl = (Double) obj[1];

            String1Value2 header = new String1Value2();
            header.setSummery(true);
            header.setString(admissionType.getName());

            if (dbl != 0) {
                inwardProfessionalPayments.add(header);
            }

            List<Object[]> listInner = getBillBean().fetchDoctorPaymentInward(admissionType, fromDate, toDate);

            for (Object[] objIn : listInner) {
                Speciality speciality = (Speciality) objIn[0];
                dbl = (Double) objIn[1];

                if (dbl != 0) {
                    String1Value2 data = new String1Value2();
                    data.setString(speciality.getName());
                    data.setValue1(dbl);
                    inwardProfessionalPayments.add(data);
                }
            }

            String1Value2 footer = new String1Value2();
            footer.setSummery(true);
            footer.setString(admissionType.getName() + " Total : ");
            footer.setValue2(dbl);

            if (dbl != 0) {
                inwardProfessionalPayments.add(footer);
            }

        }

    }

    public void createCashCategoryWithProDay() {
        makeNull();
        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            UtilityController.addErrorMessage("Date Range is too Long");
            return;
        }

        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        createOPdListWithProDayEndTable(Arrays.asList(paymentMethods));
        createOutSideFeeWithPro();
        createPharmacySale();
        createChannelBill();
        createInwardCollection();
        agentCollections = agentCollections = getBillBean().fetchBills(BillType.AgentPaymentReceiveBill, getFromDate(), getToDate(), getInstitution());
        creditCompanyCollections = getBillBean().fetchBillItems(BillType.CashRecieveBill, true, fromDate, toDate, institution);
        creditCompanyCollectionsInward = getBillBean().fetchBillItems(BillType.CashRecieveBill, false, fromDate, toDate, institution);
        /////
        createDoctorPaymentOpd();
        createDoctorPaymentChannelling();
        createDoctorPaymentInward();
        ///////////////////
        opdHospitalTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.OwnInstitution, getInstitution(), creditCompany, Arrays.asList(paymentMethods));
        opdStaffTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), FeeType.Staff, getInstitution(), creditCompany, Arrays.asList(paymentMethods));
        outSideFeeTotal = getBillBean().calOutSideInstitutionFeesWithPro(fromDate, toDate, institution);
        pharmacyTotal = getBillBean().calInstitutionSale(fromDate, toDate, institution);
        channelTotal = getBillBean().calChannelTotal(fromDate, toDate, institution);
        inwardPaymentTotal = getBillBean().calInwardPaymentTotalValue(fromDate, toDate, institution);
        agentPaymentTotal = getBillBean().calBillTotal(BillType.AgentPaymentReceiveBill, fromDate, toDate, institution);
        creditCompanyTotal = getBillBean().calBillTotal(BillType.CashRecieveBill, true, fromDate, toDate, institution);
        creditCompanyTotalInward = getBillBean().calBillTotal(BillType.CashRecieveBill, false, fromDate, toDate, institution);
        pettyCashTotal = getBillBean().calBillTotal(BillType.PettyCash, fromDate, toDate, institution);
        createCollections2Hos();
        departmentProfessionalPaymentTotal = getBillBean().calDoctorPayment(fromDate, toDate, BillType.OpdBill,institution);

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelPaid);
        channellingProfessionalPaymentTotal = getBillBean().calDoctorPayment(fromDate, toDate, bts);

        createDoctorPaymentInwardByCategoryAndSpeciality();
        creditCardBill = getBillBean().fetchBills(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeBill = getBillBean().fetchBills(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipBill = getBillBean().fetchBills(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        /////////////////
        creditCardTotal = getBillBean().calBillTotal(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeTotal = getBillBean().calBillTotal(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipTotal = getBillBean().calBillTotal(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        createFinalSummery();
    }

    Institution creditCompany;

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public void processCreditItems() {
        makeNull();
        PaymentMethod[] paymentMethods = {PaymentMethod.Credit};
        createOPdListWithProDayEndTable(Arrays.asList(paymentMethods));
        opdHospitalTotal = 0.0;
        opdStaffTotal = 0.0;
        for (bookKeepingSummeryRow b : bookKeepingSummeryRows) {
            if (b.isTotalRow()) {
                //System.out.println("b.getHosFee() = " + b.getHosFee());
                //System.out.println("b.getProFee() = " + b.getProFee());
                opdHospitalTotal += b.getHosFee();
                opdStaffTotal += b.getProFee();
            }
        }
    }

    public void processCreditPaidItems() {
        makeNull();
        createOPdListWithCreditPaid();
        opdHospitalTotal = 0.0;
        opdStaffTotal = 0.0;
        for (bookKeepingSummeryRow b : bookKeepingSummeryRows) {
            if (b.isTotalRow()) {
                //System.out.println("b.getHosFee() = " + b.getHosFee());
                //System.out.println("b.getProFee() = " + b.getProFee());
                opdHospitalTotal += b.getHosFee();
                opdStaffTotal += b.getProFee();
            }
        }
    }

    public void createCashCategoryWithoutProMonth() {
        makeNull();
        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());

//        if (Math.abs(lng) > 32) {
//            UtilityController.addErrorMessage("Date Range is too Long");
//            return;
//        }
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        createOPdListWithProDayEndTable(Arrays.asList(paymentMethods));
        createOutSideFee();
        createPharmacySale();
        createInwardCollectionMonth();
        ///////////////////
        opdHospitalTotal = getBillBean().calFeeValue(FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());
        outSideFeeTotal = getBillBean().calOutSideInstitutionFees(fromDate, toDate, institution);
        pharmacyTotal = getBillBean().calInstitutionSale(fromDate, toDate, institution);
        inwardPaymentTotal = getBillBean().calInwardPaymentTotalValue(fromDate, toDate, institution);
        agentPaymentTotal = getBillBean().calBillTotal(BillType.AgentPaymentReceiveBill, fromDate, toDate, institution);
        creditCompanyTotal = getBillBean().calBillTotal(BillType.CashRecieveBill, true, fromDate, toDate, institution);
        creditCompanyTotalInward = getBillBean().calBillTotal(BillType.CashRecieveBill, false, fromDate, toDate, institution);
        pettyCashTotal = getBillBean().calBillTotal(BillType.PettyCash, fromDate, toDate, institution);
        createCollections2HosMonth();
        //createDoctorPaymentInward();
        /////////////////
        createDoctorPaymentInwardByCategoryAndSpeciality();
        creditCardTotal = getBillBean().calBillTotal(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeTotal = getBillBean().calBillTotal(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipTotal = getBillBean().calBillTotal(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        createFinalSummeryMonth();
    }

    public void createCashCategoryWithProMonth() {
        //System.out.println("creating cash category with pro month");
        makeNull();
        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        PaymentMethod[] paymentMethods = {PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card};
        createOPdListWithProDayEndTable(Arrays.asList(paymentMethods));
        createOutSideFeeWithPro();
        createPharmacySale();
        createInwardCollectionMonth();
//        agentCollections = getBillBean().fetchBills(BillType.AgentPaymentReceiveBill, getFromDate(), getToDate(), getInstitution());
//        creditCompanyCollections = getBillBean().fetchBillItems(BillType.CashRecieveBill, fromDate, toDate, institution);
        createDoctorPaymentOpd();
        createDoctorPaymentChannelling();
        createDoctorPaymentInward();
        ///////////////////
        opdHospitalTotal = getBillBean().calFeeValue(getFromDate(), getToDate(), getInstitution(), creditCompany, Arrays.asList(paymentMethods));
        outSideFeeTotal = getBillBean().calOutSideInstitutionFeesWithPro(fromDate, toDate, institution);
        pharmacyTotal = getBillBean().calInstitutionSale(fromDate, toDate, institution);
        inwardPaymentTotal = getBillBean().calInwardPaymentTotalValue(fromDate, toDate, institution);
        agentPaymentTotal = getBillBean().calBillTotal(BillType.AgentPaymentReceiveBill, fromDate, toDate, institution);
        creditCompanyTotal = getBillBean().calBillTotal(BillType.CashRecieveBill, fromDate, toDate, institution);
        pettyCashTotal = getBillBean().calBillTotal(BillType.PettyCash, fromDate, toDate, institution);
        createCollections2HosMonth();
        departmentProfessionalPaymentTotal = getBillBean().calDoctorPayment(fromDate, toDate, BillType.OpdBill);
        createDoctorPaymentInwardByCategoryAndSpeciality();
//        creditCardBill = getBillBean().fetchBills(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
//        chequeBill = getBillBean().fetchBills(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
//        slipBill = getBillBean().fetchBills(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        /////////////////
        creditCardTotal = getBillBean().calBillTotal(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        chequeTotal = getBillBean().calBillTotal(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        slipTotal = getBillBean().calBillTotal(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        createFinalSummeryMonth();
    }

    private void createFinalSummery() {
        System.err.println("createFinalSummery");
        finalValues = new ArrayList<>();
        String1Value2 dd;
        ////////              
        dd = new String1Value2();
        dd.setString("Net Cash");
        Double tmp = grantTotal - (creditCardTotal + slipTotal + chequeTotal + departmentProfessionalPaymentTotal + channellingProfessionalPaymentTotal + inwardProfessionalPaymentTotal);
        dd.setValue1(tmp);
        finalValues.add(dd);

        dd = new String1Value2();
        dd.setString("Lab Handover Total");

        finalValues.add(dd);

        dd = new String1Value2();
        dd.setString("Final Cash");
        finalValues.add(dd);

        dd = new String1Value2();
        dd.setString("C/F Cash Balance");

        finalValues.add(dd);

    }

    private void createFinalSummeryMonth() {
        System.err.println("createFinalSummery");
        finalValues = new ArrayList<>();
        String1Value2 dd;
        ////////       
        dd = new String1Value2();
        dd.setString("Card Total ");
        dd.setValue1(0 - creditCardTotal);
        finalValues.add(dd);
        ///////////
        dd = new String1Value2();
        dd.setString("Cheque Total ");
        dd.setValue1(0 - chequeTotal);
        finalValues.add(dd);
        ///////////
        dd = new String1Value2();
        dd.setString("Slip Total ");
        dd.setValue1(0 - slipTotal);
        finalValues.add(dd);
        ///////////

        dd = new String1Value2();
        dd.setString("Net Cash");
        Double tmp = grantTotal - (creditCardTotal + slipTotal + chequeTotal + departmentProfessionalPaymentTotal + inwardProfessionalPaymentTotal);
        dd.setValue1(tmp);
        finalValues.add(dd);

        dd = new String1Value2();
        dd.setString("Lab Handover Total");

        finalValues.add(dd);

        dd = new String1Value2();
        dd.setString("Final Cash");

        finalValues.add(dd);

    }

    /**
     * Creates a new instance of BookKeepingSummery
     */
    public BookKeepingSummery() {
    }

    List<ProfessionalPaymentsByAdmissionTypeAndCategory> professionalPaymentsByAdmissionTypeAndCategorys;

    public List<ProfessionalPaymentsByAdmissionTypeAndCategory> getProfessionalPaymentsByAdmissionTypeAndCategorys() {
        return professionalPaymentsByAdmissionTypeAndCategorys;
    }

    public void setProfessionalPaymentsByAdmissionTypeAndCategorys(List<ProfessionalPaymentsByAdmissionTypeAndCategory> professionalPaymentsByAdmissionTypeAndCategorys) {
        this.professionalPaymentsByAdmissionTypeAndCategorys = professionalPaymentsByAdmissionTypeAndCategorys;
    }

    public Institution getIncomeInstitution() {
        return incomeInstitution;
    }

    public void setIncomeInstitution(Institution incomeInstitution) {
        this.incomeInstitution = incomeInstitution;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Institution getLoggediInstitution() {
        return loggediInstitution;
    }

    public void setLoggediInstitution(Institution loggediInstitution) {
        this.loggediInstitution = loggediInstitution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public class ProfessionalPaymentsByAdmissionTypeAndCategory {

        String admissionType;
        String speciality;
        double specialityValie;
        double admissionTypeValue;
        double totalValue;
        String strSpecialityValue;
        String strAdmissionTypeValue;

        public String getStrSpecialityValue() {
            if (specialityValie == 0.0) {
                return "";
            } else {
                DecimalFormat df = new DecimalFormat("#,##0.00");
                return df.format(specialityValie);
            }
        }

        public void setStrSpecialityValue(String strSpecialityValue) {
            this.strSpecialityValue = strSpecialityValue;
        }

        public String getStrAdmissionTypeValue() {
            if (admissionTypeValue == 0.0) {
                return "";
            } else {
                DecimalFormat df = new DecimalFormat("#,##0.00");
                return df.format(Math.abs(admissionTypeValue));
            }
        }

        public void setStrAdmissionTypeValue(String strAdmissionTypeValue) {
            this.strAdmissionTypeValue = strAdmissionTypeValue;
        }

        public String getAdmissionType() {
            return admissionType;
        }

        public void setAdmissionType(String admissionType) {
            this.admissionType = admissionType;
        }

        public String getSpeciality() {
            return speciality;
        }

        public void setSpeciality(String speciality) {
            this.speciality = speciality;
        }

        public double getSpecialityValie() {
            return specialityValie;
        }

        public void setSpecialityValie(double specialityValie) {
            this.specialityValie = specialityValie;
        }

        public double getAdmissionTypeValue() {
            return admissionTypeValue;
        }

        public void setAdmissionTypeValue(double admissionTypeValue) {
            this.admissionTypeValue = admissionTypeValue;
        }

        public double getTotalValue() {
            return totalValue;
        }

        public void setTotalValue(double totalValue) {
            this.totalValue = totalValue;
        }

    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public class bookKeepingSummeryRow {

        String categoryName;
        String itemName;
        boolean catRow;
        boolean totalRow;
        long catCount;
        long countTotal;
        double hosFee;
        double proFee;
        double reagentFee;
        double total;
        double subTotal;
        BillClassType billClassType;
        List<String1Value3> bills;

        int serialNo;

        public BillClassType getBillClassType() {
            return billClassType;
        }

        public void setBillClassType(BillClassType billClassType) {
            this.billClassType = billClassType;
        }

        public int getSerialNo() {
            return serialNo;
        }

        public void setSerialNo(int serialNo) {
            this.serialNo = serialNo;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public boolean isCatRow() {
            return catRow;
        }

        public void setCatRow(boolean catRow) {
            this.catRow = catRow;
        }

        public boolean isTotalRow() {
            return totalRow;
        }

        public void setTotalRow(boolean totalRow) {
            this.totalRow = totalRow;
        }

        public long getCatCount() {
            return catCount;
        }

        public void setCatCount(long catCount) {
            this.catCount = catCount;
        }

        public double getHosFee() {
            return hosFee;
        }

        public void setHosFee(double hosFee) {
            this.hosFee = hosFee;
        }

        public double getProFee() {
            return proFee;
        }

        public void setProFee(double proFee) {
            this.proFee = proFee;
        }

        public double getReagentFee() {
            return reagentFee;
        }

        public void setReagentFee(double reagentFee) {
            this.reagentFee = reagentFee;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public List<String1Value3> getBills() {
            return bills;
        }

        public void setBills(List<String1Value3> bills) {
            this.bills = bills;
        }

        public double getSubTotal() {
            return subTotal;
        }

        public void setSubTotal(double subTotal) {
            this.subTotal = subTotal;
        }

        public long getCountTotal() {
            return countTotal;
        }

        public void setCountTotal(long countTotal) {
            this.countTotal = countTotal;
        }

        @Override
        public int hashCode() {
            return serialNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final bookKeepingSummeryRow other = (bookKeepingSummeryRow) obj;
            if (other.getSerialNo() == this.getSerialNo()) {
                return true;
            } else {
                return false;
            }
        }

    }

    public List<BillItem> getCreditCompanyCollectionsInward() {
        return creditCompanyCollectionsInward;
    }

    public void setCreditCompanyCollectionsInward(List<BillItem> creditCompanyCollectionsInward) {
        this.creditCompanyCollectionsInward = creditCompanyCollectionsInward;
    }

    public double getCreditCompanyTotalInward() {
        return creditCompanyTotalInward;
    }

    public void setCreditCompanyTotalInward(double creditCompanyTotalInward) {
        this.creditCompanyTotalInward = creditCompanyTotalInward;
    }

    public double getOpdRegentTotalWithCredit() {
        return opdRegentTotalWithCredit;
    }

    public void setOpdRegentTotalWithCredit(double opdRegentTotalWithCredit) {
        this.opdRegentTotalWithCredit = opdRegentTotalWithCredit;
    }

    public double getOpdRegentTotalByPayMethod() {
        return opdRegentTotalByPayMethod;
    }

    public void setOpdRegentTotalByPayMethod(double opdRegentTotalByPayMethod) {
        this.opdRegentTotalByPayMethod = opdRegentTotalByPayMethod;
    }

}
