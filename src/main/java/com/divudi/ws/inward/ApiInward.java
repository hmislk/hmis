/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.service.PaymentService;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
    @EJB
    PaymentService paymentService;

    @Inject
    ApiKeyController apiKeyController;
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

    // =========================================================================
    // Auth helper
    // =========================================================================

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null) {
            return null;
        }
        if (user.isRetired()) {
            return null;
        }
        if (!user.isActivated()) {
            return null;
        }
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
        return user;
    }

    private String unauthorizedJson() {
        JSONObject out = new JSONObject();
        out.put("error", "1");
        out.put("error_description", "Unauthorized");
        return out.toString();
    }

    // =========================================================================
    // Endpoints
    // =========================================================================

    @GET
    @Path("/admissions")
    @Produces("application/json")
    public String getAddmissions(@HeaderParam("Finance") String key) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();

        try {
            List<PatientEncounter> encounters = fetchAdmissionToPay();
            if (!encounters.isEmpty()) {
                for (PatientEncounter pe : encounters) {
                    JSONObject object = new JSONObject();
                    object.put("patient_encounter_id", pe.getId());
                    object.put("bht_no", pe.getBhtNo());
                    object.put("patient_mrn", pe.getPatient() != null ? pe.getPatient().getPhn() : "");
                    if (pe.getPatient() != null && pe.getPatient().getPerson() != null) {
                        object.put("patient_name", pe.getPatient().getPerson().getName());
                        object.put("patient_home_phone", pe.getPatient().getPerson().getPhone());
                        object.put("patient_mobile", pe.getPatient().getPerson().getMobile());
                        object.put("patient_nic", pe.getPatient().getPerson().getNic());
                    } else {
                        object.put("patient_name", "No Person Details");
                        object.put("patient_home_phone", "No Person Details");
                        object.put("patient_mobile", "No Person Details");
                        object.put("patient_nic", "No Person Details");
                    }
                    if (pe.getFinalBill() != null) {
                        double netTotal = pe.getFinalBill().getNetTotal();
                        double paidAmount = pe.getFinalBill().getPaidAmount() + fetchCreditPaymentTotal(pe);
                        object.put("net_total", netTotal);
                        object.put("paid_amount", paidAmount);
                        object.put("balance", netTotal - paidAmount);
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
        return jSONObjectOut.toString();
    }

    @GET
    @Path("/admissions/byPhone/{phone}")
    @Produces("application/json")
    public String getAdmissionsByPhone(@HeaderParam("Finance") String key, @PathParam("phone") String phone) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();

        try {
            List<PatientEncounter> encounters = fetchAdmissionsByPhone(phone);
            if (!encounters.isEmpty()) {
                for (PatientEncounter pe : encounters) {
                    JSONObject object = new JSONObject();
                    object.put("patient_encounter_id", pe.getId());
                    object.put("bht_no", pe.getBhtNo());
                    object.put("patient_mrn", pe.getPatient() != null ? pe.getPatient().getPhn() : "");
                    if (pe.getPatient() != null && pe.getPatient().getPerson() != null) {
                        object.put("patient_name", pe.getPatient().getPerson().getName());
                    } else {
                        object.put("patient_name", "No Person Details");
                    }
                    if (pe.getFinalBill() != null) {
                        double netTotal = pe.getFinalBill().getNetTotal();
                        double paidAmount = pe.getFinalBill().getPaidAmount() + fetchCreditPaymentTotal(pe);
                        object.put("net_total", netTotal);
                        object.put("paid_amount", paidAmount);
                        object.put("balance", netTotal - paidAmount);
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
        return jSONObjectOut.toString();
    }

    @GET
    @Path("/banks")
    @Produces("application/json")
    public String getBanks(@HeaderParam("Finance") String key) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

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
        return jSONObjectOut.toString();
    }

    @GET
    @Path("/validateAdmission/{bht_no}/{phone}")
    @Produces("application/json")
    public String getAdmissionIsValidate(@HeaderParam("Finance") String key,
            @PathParam("bht_no") String bht_no,
            @PathParam("phone") String phone) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

        JSONObject jSONObjectOut = new JSONObject();
        try {
            if (checkAdmissionIsValied(bht_no, phone)) {
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "Valid");
            } else {
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Not Valid");
            }
        } catch (Exception e) {
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        return jSONObjectOut.toString();
    }

    /**
     * POST /payment — creates payment bill and records to payment table.
     * Body JSON: { "bht_no": "...", "bank_id": 123, "reference_no": "...", "amount": 1000.0, "payment_date": "2024-01-01 10:00:00" }
     */
    @POST
    @Path("/payment")
    @Consumes("application/json")
    @Produces("application/json")
    public String postPayment(@HeaderParam("Finance") String key, String body) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

        JSONObject jSONObjectOut = new JSONObject();
        try {
            JSONObject request = new JSONObject(body);
            String bht_no = request.getString("bht_no");
            long bank_id = request.getLong("bank_id");
            String reference_no = request.getString("reference_no");
            double amount = request.getDouble("amount");
            Date paymentDate = new Date();
            if (request.has("payment_date") && !request.isNull("payment_date")) {
                String dateStr = request.getString("payment_date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sdf.setLenient(false);
                        paymentDate = sdf.parse(dateStr);
                    } catch (Exception e) {
                        jSONObjectOut.put("bill", "");
                        jSONObjectOut.put("error", "1");
                        jSONObjectOut.put("error_description", "Invalid payment_date format. Expected: yyyy-MM-dd HH:mm:ss");
                        return jSONObjectOut.toString();
                    }
                }
            }

            PatientEncounter pe = fetchPatientEncounter(bht_no);
            Institution bank = fetchBank(bank_id);

            if (pe == null) {
                jSONObjectOut.put("bill", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Admission not found.");
                return jSONObjectOut.toString();
            }
            if (bank == null) {
                jSONObjectOut.put("bill", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Bank not found.");
                return jSONObjectOut.toString();
            }
            if (amount <= 0) {
                jSONObjectOut.put("bill", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Amount must be greater than zero.");
                return jSONObjectOut.toString();
            }

            PaymentMethodData pmd = new PaymentMethodData();
            pmd.getOnlineSettlement().setInstitution(bank);
            pmd.getOnlineSettlement().setReferenceNo(reference_no);
            pmd.getOnlineSettlement().setTotalValue(amount);
            pmd.getOnlineSettlement().setDate(paymentDate);

            Bill b = payWithPaymentRecord(PaymentMethod.OnlineSettlement, pe, amount, pmd, authenticatedUser);
            if (b != null) {
                JSONObject object = new JSONObject();
                object.put("bill_no", b.getDeptId());
                object.put("bht_no", b.getPatientEncounter().getBhtNo());
                object.put("amount", b.getNetTotal());
                object.put("reference_no", reference_no);
                jSONObjectOut.put("bill", object);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
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
        return jSONObjectOut.toString();
    }

    /**
     * Legacy GET payment endpoint — kept for backward compatibility.
     */
    @GET
    @Path("/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}")
    @Produces("application/json")
    public String getPayment(@HeaderParam("Finance") String key,
            @PathParam("bht_no") String bht_no, @PathParam("bank_id") String bank_id,
            @PathParam("credit_card_ref") String credit_card_ref, @PathParam("amount") String amount) {
        WebUser authenticatedUser = validateApiKey(key);
        if (authenticatedUser == null) {
            return unauthorizedJson();
        }

        JSONObject jSONObjectOut = new JSONObject();
        try {
            PatientEncounter pe = fetchPatientEncounter(bht_no);
            Institution bank = fetchBank(Long.parseLong(bank_id));
            double parsedAmount = Double.parseDouble(amount);
            if (pe != null && bank != null && parsedAmount > 0) {
                PaymentMethodData pmd = new PaymentMethodData();
                pmd.getOnlineSettlement().setReferenceNo(credit_card_ref);
                pmd.getOnlineSettlement().setInstitution(bank);

                Bill b = pay(PaymentMethod.OnlineSettlement, pe, parsedAmount, pmd, authenticatedUser);
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
        return jSONObjectOut.toString();
    }

    // =========================================================================
    // Private methods
    // =========================================================================

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

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo is not null "
                + " and pe.paymentFinalized=true "
                + " and pe.discharged=true "
                + " and (abs(pe.finalBill.netTotal) - abs(pe.finalBill.paidAmount)) >:d "
                + " order by pe.id desc";

        m.put("d", 0.1);
        List<PatientEncounter> temps = getPatientEncounterFacade().findByJpql(sql, m, 20);
        List<PatientEncounter> removeTemps = new ArrayList<>();
        for (PatientEncounter p : temps) {
            double d = fetchCreditPaymentTotal(p);
            if (p.getFinalBill().getNetTotal() - (p.getFinalBill().getPaidAmount() + d) < 0.1) {
                removeTemps.add(p);
            }
        }
        temps.removeAll(removeTemps);
        suggestions.addAll(temps);

        return suggestions;
    }

    private List<PatientEncounter> fetchAdmissionsByPhone(String phone) {
        List<PatientEncounter> suggestions = new ArrayList<>();
        String sql;
        HashMap m = new HashMap();

        // Not-finalized admissions matching phone
        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo is not null "
                + " and pe.paymentFinalized!=true "
                + " and pe.discharged!=true "
                + " and (pe.patient.person.mobile=:phone or pe.patient.person.phone=:phone"
                + "      or pe.guardian.mobile=:phone or pe.guardian.phone=:phone) "
                + " order by pe.id desc";
        m.put("class", Admission.class);
        m.put("phone", phone);
        suggestions.addAll(getPatientEncounterFacade().findByJpql(sql, m, 20));

        // Finalized with outstanding balance matching phone
        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo is not null "
                + " and pe.paymentFinalized=true "
                + " and pe.discharged=true "
                + " and (abs(pe.finalBill.netTotal) - abs(pe.finalBill.paidAmount)) >:d "
                + " and (pe.patient.person.mobile=:phone or pe.patient.person.phone=:phone"
                + "      or pe.guardian.mobile=:phone or pe.guardian.phone=:phone) "
                + " order by pe.id desc";
        m.put("d", 0.1);
        List<PatientEncounter> temps = getPatientEncounterFacade().findByJpql(sql, m, 20);
        List<PatientEncounter> removeTemps = new ArrayList<>();
        for (PatientEncounter p : temps) {
            double d = fetchCreditPaymentTotal(p);
            if (p.getFinalBill().getNetTotal() - (p.getFinalBill().getPaidAmount() + d) < 0.1) {
                removeTemps.add(p);
            }
        }
        temps.removeAll(removeTemps);
        suggestions.addAll(temps);

        return suggestions;
    }

    private List<Institution> fetchBanks() {
        return getInstitutionController().completeInstitution(null, InstitutionType.Bank);
    }

    public boolean checkAdmissionIsValied(String bhtNo, String phone) {
        String sql;
        HashMap m = new HashMap();

        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo=:bht "
                + " and pe.paymentFinalized!=true "
                + " and pe.discharged!=true "
                + " and (pe.patient.person.mobile=:phone or pe.patient.person.phone=:phone"
                + "      or pe.guardian.mobile=:phone or pe.guardian.phone=:phone) "
                + " order by pe.bhtNo";
        m.put("class", Admission.class);
        m.put("bht", bhtNo);
        m.put("phone", phone);
        List<PatientEncounter> temps = getPatientEncounterFacade().findByJpql(sql, m);
        if (temps.size() > 0) {
            return true;
        }
        sql = "select pe from PatientEncounter pe where pe.retired=false "
                + " and type(pe)=:class "
                + " and pe.bhtNo=:bht "
                + " and pe.paymentFinalized=true "
                + " and pe.discharged=true "
                + " and (abs(pe.finalBill.netTotal) - abs(pe.finalBill.paidAmount)) >:d "
                + " and (pe.patient.person.mobile=:phone or pe.patient.person.phone=:phone"
                + "      or pe.guardian.mobile=:phone or pe.guardian.phone=:phone) "
                + " order by pe.bhtNo ";
        m.put("d", 0.1);
        temps = getPatientEncounterFacade().findByJpql(sql, m);
        if (temps.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * New payment method: creates bill AND records to payment table.
     */
    public Bill payWithPaymentRecord(PaymentMethod paymentMethod, PatientEncounter patientEncounter, double value, PaymentMethodData pmd, WebUser createdBy) {
        BilledBill bill = new BilledBill();
        bill.setPatientEncounter(patientEncounter);
        bill.setPaymentMethod(paymentMethod);
        bill.setTotal(value);

        if (errorCheck(bill, pmd)) {
            return null;
        }

        bill = saveBill(bill, pmd, createdBy);
        if (bill == null) {
            return null;
        }

        saveBillItem(bill, createdBy);

        // Record payment to payment table (before updateInwardDipositList, matching UI order)
        paymentService.createPayment(bill, paymentMethod, pmd, bill.getInstitution(), bill.getDepartment(), createdBy);

        getBillBeanController().updateInwardDipositList(bill.getPatientEncounter(), bill);

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            getInwardBeanController().updateFinalFill(bill.getPatientEncounter());
            if (bill.getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBeanController().updateCreditDetail(bill.getPatientEncounter(), bill.getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        return getBillFacade().find(bill.getId());
    }

    /**
     * Legacy pay method (used by GET /payment endpoint) — no payment table record.
     */
    public Bill pay(PaymentMethod paymentMethod, PatientEncounter patientEncounter, double value, PaymentMethodData pmd, WebUser createdBy) {
        BilledBill bill = new BilledBill();
        bill.setPatientEncounter(patientEncounter);
        bill.setPaymentMethod(paymentMethod);
        bill.setTotal(value);

        if (errorCheck(bill, pmd)) {
            return null;
        }

        bill = saveBill(bill, pmd, createdBy);

        if (bill == null) {
            return null;
        }

        saveBillItem(bill, createdBy);

        getBillBeanController().updateInwardDipositList(bill.getPatientEncounter(), bill);

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            getInwardBeanController().updateFinalFill(bill.getPatientEncounter());

            if (bill.getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBeanController().updateCreditDetail(bill.getPatientEncounter(), bill.getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        return getBillFacade().find(bill.getId());
    }

    private BilledBill saveBill(BilledBill b, PaymentMethodData pmd, WebUser createdBy) {
        getBillBeanController().setPaymentMethodData(b, b.getPaymentMethod(), pmd);
        Bill temp = findLastPaymentBill();
        if (temp == null) {
            return null;
        }
        b.setBillType(BillType.InwardPaymentBill);
        b.setBillTypeAtomic(BillTypeAtomic.INWARD_DEPOSIT);
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
        b.setCreater(createdBy);
        b.setComments("Online Payment");

        if (b.getId() == null) {
            getBilledBillFacade().create(b);
        }

        return b;
    }

    private void saveBillItem(Bill b, WebUser createdBy) {
        BillItem temBi = new BillItem();
        temBi.setBill(b);
        temBi.setGrossValue(b.getTotal());
        temBi.setNetValue(b.getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(createdBy);

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

        if (getPaymentSchemeController().checkPaymentMethodError(bill.getPaymentMethod(), pmd)) {
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
        return temp;
    }

    private Institution fetchBank(Long id) {
        String sql;
        HashMap m = new HashMap();
        sql = "SELECT i FROM Institution i where i.retired=false "
                + " and i.id=:id";
        m.put("id", id);
        Institution bank = getInstitutionFacade().findFirstByJpql(sql, m);
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
        return d;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

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
