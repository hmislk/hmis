/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BilledBillFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.MembershipSchemeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * REST Web Service
 *
 * @author Archmage-Dushan
 */
@Path("apiMembership")
@RequestScoped
public class ApiMembership {

    @Context
    private UriInfo context;

    @EJB
    BillFacade billFacade;
    @EJB
    BilledBillFacade billedBillFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    MembershipSchemeFacade membershipSchemeFacade;

    @EJB
    BillNumberGenerator billNumberGenerator;

    @Inject
    SessionController sessionController;
    @Inject
    InstitutionController institutionController;
    @Inject
    CommonController commonController;
    @Inject
    PatientController patientController;
    @Inject
    private BillBeanController billBean;

    /**
     * Creates a new instance of ApiInward
     */
    public ApiMembership() {
    }

    //--Api
    @GET
    @Path("/banks")
    @Produces("application/json")
    public Response getBanks() {
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

        ResponseBuilder response = Response.ok(json);
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");
        response.header("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

        return response.build();
    }

//      {
//"bank_id": 16664599,
//"bank_name": "Amana Bank"
//},
//    /apiMembership/savePatient/Mr/Dushan+Maduranga/Male/1990-10-17/Karapitiya,Galle/0788044212/13456789v
    @GET
    @Path("/savePatient/{title}/{name}/{sex}/{dob}/{address}/{phone}/{nic}")
    @Produces("application/json")
    public String getSavePatient(
            @PathParam("title") String title,
            @PathParam("name") String name,
            @PathParam("sex") String sex,
            @PathParam("dob") String dob,
            @PathParam("address") String address,
            @PathParam("phone") String phone,
            @PathParam("nic") String nic) {
        JSONObject jSONObjectOut = new JSONObject();
        String json;

        String s = fetchErrors(title, name, sex, dob, address, phone, nic);
//        //// // System.out.println("s = " + s);

        if (!"".equals(s)) {
            jSONObjectOut.put("save_patient", s);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "No Data.");
            json = jSONObjectOut.toString();
            return json;
        }

        try {
//            //// // System.out.println("Title.valueOf(title) = " + Title.valueOf(title));
            MembershipScheme ms = getMembershipSchemeFacade().find(3888l);
//            MembershipScheme ms = getMembershipSchemeFacade().find(2670l);
            Person person = new Person();
            person.setTitle(Title.valueOf(title));
            person.setName(name);
            person.setSex(Sex.valueOf(sex));
            person.setDob(getCommonController().getConvertDateTimeFormat24(dob));
            person.setAddress(address);
            person.setPhone(phone.substring(0, 3) + "-" + phone.substring(3, 10));
            person.setNic(nic);
            person.setCreatedAt(new Date());
            person.setRetired(true);
            person.setMembershipScheme(ms);
            getPersonFacade().create(person);

            Patient patient = new Patient();
            patient.setPerson(person);
            if (ms != null) {
                patient.setCode(getPatientController().getCountPatientCode(ms.getCode()));
            } else {
                patient.setCode(getPatientController().getCountPatientCode("LM"));
            }
            patient.setCreatedAt(new Date());
            patient.setRetired(true);
            getPatientFacade().create(patient);

            JSONObject object = new JSONObject();
            object.put("save_patient_id", patient.getId());
            object.put("save_patient_temp_code", patient.getCode());
            object.put("save_patient_title", patient.getPerson().getTitle());
            object.put("save_patient_name", patient.getPerson().getName());
            object.put("save_patient_sex", patient.getPerson().getSex());
            object.put("save_patient_dob", patient.getPerson().getDob());
            object.put("save_patient_address", patient.getPerson().getAddress());
            object.put("save_patient_phone", patient.getPerson().getPhone());
            object.put("save_patient_nic", patient.getPerson().getNic());
            if (patient.isRetired()) {
                object.put("save_patient_status", "De-Active");
            } else {
                object.put("save_patient_status", "Active");
            }

            jSONObjectOut.put("save_patient", object);
            jSONObjectOut.put("error", "0");
            jSONObjectOut.put("error_description", "");

        } catch (Exception e) {
            jSONObjectOut.put("save_patient", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        json = jSONObjectOut.toString();
        return json;

    }

    @GET
    @Path("/patient/{patient_id}")
    @Produces("application/json")
    public String getPatient(@PathParam("patient_id") String patient_id) {
        JSONObject jSONObjectOut = new JSONObject();
        try {
            long l = Long.valueOf(patient_id);
            Patient p = getPatientFacade().find(l);
            if (p != null) {
                JSONObject object = new JSONObject();
                object.put("save_patient_id", p.getId());
                object.put("save_patient_title", p.getPerson().getTitle());
                object.put("save_patient_name", p.getPerson().getName());
                object.put("save_patient_sex", p.getPerson().getSex());
                object.put("save_patient_dob", p.getPerson().getDob());
                object.put("save_patient_address", p.getPerson().getAddress());
                object.put("save_patient_phone", p.getPerson().getPhone());
                object.put("save_patient_nic", p.getPerson().getNic());
                if (p.isRetired()) {
                    object.put("save_patient_temp_code", p.getCode());
                    object.put("save_patient_status", "De-Active");
                } else {
                    object.put("save_patient_code", p.getCode());
                    object.put("save_patient_status", "Active");
                }

                jSONObjectOut.put("patient", object);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("patient", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Patient");
            }

        } catch (Exception e) {
            jSONObjectOut.put("patient", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;

    }

    @GET
    @Path("/serviceValue")
    @Produces("application/json")
    public String getServiceValue() {
        JSONObject jSONObjectOut = new JSONObject();
        try {
            Item i = getItemFacade().find(17860304l);
//            Item i = getItemFacade().find(32768435l);
            if (i != null) {
                BillItem bi = fechserviceFee(i);
                JSONObject object = new JSONObject();
                object.put("save_service_id", i.getId());
                object.put("save_service_name", i.getName());
                object.put("save_service_value", bi.getGrossValue());
                object.put("save_service_vat", bi.getVat());
                object.put("save_service_value_plus_vat", bi.getVatPlusNetValue());

                jSONObjectOut.put("service", object);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("service", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Sevice(Invalid ID)");
            }

        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("patient", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;

    }

    @GET
    @Path("/payForMembership/{patient_id}/{bank_id}/{credit_card_ref}/{memo}")
    @Produces("application/json")
    public String getPayForMembership(
            @PathParam("patient_id") String patient_id,
            @PathParam("bank_id") String bank_id,
            @PathParam("credit_card_ref") String credit_card_ref,
            @PathParam("memo") String memo) {
        JSONObject jSONObjectOut = new JSONObject();
        try {
            fetchErrorsPay(patient_id, bank_id, credit_card_ref, memo);
            long l = Long.valueOf(patient_id);
            Patient p = getPatientFacade().find(l);
            Item i = getItemFacade().find(17860304l);
//            Item i = getItemFacade().find(32768435l);
            Institution bank = fetchBank(Long.parseLong(bank_id));

            PaymentMethodData pmd = new PaymentMethodData();
            pmd.getCreditCard().setNo(credit_card_ref);
            pmd.getCreditCard().setInstitution(bank);

            Bill b = saveBill(i, pmd, p, memo);
            String code = getPatientController().getCountPatientCode("LM");
            p.setCode(code);
            p.setRetired(false);
            getPatientFacade().edit(p);

            p.getPerson().setRetired(false);
            getPersonFacade().edit(p.getPerson());

            if (b != null) {
                JSONObject object = new JSONObject();
                object.put("bill_no_ins", b.getInsId());
                object.put("bill_no_dept", b.getDeptId());
                object.put("bill_patient_id", b.getPatient().getId());
                object.put("bill_patient_name", b.getPatient().getPerson().getName());
                object.put("bill_patient_code", b.getPatient().getCode());
                object.put("bill_amount", b.getNetTotal());
                object.put("bill_amount_vat", b.getVat());
                object.put("bill_amount_with_vat", b.getVatPlusNetTotal());
                object.put("bill_bank", b.getBank().getName());
                object.put("bill_crad_ref_no", b.getCreditCardRefNo());

                jSONObjectOut.put("PayForMembership", object);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("PayForMembership", "");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "Payment Not Done.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("payForMembership", "");
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;
    }

    //--Methords
    private Bill saveBill(Item i, PaymentMethodData pmd, Patient p, String memo) {
        Bill temp = new BilledBill();
        temp.setBillType(BillType.OpdBill);

        Bill lastBill = findLastBill(BillType.OpdBill);

        if (lastBill != null) {
            if (lastBill.getDepartment() != null && lastBill.getInstitution() != null) {
                temp.setDepartment(lastBill.getDepartment());
                temp.setInstitution(lastBill.getInstitution());
                temp.setFromDepartment(lastBill.getDepartment());
                temp.setFromInstitution(lastBill.getDepartment().getInstitution());
            } else {
                temp.setDepartment(i.getDepartment());
                temp.setInstitution(i.getDepartment().getInstitution());
                temp.setFromDepartment(i.getDepartment());
                temp.setFromInstitution(i.getDepartment().getInstitution());
            }
        } else {
            temp.setDepartment(i.getDepartment());
            temp.setInstitution(i.getDepartment().getInstitution());
            temp.setFromDepartment(i.getDepartment());
            temp.setFromInstitution(i.getDepartment().getInstitution());
        }

        temp.setToDepartment(i.getDepartment());
        temp.setToInstitution(i.getDepartment().getInstitution());

//        //// // System.out.println("temp.getDepartment().getName() = " + temp.getDepartment().getName());
//        //// // System.out.println("temp.getInstitution().getName() = " + temp.getInstitution().getName());
//
//        //// // System.out.println("temp.getToDepartment().getName() = " + temp.getToDepartment().getName());
//        //// // System.out.println("temp.getToInstitution().getName() = " + temp.getToInstitution().getName());
//        temp.setStaff(staff);
//        temp.setToStaff(toStaff);
//        temp.setReferredBy(referredBy);
//        temp.setReferenceNumber(referralId);
//        temp.setReferredByInstitution(referredByInstitution);
//        temp.setCreditCompany(creditCompany);
//        temp.setComments(comment);
        getBillBean().setPaymentMethodData(temp, PaymentMethod.OnlineSettlement, pmd);

        temp.setComments(memo);
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(p);

//        temp.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(tmpPatient));
//        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(PaymentMethod.OnlineSettlement);
        temp.setCreatedAt(new Date());
//        temp.setCreater(getSessionController().getLoggedUser());

        String insId = generateBillNumberInsId(temp);

        if (insId.equals("")) {
            return null;
        }
        temp.setInsId(insId);

        //Department ID (DEPT ID)
        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill);
        temp.setDeptId(deptId);

        if (temp.getId() == null) {
            getBillFacade().create(temp);
        } else {
            getBillFacade().edit(temp);
        }
        saveBillItem(temp, i);
        return temp;

    }

    private BillItem saveBillItem(Bill b, Item i) {
        BillItem bi = new BillItem();
        bi.setBill(b);
        bi.setItem(i);
        bi.setCreatedAt(new Date());
        bi.setQty(1.0);
        getBillItemFacade().create(bi);

        b.getBillItems().add(bi);
        getBillFacade().edit(b);

        saveBillFee(bi.getItem(), bi, b);

        return bi;
    }

    private void saveBillFee(Item item, BillItem bi, Bill b) {
        List<BillFee> billFees = new ArrayList<>();
        String sql = "Select f from ItemFee f where f.retired=false and f.item.id = " + item.getId();
        List<ItemFee> itemFee = getItemFeeFacade().findByJpql(sql);
        double val = 0.0;
        double valGros = 0.0;
        double vat = 0.0;
        double vatPlusVal = 0.0;
        for (Fee i : itemFee) {
            BillFee f = new BillFee();
            f.setFee(i);
            f.setFeeValue(i.getFee());
            f.setFeeGrossValue(i.getFee());
            f.setBill(bi.getBill());
            f.setBillItem(bi);
            f.setCreatedAt(new Date());
            if (bi.getItem().getDepartment() != null) {
                f.setDepartment(bi.getItem().getDepartment());
            }

            if (bi.getItem().getInstitution() != null) {
                f.setInstitution(bi.getItem().getInstitution());
            }
            if (i.getStaff() != null) {
                f.setStaff(i.getStaff());
                f.setSpeciality(i.getSpeciality());
            } else {
                f.setStaff(null);
                f.setSpeciality(null);
            }

            if (f.getBillItem().getItem().isVatable()) {
                f.setFeeVat(f.getFeeValue() * f.getBillItem().getItem().getVatPercentage() / 100);
            }

            f.setFeeVatPlusValue(f.getFeeValue() + f.getFeeVat());
            getBillFeeFacade().create(f);
            billFees.add(f);
            val += f.getFeeValue();
            valGros += f.getFeeGrossValue();
            vat += f.getFeeVat();
            vatPlusVal += f.getFeeVatPlusValue();
        }

        bi.setRate(val);
        bi.setGrossValue(valGros);
        bi.setVat(vat);
        bi.setVatPlusNetValue(vatPlusVal);
        getBillItemFacade().edit(bi);

        b.setTotal(val);
        b.setNetTotal(valGros);
        b.setVat(vat);
        b.setVatPlusNetTotal(vatPlusVal);
        getBillFacade().edit(b);

    }

    private BillItem fechserviceFee(Item item) {
        BillItem bi = new BillItem();
        String sql = "Select f from ItemFee f where f.retired=false and f.item.id = " + item.getId();
        List<ItemFee> itemFee = getItemFeeFacade().findByJpql(sql);
        double val = 0.0;
        double valGros = 0.0;
        double vat = 0.0;
        double vatPlusVal = 0.0;
        for (Fee i : itemFee) {
            BillFee f = new BillFee();
            f.setFeeValue(i.getFee());
            f.setFeeGrossValue(i.getFee());

            if (item.isVatable()) {
                f.setFeeVat(f.getFeeValue() * item.getVatPercentage() / 100);
            }

            f.setFeeVatPlusValue(f.getFeeValue() + f.getFeeVat());
            val += f.getFeeValue();
            valGros += f.getFeeGrossValue();
            vat += f.getFeeVat();
            vatPlusVal += f.getFeeVatPlusValue();
        }

        bi.setRate(val);
        bi.setGrossValue(valGros);
        bi.setVat(vat);
        bi.setVatPlusNetValue(vatPlusVal);

        return bi;
    }

    private String generateBillNumberInsId(Bill bill) {

        String insId = getBillNumberGenerator().institutionBillNumberGenerator(bill.getInstitution(), bill.getToDepartment(), bill.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE);

        return insId;
    }

    private List<Institution> fetchBanks() {

        List<Institution> banks = getInstitutionController().completeInstitution(null, InstitutionType.Bank);

        return banks;
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

    private Bill findLastBill(BillType bt) {
        String sql;
        Map m = new HashMap();
        sql = "SELECT b FROM Bill b where b.billType=:bTp "
                + " and b.retired=false "
                + " and type(b)=:class "
                + " order by b.id desc ";

        m.put("bTp", bt);
        m.put("class", BilledBill.class);

        Bill b = getBillFacade().findFirstByJpql(sql, m);

//        //// // System.out.println("b = " + b);
        return b;
    }

    private String fetchErrors(String title, String name, String sex, String dob,
            String address, String phone, String nic) {

        String s = "";
        if (title == null || "".equals(title)) {
            s = "Please Enter title";
            return s;
        }
        if (name == null || "".equals(name)) {
            s = "Please Enter Name";
            return s;
        }
        if (sex == null || "".equals(sex)) {
            s = "Please Enter sex";
            return s;
        }
        if (dob == null || "".equals(dob)) {
            s = "Please Enter dob";
            return s;
        }
        if (address == null || "".equals(address)) {
            s = "Please Enter address";
            return s;
        }
        if (phone == null || "".equals(phone)) {
            s = "Please Enter phone";
            return s;
        }
        if (phone.length() != 10) {
            s = "Please Enter Ten Numbers";
            return s;
        }
        if (nic == null || "".equals(nic)) {
            s = "Please Enter Nic";
            return s;
        }

        return s;

    }

    private String fetchErrorsPay(String patient_id, String bank_id, String credit_card_ref, String memo) {

        String s = "";
        if (patient_id == null || "".equals(patient_id)) {
            s = "Please Enter Patient ID";
            return s;
        }
        long l = Long.valueOf(patient_id);
        Patient p = getPatientFacade().find(l);
        if (p == null) {
            s = "No Patient(Wrong Patient Id)";
            return s;
        }
        if (bank_id == null || "".equals(bank_id)) {
            s = "Please Enter Name";
            return s;
        }
        Institution bank = fetchBank(Long.parseLong(bank_id));
        if (bank == null) {
            s = "No Bank(Wrong Bank Id)";
            return s;
        }
        if (credit_card_ref == null || "".equals(credit_card_ref)) {
            s = "Please Enter Credit Card Reference";
            return s;
        }
        Item i = getItemFacade().find(32768435l);
        if (i == null) {
            s = "No Item(Wrong Item Id)";
            return s;
        }
        if (memo == null || "".equals(memo)) {
            s = "Please Enter memo";
            return s;
        }

        return s;

    }

    //--Getters and Setters
    public UriInfo getContext() {
        return context;
    }

    public void setContext(UriInfo context) {
        this.context = context;
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientController getPatientController() {
        return patientController;
    }

    public void setPatientController(PatientController patientController) {
        this.patientController = patientController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public void setBillNumberGenerator(BillNumberGenerator billNumberGenerator) {
        this.billNumberGenerator = billNumberGenerator;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public MembershipSchemeFacade getMembershipSchemeFacade() {
        return membershipSchemeFacade;
    }

    public void setMembershipSchemeFacade(MembershipSchemeFacade membershipSchemeFacade) {
        this.membershipSchemeFacade = membershipSchemeFacade;
    }

}
