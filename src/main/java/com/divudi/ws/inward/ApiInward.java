/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.inward.Admission;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BilledBillFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientEncounterFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * REST Web Service
 *
 * @author Archmage-Dushan
 */
@Path("apiInward")
@RequestScoped
public class ApiInward {

    @Context
    private UriInfo context;

    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BilledBillFacade billedBillFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    InstitutionFacade institutionFacade;

    @EJB
    BillNumberGenerator billNumberGenerator;

    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    InwardBeanController inwardBeanController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    SessionController sessionController;
    @Inject
    InstitutionController institutionController;

    /**
     * Creates a new instance of ApiInward
     */
    public ApiInward() {
    }

    //--Api
    @GET
    @Path("/admissions")
    @Produces("application/json")
    public String getAddmissions() {
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();

        try {
            List<PatientEncounter> encounters = fetchAdmissionToPay();
            if (!encounters.isEmpty()) {
                for (PatientEncounter pe : encounters) {
                    JSONObject object = new JSONObject();
                    object.put("pe_id", pe.getId());
                    object.put("pe_bht_no", pe.getBhtNo());
                    if (pe.getPatient().getPerson()!=null) {
                        object.put("pe_patient_name", pe.getPatient().getPerson().getName());
                        object.put("pe_patient_home_phone", pe.getPatient().getPerson().getPhone());
                        object.put("pe_patient_mobile", pe.getPatient().getPerson().getMobile());
                        object.put("pe_patient_nic", pe.getPatient().getPerson().getNic());
                    } else {
                        object.put("pe_patient_name", "No Person Details");
                        object.put("pe_patient_home_phone", "No Person Details");
                        object.put("pe_patient_mobile", "No Person Details");
                        object.put("pe_patient_nic", "No Person Details");
                    }
                    if (pe.getFinalBill() != null) {
                        object.put("pe_patient_final_total", pe.getFinalBill().getNetTotal());
                        object.put("pe_patient_paid_amount", pe.getFinalBill().getPaidAmount());
                    }
                    array.put(object);
                }
                jSONObjectOut.put("admission", array);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("admission", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("admission", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/banks")
    @Produces("application/json")
    public String getBanks() {
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        try {
            List<Institution> banks = fetchBanks();
            if (!banks.isEmpty()) {
                for (Institution i : banks) {
                    JSONObject object = new JSONObject();
                    object.put("bank_id", i.getId());
                    object.put("bank_name", i.getName());
                    array.put(object);
                }
                jSONObjectOut.put("bank", array);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("bank", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            jSONObjectOut.put("bank", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;

    }

    @GET
    @Path("/validateAdmission/{bht_no}")
    @Produces("application/json")
    public String getAdmissionIsValidate(@PathParam("bht_no") String bht_no) {
        JSONObject jSONObjectOut = new JSONObject();
        try {
            if (checkAdmissionIsValied(bht_no)) {
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "Valied");
            } else {
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Not Valid");
            }
        } catch (Exception e) {
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;

    }

//      {
//"bank_id": 16664599,
//"bank_name": "Amana Bank"
//},
//  {
//"bank_id": 116759,
//"bank_name": "Amarican Express"
//},
//  {
//"bank_id": 62127,
//"bank_name": "Bank Of Ceylon"
//},
//  {
//"bank_id": 4584929,
//"bank_name": "Citi Bank"
//},
//  {
//"bank_id": 119179,
//"bank_name": "Commercial Bank"
//},
    @GET
    @Path("/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}")
    @Produces("application/json")
    public String getPayment(@PathParam("bht_no") String bht_no, @PathParam("bank_id") String bank_id,
            @PathParam("credit_card_ref") String credit_card_ref, @PathParam("amount") String amount) {
        JSONObject jSONObjectOut = new JSONObject();
        try {
            PatientEncounter pe = fetchPatientEncounter(bht_no);
            Institution bank = fetchBank(Long.parseLong(bank_id));
            if (pe != null && bank != null && Long.parseLong(amount) > 0) {
                PaymentMethodData pmd = new PaymentMethodData();
                pmd.getCreditCard().setNo(credit_card_ref);
                pmd.getCreditCard().setInstitution(bank);

                Bill b = pay(PaymentMethod.OnlineSettlement, pe, Long.parseLong(amount), pmd);
                if (b != null) {
                    JSONObject object = new JSONObject();
                    object.put("bill_no_ins", b.getInsId());
                    object.put("bill_no_dept", b.getDeptId());
                    object.put("bill_bht_no", b.getPatientEncounter().getBhtNo());
                    object.put("bill_amount", b.getNetTotal());
                    object.put("bill_bank", b.getBank().getName());
                    object.put("bill_crad_ref_no", b.getCreditCardRefNo());

                    jSONObjectOut.put("bill", object);
                    jSONObjectOut.put("error", "0");
                    jSONObjectOut.put("error_description", "");
                } else {
                    jSONObjectOut.put("bill", "");
                    jSONObjectOut.put("error", "1");
                    jSONObjectOut.put("error_description", "Payment Not Done.");
                }

            } else {
                jSONObjectOut.put("bill", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Payment Not Done.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("bill", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;

    }

    //--Methords
    private List<PatientEncounter> fetchAdmissionToPay() {
        List<PatientEncounter> suggestions;
        String sql;
        HashMap m = new HashMap();

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo is not null "
                + " and pe.paymentFinalized!=true "
                + " and pe.discharged!=true "
                + " order by pe.id desc";
        m.put("class", Admission.class);
        suggestions = getPatientEncounterFacade().findByJpql(sql, m, 20);
//        //// // System.out.println("1.suggestions.size() = " + suggestions.size());

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo is not null "
                + " and pe.paymentFinalized=true "
                + " and pe.discharged=true "
                + " and (abs(pe.finalBill.netTotal) - abs(pe.finalBill.paidAmount)) >:d "
                + " order by pe.id desc";

        m.put("d", 0.1);
        List<PatientEncounter> temps = getPatientEncounterFacade().findByJpql(sql, m, 20);
//        //// // System.out.println("temps.size() = " + temps.size());
        List<PatientEncounter> removeTemps = new ArrayList<>();
        for (PatientEncounter p : temps) {
//            //// // System.out.println("p.getFinalBill().getNetTotal() = " + p.getFinalBill().getNetTotal());
//            //// // System.out.println("p.getFinalBill().getPaidAmount() = " + p.getFinalBill().getPaidAmount());
            double d = fetchCreditPaymentTotal(p);
            if (p.getFinalBill().getNetTotal() - (p.getFinalBill().getPaidAmount() + d) < 0.1) {
                removeTemps.add(p);
            }
            p.getFinalBill().setPaidAmount(d + p.getFinalBill().getPaidAmount());
        }
//        //// // System.out.println("removeTemps.size() = " + removeTemps.size());
        temps.removeAll(removeTemps);
//        //// // System.out.println("temps.size() = " + temps.size());

        suggestions.addAll(temps);
//        //// // System.out.println("2.suggestions.size() = " + suggestions.size());
//        for (PatientEncounter pe : suggestions) {
//            //// // System.out.println("pe.getBhtNo() = " + pe.getBhtNo());
//        }

        return suggestions;
    }

    private List<Institution> fetchBanks() {

        List<Institution> banks = getInstitutionController().completeInstitution(null, InstitutionType.Bank);

        return banks;
    }

    public boolean checkAdmissionIsValied(String bhtNo) {
        String sql;
        HashMap m = new HashMap();

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo=:bht "
                + " and pe.paymentFinalized!=true "
                + " and pe.discharged!=true "
                + " order by pe.bhtNo";
        m.put("class", Admission.class);
        m.put("bht", bhtNo);
        List<PatientEncounter> temps = getPatientEncounterFacade().findByJpql(sql, m);
//        //// // System.out.println("1.temps.size() = " + temps.size());
        if (temps.size() > 0) {
            return true;
        }
        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo=:bht "
                + " and pe.paymentFinalized=true "
                + " and pe.discharged=true "
                + " and (abs(pe.finalBill.netTotal) - abs(pe.finalBill.paidAmount)) >:d "
                + " order by pe.bhtNo ";

        m.put("d", 0.1);
        temps = getPatientEncounterFacade().findByJpql(sql, m);
//        //// // System.out.println("2.temps.size() = " + temps.size());
        if (temps.size() > 0) {
            return true;
        }
        return false;
    }

    public Bill pay(PaymentMethod paymentMethod, PatientEncounter patientEncounter, double value, PaymentMethodData pmd) {
        BilledBill bill = new BilledBill();
        bill.setPatientEncounter(patientEncounter);
        bill.setPaymentMethod(paymentMethod);
        bill.setTotal(value);

        if (errorCheck(bill, pmd)) {
            return null;
        }

        bill = saveBill(bill, pmd);

        if (bill == null) {
            return null;
        }

        saveBillItem(bill);

        getBillBeanController().updateInwardDipositList(bill.getPatientEncounter(), bill);

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            getInwardBeanController().updateFinalFill(bill.getPatientEncounter());

            if (bill.getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBeanController().updateCreditDetail(bill.getPatientEncounter(), bill.getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        return getBillFacade().find(bill.getId());
    }

    private BilledBill saveBill(BilledBill b, PaymentMethodData pmd) {
        getBillBeanController().setPaymentMethodData(b, b.getPaymentMethod(), pmd);
        Bill temp = findLastPaymentBill();
//        //// // System.out.println("temp = " + temp);
        if (temp == null) {
            return null;
        }
        b.setBillType(BillType.InwardPaymentBill);
        if (temp.getInstitution() != null) {
            b.setInsId(getBillNumberGenerator().institutionBillNumberGenerator(temp.getInstitution(), b.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));
        }
        if (temp.getDepartment() != null) {
            b.setDeptId(getBillNumberGenerator().institutionBillNumberGenerator(temp.getDepartment(), b.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));
        }
        b.setInstitution(temp.getInstitution());
        b.setDepartment(temp.getDepartment());

        b.setBillDate(new Date());
        b.setBillTime(new Date());

        double dbl = Math.abs(b.getTotal());
        b.setTotal(dbl);
        b.setNetTotal(dbl);
        b.setCreatedAt(new Date());
        b.setComments("Online Payment");

        if (b.getId() == null) {
            getBilledBillFacade().create(b);
        }

        return b;

    }

    private void saveBillItem(Bill b) {
        BillItem temBi = new BillItem();
        temBi.setBill(b);
        temBi.setGrossValue(b.getTotal());
        temBi.setNetValue(b.getTotal());
        temBi.setCreatedAt(new Date());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }

    }

    private boolean errorCheck(Bill bill, PaymentMethodData pmd) {
        if (bill.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (bill.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(bill.getPaymentMethod(), pmd)) {
            return true;
        }

        return false;

    }

    private PatientEncounter fetchPatientEncounter(String bht_no) {
        String sql;
        HashMap m = new HashMap();

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo=:bht ";
        m.put("class", Admission.class);
        m.put("bht", bht_no);
        PatientEncounter temp = getPatientEncounterFacade().findFirstByJpql(sql, m);
//        //// // System.out.println("temp = " + temp);
        return temp;
    }

    private Institution fetchBank(Long id) {
//        //// // System.out.println("id = " + id);
        String sql;
        HashMap m = new HashMap();
        sql = "SELECT i FROM Institution i where i.retired=false "
                + " and i.id=" + id;
        Institution bank = getInstitutionFacade().findFirstByJpql(sql);
//        //// // System.out.println("bank = " + bank);
        return bank;
    }

    private Bill findLastPaymentBill() {
        String sql;
        Map m = new HashMap();
        sql = "SELECT b FROM Bill b where b.billType=:bTp "
                + " and b.retired=false "
                + " and type(b)=:class "
                + " order by b.id desc ";

        m.put("bTp", BillType.InwardPaymentBill);
        m.put("class", BilledBill.class);

        Bill b = getBillFacade().findFirstByJpql(sql, m);

//        //// // System.out.println("b = " + b);

        return b;
    }

    public double fetchCreditPaymentTotal(PatientEncounter pe) {

        String sql;
        Map m = new HashMap();
        sql = "SELECT sum(bi.netValue) FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter=:bhtno";

        m.put("bty", BillType.CashRecieveBill);
        m.put("bhtno", pe);

        double d = getBillItemFacade().findDoubleByJpql(sql, m);

//        //// // System.out.println("d = " + d);

        return d;

    }

    //--Getters and Setters
    public UriInfo getContext() {
        return context;
    }

    public void setContext(UriInfo context) {
        this.context = context;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public InwardBeanController getInwardBeanController() {
        return inwardBeanController;
    }

    public void setInwardBeanController(InwardBeanController inwardBeanController) {
        this.inwardBeanController = inwardBeanController;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

}
