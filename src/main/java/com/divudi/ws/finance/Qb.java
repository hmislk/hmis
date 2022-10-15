/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ws.finance;

import com.divudi.bean.channel.AgentReferenceBookController;
import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.AuthenticateController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.ApiKey;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.SecurityContext;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * REST Web Service
 *
 * @author Archmage-Dushan
 */
@Path("qb")
@RequestScoped
public class Qb {

    @Context
    private UriInfo context;

    @EJB
    private BillSessionFacade billSessionFacade;

    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    @EJB
    private CommonFunctions commonFunctions;

    @Inject
    private CommonController commonController;
    @Inject
    AuthenticateController authenticateController;
    @Inject
    ApiKeyController apiKeyController;

    /**
     * Creates a new instance of Api
     */
    public Qb() {
    }

    private JSONObject errorMessage() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "Parameter name is not recognized.";
        jSONObjectOut.put("message", "Parameter name is not recognized.");
        return jSONObjectOut;
    }

    private JSONObject errorMessageNoData() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "No Data.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidKey() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid key.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidPathParameter() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid path parameter.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject successMessage() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 200);
        jSONObjectOut.put("type", "success");
        return jSONObjectOut;
    }

    private JSONArray billToJSONArray(List<BillFee> billFees) {
        JSONArray array = new JSONArray();
        for (BillFee billFee : billFees) {
            Bill bill = billFee.getBill();
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("id", bill.getId());
            jSONObject.put("bill_id_1", bill.getInsId());
            jSONObject.put("bill_id_2", bill.getDeptId());

            jSONObject.put("bill_date", bill.getBillDate());
            jSONObject.put("bill_time", bill.getBillTime());

            if (bill.getBillType() != null) {
                jSONObject.put("bill_categoty", bill.getBillType().name());
            }

            if (bill.getBillClassType() != null) {
                jSONObject.put("type", bill.getBillClassType().name());
            }

            jSONObject.put("gross_total", bill.getTotal());
            jSONObject.put("discount", bill.getDiscount());
            jSONObject.put("net_total", bill.getNetTotal());

            if (bill.getTax() != null) {
                jSONObject.put("tax", bill.getTax());
            }
            if (bill.getVat() != 0.0) {
                jSONObject.put("vat", bill.getVat());
            }

            if (bill.getHospitalFee() != 0.0) {
                jSONObject.put("hospital_fee", bill.getHospitalFee());
            }
            if (bill.getStaffFee() != 0.0) {
                jSONObject.put("staff_fee", bill.getStaffFee());
            }
            if (bill.getProfessionalFee() != 0.0) {
                jSONObject.put("professional_fee", bill.getProfessionalFee());
            }

            if (bill.getCashBalance() != null) {
                jSONObject.put("cash_balance", bill.getCashBalance());
            }
            if (bill.getCashPaid() != null) {
                jSONObject.put("cash_paid", bill.getCashPaid());
            }

            if (bill.getPaymentMethod() != null) {
                jSONObject.put("payment_method", bill.getPaymentMethod().name());
            }

            if (bill.getPaymentScheme() != null) {
                jSONObject.put("discount_scheme", bill.getPaymentScheme().getName());
            }
            if (bill.getPaymentMethod() == PaymentMethod.Cheque) {
                if (bill.getBank() != null) {
                    jSONObject.put("cheque_bank", bill.getBank().getName());
                }
                jSONObject.put("cheque_number", bill.getChequeRefNo());
                jSONObject.put("cheque_date", CommonFunctions.formatDate(bill.getChequeDate(), null));
            }

            if (bill.getPaymentMethod() == PaymentMethod.Card) {
                if (bill.getBank() != null) {
                    jSONObject.put("card_bank", bill.getBank().getName());
                }
                jSONObject.put("card_number", bill.getCreditCardRefNo());
                jSONObject.put("cheque_date", CommonFunctions.formatDate(bill.getChequeDate(), null));
            }

            if (bill.getCreditCompany() != null) {
                jSONObject.put("credit_company", bill.getCreditCompany().getName());
            }

            if (bill.getPaidAt() != null) {
                jSONObject.put("paid_At", bill.getPaidAt());
            }

            if (bill.getPaidBill() != null) {
                jSONObject.put("paid_bill_id", bill.getPaidBill());
            }

            if (bill.getInstitution() != null) {
                jSONObject.put("institution", bill.getInstitution().getName());
            }
            if (bill.getDepartment() != null) {
                jSONObject.put("department", bill.getDepartment().getName());
            }
            if (bill.getFromInstitution() != null) {
                jSONObject.put("from_institution", bill.getFromInstitution().getName());
            }
            if (bill.getFromDepartment() != null) {
                jSONObject.put("from_department", bill.getFromDepartment().getName());
            }

            if (bill.getToInstitution() != null) {
                jSONObject.put("to_institution", bill.getToInstitution().getName());
            }
            if (bill.getToDepartment() != null) {
                jSONObject.put("to_department", bill.getToDepartment().getName());
            }

            jSONObject.put("created_at", bill.getCreatedAt());
            jSONObject.put("created_user", bill.getCreater().getWebUserPerson().getName());

            if (bill.getPatient() != null) {
                jSONObject.put("patient_id", bill.getPatient().getId());
                jSONObject.put("patient", bill.getPatient().getPerson().getNameWithTitle());
            }
            if (bill.getPerson() != null) {
                jSONObject.put("person_id", bill.getPerson().getNameWithTitle());
                jSONObject.put("person", bill.getPerson());
            }

            if (bill.getApproveAt() != null) {
                jSONObject.put("approved_at", bill.getApproveAt());
            }
            if (bill.getApproveUser() != null) {
                jSONObject.put("approved_user", bill.getApproveUser());
            }
            if (bill.getAppointmentAt() != null) {
                jSONObject.put("appointment_at", bill.getAppointmentAt());
            }

            if (bill.getCategory() != null) {
                jSONObject.put("category", bill.getCategory().getName());
            }
            if (bill.getCatId() != null) {
                jSONObject.put("catId", bill.getCatId());
            }

            if (bill.getCheckeAt() != null) {
                jSONObject.put("checked_at", bill.getCheckeAt());
            }

            if (bill.getCheckedBy() != null) {
                jSONObject.put("checked_by", bill.getCheckedBy().getWebUserPerson().getName());
            }

            if (bill.getCollectingCentre() != null) {
                jSONObject.put("collecting_centre_id", bill.getCollectingCentre().getId());
                jSONObject.put("collecting_centre", bill.getCollectingCentre().getName());
            }

            if (bill.getComments() != null) {
                jSONObject.put("comments", bill.getComments());
            }

            if (bill.getEditedAt() != null) {
                jSONObject.put("last_edite_at", bill.getEditedAt());
            }
            if (bill.getEditor() != null) {
                jSONObject.put("last_edit_by", bill.getEditor());
            }

            if (bill.getFromStaff() != null) {
                jSONObject.put("from_staff_id", bill.getFromStaff().getId());
                jSONObject.put("from_staff", bill.getFromStaff().getName());
            }
            if (bill.getFromWebUser() != null) {
                jSONObject.put("from_user_id", bill.getFromWebUser().getId());
                jSONObject.put("from_user", bill.getFromWebUser().getName());
            }
            if (bill.getToStaff() != null) {
                jSONObject.put("to_staff_id", bill.getToStaff());
                jSONObject.put("to_staff", bill.getToStaff());
            }
            if (bill.getToWebUser() != null) {
                jSONObject.put("to_user_id", bill.getToWebUser().getId());
                jSONObject.put("to_user", bill.getToWebUser().getName());
            }

            if (bill.getGrantTotal() != 0.0) {
                jSONObject.put("grand_total", bill.getGrantTotal());
            }
            if (bill.getGrnNetTotal() != 0.0) {
                jSONObject.put("grn_net_total", bill.getGrnNetTotal());
            }

            if (bill.getInvoiceDate() != null) {
                jSONObject.put("invoice_date", CommonFunctions.formatDate(bill.getInvoiceDate(), null));
            }
            if (bill.getInvoiceNumber() != null) {
                jSONObject.put("invoice_number", bill.getInvoiceNumber());
            }

            if (bill.getMembershipScheme() != null) {
                jSONObject.put("membership_scheme_id", bill.getMembershipScheme().getId());
                jSONObject.put("membership_scheme", bill.getMembershipScheme().getName());
            }

            if (bill.getPaidBill() != null) {
                jSONObject.put("paid_bill_id", bill.getPaidBill().getId());
            }

            if (bill.getQutationNumber() != null) {
                jSONObject.put("qutation_number", bill.getQutationNumber());
            }

            if (bill.getReactivatedBill() != null) {
                jSONObject.put("reactivated_bill_id", bill.getReactivatedBill().getId());
            }

            if (bill.getReferenceBill() != null) {
                jSONObject.put("reference_bill_id", bill.getReferenceBill().getId());
            }

            if (bill.getReferenceInstitution() != null) {
                jSONObject.put("reference_institution_id",
                        bill.getReferenceInstitution().getId());
                jSONObject.put("reference_institution",
                        bill.getReferenceInstitution().getName());
            }

            if (bill.getReferredBy() != null) {
                jSONObject.put("referred_by_id", bill.getReferredBy().getId());
                jSONObject.put("referred_by", bill.getReferredBy().getPerson().getNameWithTitle());
            }
            if (bill.getReferralNumber() != null) {
                jSONObject.put("referral_number", bill.getReferralNumber());
            }

            if (bill.getStaff() != null) {
                jSONObject.put("staff_id", bill.getStaff().getId());
                jSONObject.put("staff", bill.getStaff().getPerson().getName());
            }

            array.put(jSONObject);
        }
        return array;
    }

    private JSONObject pharmBilltoJSONObject(Bill b) {
//        System.out.println("pharmBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        if (b.getPatient() != null && b.getPatient().getPerson() != null) {
            headerJo.put("customerName", b.getPatient().getPerson().getNameWithTitle());
        } else {
            headerJo.put("customerName", "Customer");
        }

        headerJo.put("invoiceDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("invoiceNo", b.getDeptId());
         if(b.getBillType()!=null){
            headerJo.put("invoiceType", b.getBillType().toString());
        }

        JSONArray bija = new JSONArray();
        for (BillItem bi : b.getBillItems()) {
            JSONObject bijo = new JSONObject();
            if (bi.getItem() != null) {
                bijo.put("item", bi.getItem().getName());
            } else {
                bijo.put("item", "item");
            }
            bijo.put("qty", bi.getAbsoluteQty());
            bijo.put("amount", bi.getAbsoluteNetValue());
            bijo.put("bit", "Pharmacy Item");
             bijo.put("itemType", "Service");
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONObject pharmGrnBilltoJSONObject(Bill b) {
//        System.out.println("pharmBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        String supplierName = "Supplier";
        if (b.getFromInstitution() != null) {
            supplierName = b.getFromInstitution().getName();
        }
        if (b.getToInstitution() != null) {
            supplierName += " " + b.getToInstitution().getName();
        }
        headerJo.put("supplier", supplierName);

        headerJo.put("billDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("billNo", b.getDeptId());
        if(b.getBillType()!=null){
            headerJo.put("billType", b.getBillType().toString());
        }

        
        JSONArray bija = new JSONArray();
        for (BillItem bi : b.getBillItems()) {
            JSONObject bijo = new JSONObject();
            if (bi.getItem() != null) {
                bijo.put("item", bi.getItem().getName());
            } else {
                bijo.put("item", "item");
            }
            bijo.put("qty", bi.getQty());
            bijo.put("amount", bi.getNetValue());
            bijo.put("bit", "Pharmacy Item");
             bijo.put("itemType", "Service");
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONObject paymentBilltoJSONObject(Bill b) {
//        System.out.println("pharmBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        String supplierName = "Supplier";
        if (b.getFromInstitution() != null) {
            supplierName = b.getFromInstitution().getName();
        }
        if (b.getToInstitution() != null) {
            supplierName += " " + b.getToInstitution().getName();
        }
        headerJo.put("supplier", supplierName);

        headerJo.put("billDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("billNo", b.getDeptId());
         if(b.getBillType()!=null){
            headerJo.put("billType", b.getBillType().toString());
        }

        JSONArray bija = new JSONArray();
        for (BillItem bi : b.getBillItems()) {
            JSONObject bijo = new JSONObject();
            if (bi.getItem() != null) {
                bijo.put("item", bi.getItem().getName());
            } else {
                bijo.put("item", "item");
            }
            bijo.put("qty", bi.getQty());
            bijo.put("amount", bi.getNetValue());
            bijo.put("bit", "Pharmacy Item");
             bijo.put("itemType", "Service");
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONObject opdBilltoJSONObject(Bill b) {
        //        System.out.println("opdBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        if (b.getPatient() != null & b.getPatient().getPerson() != null) {
            headerJo.put("customerName", b.getPatient().getPerson().getNameWithTitle());
        } else {
            headerJo.put("customerName", "Customer");
        }

        headerJo.put("invoiceDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("invoiceNo", b.getDeptId());
        if(b.getBillType()!=null){
            headerJo.put("invoiceType", b.getBillType().toString());
        }

        JSONArray bija = new JSONArray();
        for (BillFee bi : b.getBillFees()) {
            JSONObject bijo = new JSONObject();
            String feeName = "";
            String itemName = "";
            String docName = "";
            String insName ="";
            String itemTypeName = "Service or Test";
            if (bi.getFee() != null && bi.getFee().getFeeType() != null) {
                feeName = bi.getFee().getFeeType().getLabel();
            }
            if (bi.getBillItem() != null && bi.getBillItem().getItem() != null) {
                itemName = bi.getBillItem().getItem().getName();
            }
            if (bi.getBillItem() != null && bi.getBillItem().getItem() != null
                    && bi.getBillItem().getItem().getItemType() != null) {
                itemTypeName = bi.getBillItem().getItem().getItemType().name();
            }
            
            bijo.put("item", itemName + " - " + feeName);
            bijo.put("qty", 1);
            bijo.put("amount", bi.getAbsoluteFeeValue());
            bijo.put("bit", "Channelling");
            bijo.put("itemType", "Service");
            
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONObject inwardPaymentBilltoJSONObject(Bill b) {
        //        System.out.println("inwardPaymentBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        String ptName = "";

        if (b.getPatient() != null && b.getPatient().getPerson() != null) {
            ptName = b.getPatient().getPerson().getNameWithTitle();
        }

        if (b.getPatientEncounter() != null && b.getPatientEncounter().getPatient() != null && b.getPatientEncounter().getPatient().getPerson() != null) {
            ptName = b.getPatient().getPerson().getNameWithTitle();
        }

        headerJo.put("customerName", ptName);
        headerJo.put("invoiceDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("invoiceNo", b.getDeptId());
         if(b.getBillType()!=null){
            headerJo.put("invoiceType", b.getBillType().toString());
        }

        JSONArray bija = new JSONArray();
        for (BillFee bi : b.getBillFees()) {
            JSONObject bijo = new JSONObject();
            String feeName = "";
            String itemName = "";
            if (bi.getFee() != null && bi.getFee().getFeeType() != null) {
                feeName = bi.getFee().getFeeType().getLabel();
            }
            if (bi.getBillItem() != null && bi.getBillItem().getItem() != null) {
                itemName = bi.getBillItem().getItem().getName();
            }
            bijo.put("item", itemName + " - " + feeName);
            bijo.put("qty", 1);
            bijo.put("amount", bi.getAbsoluteFeeValue());
            bijo.put("bit", "Inward Deposit");
             bijo.put("itemType", "Service");
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONObject channelBilltoJSONObject(Bill b) {
//        System.out.println("channelBilltoJSONObject");
        JSONObject jSONObject = new JSONObject();
        JSONObject headerJo = new JSONObject();
        if (b.getPatient() != null & b.getPatient().getPerson() != null) {
            headerJo.put("customerName", b.getPatient().getPerson().getNameWithTitle());
        } else {
            headerJo.put("customerName", "Customer");
        }

        headerJo.put("invoiceDate", CommonFunctions.formatDate(b.getCreatedAt(), "yyyy-MM-dd"));
        headerJo.put("invoiceNo", b.getDeptId());
         if(b.getBillType()!=null){
            headerJo.put("invoiceType", b.getBillType().toString());
        }

        JSONArray bija = new JSONArray();
        for (BillFee bi : b.getBillFees()) {
            JSONObject bijo = new JSONObject();
            if (bi.getFee() != null) {
                bijo.put("item", bi.getFee().getFeeType());
            } else {
                bijo.put("item", "item");
            }
            bijo.put("qty", 1);
            bijo.put("amount", bi.getAbsoluteFeeValue());
            bijo.put("bit", "Channellnig");
             bijo.put("itemType", "Service");
            bija.put(bijo);
        }
        jSONObject.put("header", headerJo);
        jSONObject.put("grid", bija);
//        System.out.println("jSONObject = " + jSONObject);
        return jSONObject;
    }

    private JSONArray invoiceBillsToJSONArray(List<Bill> bills) {
//        System.out.println("invoiceBillsToJSONArray");
//        System.out.println("bills = " + bills.size());
        JSONArray array = new JSONArray();
        for (Bill bill : bills) {
            if (bill.getBillType() == null) {
                continue;
            }
            JSONObject jSONObject = new JSONObject();
//            System.out.println("bill.getBillType() = " + bill.getBillType());
            switch (bill.getBillType()) {
                case PharmacySale:
                case PharmacyWholeSale:
                    jSONObject = pharmBilltoJSONObject(bill);
                    break;
                case PharmacyGrnBill:
                case PharmacyPurchaseBill:
                case PharmacyReturnWithoutTraising:
                case PharmacyGrnReturn:
                case StoreGrnReturn:
                case StoreGrnBill:
                case StorePurchase:
                case StorePurchaseReturn:
                    jSONObject = pharmGrnBilltoJSONObject(bill);
                    break;
                case ChannelPaid:
                case ChannelCash:
                    jSONObject = channelBilltoJSONObject(bill);
                    break;
                case OpdBill:
                    jSONObject = opdBilltoJSONObject(bill);
                    break;
                case InwardPaymentBill:
                    jSONObject = inwardPaymentBilltoJSONObject(bill);
                    break;
                case ChannelProPayment:
                case PettyCash:
                case PaymentBill:
                    jSONObject = paymentBilltoJSONObject(bill);
                    break;
                default:
                    continue;
            }

            array.put(jSONObject);
        }
//        System.out.println("array = " + array);
        return array;
    }

    private JSONArray billAndBillItemsToJSONArray(List<Bill> bills) {
        JSONArray array = new JSONArray();
        for (Bill bill : bills) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("id", bill.getId());
            jSONObject.put("bill_id_1", bill.getInsId());
            jSONObject.put("bill_id_2", bill.getDeptId());

            jSONObject.put("bill_date", bill.getBillDate());
            jSONObject.put("bill_time", bill.getBillTime());

            JSONArray biArray = new JSONArray();
            for (BillItem bi : bill.getBillItems()) {
                JSONObject joBi = new JSONObject();
                if (bi != null) {
                    if (bi.getItem() != null) {
                        joBi.put("item_id", bi.getItem());
                        joBi.put("item", bi.getItem().getName());
                        if (bi.getItem().getItemType() != null) {
                            joBi.put("item_type", bi.getItem().getItemType());
                        }
                    }

                    if (bi.getBillSession() != null) {
                        joBi.put("BillSession", bi.getBillSession());
                    }

                    if (bi.getDiscount() != 0.0) {
                        joBi.put("Discount", bi.getDiscount());
                    }
                    if (bi.getDiscountRate() != 0.0) {
                        joBi.put("DiscountRate", bi.getDiscountRate());
                    }
                    if (bi.getGrossValue() != 0.0) {
                        joBi.put("GrossValue", bi.getGrossValue());
                    }
                    if (bi.getHospitalFee() != 0.0) {
                        joBi.put("HospitalFee", bi.getHospitalFee());
                    }
                    if (bi.getId() != null) {
                        joBi.put("Id", bi.getId());
                    }
                    if (bi.getInsId() != null) {
                        joBi.put("InsId", bi.getInsId());
                    }
                    if (bi.getInwardChargeType() != null) {
                        joBi.put("InwardChargeType", bi.getInwardChargeType());
                    }
                    if (bi.getItemId() != null) {
                        joBi.put("ItemId", bi.getItemId());
                    }
                    if (bi.getMarginRate() != 0.0) {
                        joBi.put("MarginRate", bi.getMarginRate());
                    }
                    if (bi.getMarginValue() != 0.0) {
                        joBi.put("MarginValue", bi.getMarginValue());
                    }
                    if (bi.getNetRate() != 0.0) {
                        joBi.put("NetRate", bi.getNetRate());
                    }
                    if (bi.getNetValue() != 0.0) {
                        joBi.put("NetValue", bi.getNetValue());
                    }
                    if (bi.getProFees() != null) {
                        joBi.put("ProFees", bi.getProFees());
                    }
                    if (bi.getAbsoluteQty() != null) {
                        joBi.put("Qty", bi.getAbsoluteQty());
                    }
                    if (bi.getRate() != 0.0) {
                        joBi.put("Rate", bi.getRate());
                    }
                    if (bi.getRefunded() != null) {
                        joBi.put("Refunded", bi.getRefunded());
                    }
                    if (bi.getSearialNo() != 0.0) {
                        joBi.put("SearialNo", bi.getSearialNo());
                    }
                    if (bi.getSessionDate() != null) {
                        joBi.put("SessionDate", bi.getSessionDate());
                    }
                    if (bi.getStaffFee() != 0.0) {
                        joBi.put("StaffFee", bi.getStaffFee());
                    }
                    if (bi.getTotalGrnQty() != 0.0) {
                        joBi.put("TotalGrnQty", bi.getTotalGrnQty());
                    }
                    if (bi.getVat() != 0.0) {
                        joBi.put("Vat", bi.getVat());
                    }
                    if (bi.getVatPlusNetValue() != 0.0) {
                        joBi.put("VatPlusNetValue", bi.getVatPlusNetValue());
                    }

                }
                biArray.put(joBi);
            }
            jSONObject.put("bill_items", biArray);

            if (bill.getBillType() != null) {
                jSONObject.put("bill_categoty", bill.getBillType().name());
            }

            if (bill.getBillClassType() != null) {
                jSONObject.put("type", bill.getBillClassType().name());
            }

            jSONObject.put("gross_total", bill.getTotal());
            jSONObject.put("discount", bill.getDiscount());
            jSONObject.put("net_total", bill.getNetTotal());

            if (bill.getTax() != null) {
                jSONObject.put("tax", bill.getTax());
            }
            if (bill.getVat() != 0.0) {
                jSONObject.put("vat", bill.getVat());
            }

            if (bill.getHospitalFee() != 0.0) {
                jSONObject.put("hospital_fee", bill.getHospitalFee());
            }
            if (bill.getStaffFee() != 0.0) {
                jSONObject.put("staff_fee", bill.getStaffFee());
            }
            if (bill.getProfessionalFee() != 0.0) {
                jSONObject.put("professional_fee", bill.getProfessionalFee());
            }

            if (bill.getCashBalance() != null) {
                jSONObject.put("cash_balance", bill.getCashBalance());
            }
            if (bill.getCashPaid() != null) {
                jSONObject.put("cash_paid", bill.getCashPaid());
            }

            if (bill.getPaymentMethod() != null) {
                jSONObject.put("payment_method", bill.getPaymentMethod().name());
            }

            if (bill.getPaymentScheme() != null) {
                jSONObject.put("discount_scheme", bill.getPaymentScheme().getName());
            }

            if (bill.getInstitution() != null) {
                jSONObject.put("institution", bill.getInstitution().getName());
            }
            if (bill.getDepartment() != null) {
                jSONObject.put("department", bill.getDepartment().getName());
            }

            if (bill.getGrantTotal() != 0.0) {
                jSONObject.put("grand_total", bill.getGrantTotal());
            }
            if (bill.getGrnNetTotal() != 0.0) {
                jSONObject.put("grn_net_total", bill.getGrnNetTotal());
            }

            if (bill.getInvoiceDate() != null) {
                jSONObject.put("invoice_date", CommonFunctions.formatDate(bill.getInvoiceDate(), null));
            }
            if (bill.getInvoiceNumber() != null) {
                jSONObject.put("invoice_number", bill.getInvoiceNumber());
            }

            array.put(jSONObject);
        }
        return array;
    }

    private boolean isValidKey(String key) {
        System.out.println("key = " + key);
        if (key == null || key.trim().equals("")) {
            System.out.println("No key given");
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null) {
            System.out.println("No key found");
            return false;
        }
        if (k.getWebUser() == null) {
            System.out.println("No user for the key");
            return false;
        }
        if (k.getWebUser().isRetired()) {
            System.out.println("User Retired");
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            System.out.println("User Inactive");
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
            System.out.println("Key Expired");
            return false;
        }
        return true;
    }

    @GET
    @Path("/invoice/{last_invoice_id}")
    @Produces("application/json")
    public String getInvoice(@Context HttpServletRequest requestContext,
            @PathParam("last_invoice_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacySale);
        billTypes.add(BillType.ChannelPaid);
        billTypes.add(BillType.OpdBill);
        billTypes.add(BillType.InwardPaymentBill);
        billTypes.add(BillType.ChannelCash);
        billTypes.add(BillType.PharmacyWholeSale);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.BilledBill);
        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 1000;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("invoiceList", array);
        jSONObjectOut.put("lastInvoiceId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/invoicereturn/{last_return_invoice_id}")
    @Produces("application/json")
    public String getInvoicereturn(@Context HttpServletRequest requestContext,
            @PathParam("last_return_invoice_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacySale);
        billTypes.add(BillType.ChannelPaid);
        billTypes.add(BillType.OpdBill);
        billTypes.add(BillType.InwardPaymentBill);
        billTypes.add(BillType.ChannelCash);
        billTypes.add(BillType.PharmacyWholeSale);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.RefundBill);
        billClassTypes.add(BillClassType.CancelledBill);

        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 1000;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("invoiceReturnList", array);
        jSONObjectOut.put("lastReturnInvoiceId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/grn/{last_grn_id}")
    @Produces("application/json")
    public String getGrn(@Context HttpServletRequest requestContext,
            @PathParam("last_grn_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacyGrnBill);
        billTypes.add(BillType.PharmacyPurchaseBill);
        billTypes.add(BillType.PharmacyGrnReturn);
        billTypes.add(BillType.PharmacyReturnWithoutTraising);

        billTypes.add(BillType.StoreGrnBill);
        billTypes.add(BillType.StoreGrnReturn);
        billTypes.add(BillType.StorePurchaseReturn);
        billTypes.add(BillType.StorePurchase);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.BilledBill);
        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 1000;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("grnList", array);
        jSONObjectOut.put("lastgrnId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/grnreturn/{last_return_grn_id}")
    @Produces("application/json")
    public String getGrnReturn(@Context HttpServletRequest requestContext,
            @PathParam("last_return_grn_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacyGrnBill);
        billTypes.add(BillType.PharmacyPurchaseBill);
        billTypes.add(BillType.PharmacyGrnReturn);
        billTypes.add(BillType.PharmacyReturnWithoutTraising);

        billTypes.add(BillType.StoreGrnBill);
        billTypes.add(BillType.StoreGrnReturn);
        billTypes.add(BillType.StorePurchaseReturn);
        billTypes.add(BillType.StorePurchase);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.RefundBill);
        billClassTypes.add(BillClassType.CancelledBill);

        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 1000;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("grnReturnList", array);
        jSONObjectOut.put("lastReturnGrnId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/payment/{last_payment_id}")
    @Produces("application/json")
    public String getPayment(@Context HttpServletRequest requestContext,
            @PathParam("last_payment_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PaymentBill);
        billTypes.add(BillType.ChannelProPayment);
        billTypes.add(BillType.PettyCash);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.BilledBill);
        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 100;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("paymentList", array);
        jSONObjectOut.put("lastgrnId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/paymentreturn/{last_return_payment_id}")
    @Produces("application/json")
    public String getPaymentReturn(@Context HttpServletRequest requestContext,
            @PathParam("last_return_payment_id") String strLastIdInRequest) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Long lastIdInRequest;
        try {
            lastIdInRequest = Long.valueOf(strLastIdInRequest);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (lastIdInRequest == null || lastIdInRequest < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PaymentBill);
        billTypes.add(BillType.ChannelProPayment);
        billTypes.add(BillType.PettyCash);

        List<BillClassType> billClassTypes = new ArrayList<>();
        billClassTypes.add(BillClassType.RefundBill);
        billClassTypes.add(BillClassType.CancelledBill);

        /**
         * PharmacySale ChannelPaid OpdBill InwardPaymentBill ChannelCash
         * PharmacyWholeSale
         *
         */
        int maxNo = 100;

        List<Bill> bills = billList(maxNo, billTypes, billClassTypes, lastIdInRequest, null);
        Long lastIdOfCurrentdata = null;
        if (!bills.isEmpty()) {
            Bill tbf = bills.get(bills.size() - 1);
            if (tbf != null) {
                lastIdOfCurrentdata = tbf.getId();
            }
        }

//        System.out.println("bills.size() = " + bills.size());
        array = invoiceBillsToJSONArray(bills);
        jSONObjectOut.put("paymentReturnList", array);
        jSONObjectOut.put("lastReturnGrnId", lastIdOfCurrentdata);
        jSONObjectOut.put("status", successMessage());

        String json = jSONObjectOut.toString();
        return json;
    }

    public List<BillFee> billFeeList(Integer recordCount, List<BillType> bts, Long lastId) {
        List<BillFee> billfees;
        String j;
        Map m = new HashMap();

        j = " select b "
                + " from BillFee b "
                + " where b.retired<>:ret "
                + " and b.bill.retired<>:ret "
                + " and b.bill.createdAt between :fd and :td "
                + " and b.id > :bid ";
        if (bts != null) {
            j += " and b.bill.billType in :bts";
            m.put("bts", bts);
        }
        j = j + " order by b.id ";

        m.put("bid", lastId);
        m.put("ret", true);
        if (recordCount == null || recordCount == 0) {
            billfees = billFacade.findBySQL(j, m, TemporalType.TIMESTAMP);
        } else {
            billfees = billFacade.findBySQL(j, m, TemporalType.TIMESTAMP, recordCount);
        }

        if (billfees == null) {
            billfees = new ArrayList<>();
        }
        return billfees;
    }

    public List<BillItem> billItemList(Integer recordCount, List<BillType> bts, Long fromId, Long toId) {
        List<BillItem> billItems;
        String j;
        Map m = new HashMap();

        j = " select b "
                + " from BillItem b "
                + " where b.retired<>:ret "
                + " and b.bill.retired<>:ret "
                + " and b.bill.createdAt between :fd and :td "
                + " and b.id > :bid ";
        if (toId != null) {
            j += " and b.id < :tid";
            m.put("tid", toId);
        }
        if (bts != null) {
            j += " and b.bill.billType in :bts";
            m.put("bts", bts);
        }
        j = j + " order by b.id ";

        m.put("bid", fromId);
        m.put("ret", true);
        if (recordCount == null || recordCount == 0) {
            billItems = billItemFacade.findBySQL(j, m, TemporalType.TIMESTAMP);
        } else {
            billItems = billItemFacade.findBySQL(j, m, TemporalType.TIMESTAMP, recordCount);
        }

        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public List<Bill> billList(Integer recordCount, List<BillType> bts, List<BillClassType> bcts, Long fromId, Long toId) {
        List<Bill> bills;
        String j;
        Map m = new HashMap();

        j = " select b "
                + " from Bill b "
                + " where b.retired<>:ret "
                + " and b.id > :bid ";
        if (toId != null) {
            j += " and b.id < :tid";
            m.put("tid", toId);
        }
        if (bts != null) {
            j += " and b.billType in :bts";
            m.put("bts", bts);
        }
        if (bcts != null) {
            j += " and b.billClassType in :bct";
            m.put("bct", bcts);
        }
        j = j + " order by b.id ";

        m.put("bid", fromId);
        m.put("ret", true);
        if (recordCount == null || recordCount == 0) {
            bills = billFacade.findBySQL(j, m, TemporalType.TIMESTAMP);
        } else {
            bills = billFacade.findBySQL(j, m, TemporalType.TIMESTAMP, recordCount);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public JSONArray billDetails(long billId) {
        List<BillSession> billObjects;
        JSONArray array = new JSONArray();
        String sql;
        Map m = new HashMap();

        sql = "Select bs From BillSession bs "
                + " where bs.bill.id=:id ";

        m.put("id", billId);
        billObjects = billSessionFacade.findBySQL(sql, m);

//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
//        //System.out.println("billObjects.length = " + billObjects.size());
        Map map = new HashMap();
        if (!billObjects.isEmpty()) {

            try {
                map.put("bill_id", billObjects.get(0).getBill().getId());
                map.put("bill_number", billObjects.get(0).getBill().getInsId());
                map.put("bill_agent", billObjects.get(0).getBill().getCreditCompany().getId());
                map.put("bill_app_no", billObjects.get(0).getBill().getSingleBillSession().getSerialNo());
                map.put("bill_patient_name", billObjects.get(0).getBill().getPatient().getPerson().getName());
                map.put("bill_phone", billObjects.get(0).getBill().getPatient().getPerson().getPhone());
                map.put("bill_doc_name", billObjects.get(0).getBill().getStaff().getPerson().getName());
                map.put("bill_session_date", commonController.getDateFormat(billObjects.get(0).getBill().getSingleBillSession().getSessionDate()));
                map.put("bill_session_start_time", commonController.getTimeFormat24(billObjects.get(0).getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", commonController.getDateTimeFormat24(billObjects.get(0).getBill().getCreatedAt()));
                map.put("bill_total", commonController.getDouble(billObjects.get(0).getBill().getNetTotal()));
                map.put("bill_vat", commonController.getDouble(billObjects.get(0).getBill().getVat()));
                map.put("bill_vat_plus_total", commonController.getDouble(billObjects.get(0).getBill().getNetTotal() + billObjects.get(0).getBill().getVat()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        //System.out.println("map.length = " + map.size());
        array.put(map);

        return array;
    }

    public JSONArray billsDetails(long agentId, Date fromDate, Date toDate, boolean createDate) {
        List<BillSession> billObjects;
        JSONArray array = new JSONArray();
        String sql;
        Map m = new HashMap();

        sql = "Select bs From BillSession bs "
                + " where bs.bill.creditCompany.id=:id ";
        if (createDate) {
            sql += " and bs.bill.createdAt between :fd and :td "
                    + " order by bs.bill.createdAt ";
        } else {
            sql += " and bs.bill.singleBillSession.sessionDate between :fd and :td "
                    + " order by bs.bill.singleBillSession.sessionDate ";
        }

        m.put("id", agentId);
        m.put("fd", commonFunctions.getStartOfDay(fromDate));
        m.put("td", commonFunctions.getEndOfDay(toDate));
        billObjects = billSessionFacade.findBySQL(sql, m, TemporalType.TIMESTAMP);

//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
//        //System.out.println("billObjects.length = " + billObjects.size());
        for (BillSession o : billObjects) {
            try {
                JSONObject map = new JSONObject();
                map.put("bill_id", o.getBill().getId());
                map.put("bill_number", o.getBill().getInsId());
                map.put("bill_agent", o.getBill().getCreditCompany().getId());
                map.put("bill_app_no", o.getBill().getSingleBillSession().getSerialNo());
                map.put("bill_patient_name", o.getBill().getPatient().getPerson().getName());
                map.put("bill_phone", o.getBill().getPatient().getPerson().getPhone());
                map.put("bill_doc_name", o.getBill().getStaff().getPerson().getName());
                map.put("bill_session_date", commonController.getDateFormat(o.getBill().getSingleBillSession().getSessionDate()));
                map.put("bill_session_start_time", commonController.getTimeFormat24(o.getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", commonController.getDateTimeFormat24(o.getBill().getCreatedAt()));
                map.put("bill_total", commonController.getDouble(o.getBill().getNetTotal() + o.getBill().getVat()));
                array.put(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return array;
    }

}
