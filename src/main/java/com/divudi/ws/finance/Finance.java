/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
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
@Path("finance")
@RequestScoped
public class Finance {

    @Context
    private UriInfo context;

    @EJB
    StaffFacade staffFacade;
    @EJB
    private ItemFeeFacade ItemFeeFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private AgentHistoryFacade agentHistoryFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;

    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private ChannelBean channelBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    FinalVariables finalVariables;

    @Inject
    private BillBeanController billBeanController;
    @Inject
    private CommonController commonController;
    @Inject
    AgentReferenceBookController AgentReferenceBookController;
    @Inject
    AuthenticateController authenticateController;
    @Inject
    ApiKeyController apiKeyController;

    /**
     * Creates a new instance of Api
     */
    public Finance() {
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

    private JSONArray billToJSONArray(List<Bill> bills) {
        JSONArray array = new JSONArray();
        for (Bill bill : bills) {
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
                    if (bi.getQty() != null) {
                        joBi.put("Qty", bi.getQty());
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

    private boolean isUserAuthenticated(String authString) {
        System.out.println("authString = " + authString);
        try {
            byte[] decoded = Base64.decodeBase64(authString);
            String decodedAuth = new String(decoded, "UTF-8") + "\n";

            String[] authParts = decodedAuth.split("\\s+");
            System.out.println("authParts = " + authParts);
            String username = authParts[0];
            System.out.println("username = " + username);
            String password = authParts[1];
            System.out.println("password = " + password);
            return authenticateController.userAuthenticated(username, password);
        } catch (UnsupportedEncodingException ex) {
            System.out.println("ex = " + ex);
            return false;
        }
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
    @Path("/bill")
    @Produces("application/json")
    public String getBill(@Context HttpServletRequest requestContext) {
//        String ipadd = requestContext.getHeader("X-FORWARDED-FOR");
//        if (ipadd == null) {
//            ipadd = requestContext.getRemoteAddr();
//        }

        List<Bill> bills = billList(0, null, null, null);
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (!bills.isEmpty()) {
            array = billToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill/{date}")
    @Produces("application/json")
    public String getBill(@PathParam("date") String dateString, @Context HttpServletRequest requestContext
    ) {
        String fromat = "dd-MM-yyyy";
        Date date = CommonFunctions.parseDate(dateString, fromat);
        Date fromDate = CommonFunctions.getStartOfDay(date);
        Date toDate = CommonFunctions.getEndOfDay(date);
        List<Bill> bills = billList(0, fromDate, toDate, null);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (bills != null && !bills.isEmpty()) {
            array = billToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill/{from}/{to}")
    @Produces("application/json")
    public String getBill(@PathParam("from") String fromString, @PathParam("to") String toString,
            @Context HttpServletRequest requestContext) {
        String format = "dd-MM-yyyy-hh:mm:ss";
        Date fromDate = CommonFunctions.parseDate(fromString, format);
        Date toDate = CommonFunctions.parseDate(toString, format);
        List<Bill> bills = billList(0, fromDate, toDate, null);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (!bills.isEmpty()) {
            array = billToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item")
    @Produces("application/json")
    public String getBillItem(@Context HttpServletRequest requestContext) {
//        String ipadd = requestContext.getHeader("X-FORWARDED-FOR");
//        if (ipadd == null) {
//            ipadd = requestContext.getRemoteAddr();
//        }

        List<Bill> bills = billList(0, null, null, null);
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (!bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item/{date}")
    @Produces("application/json")
    public String getBillItem(@PathParam("date") String dateString, @Context HttpServletRequest requestContext
    ) {
        String fromat = "dd-MM-yyyy";
        Date date = CommonFunctions.parseDate(dateString, fromat);
        Date fromDate = CommonFunctions.getStartOfDay(date);
        Date toDate = CommonFunctions.getEndOfDay(date);
        List<Bill> bills = billList(0, fromDate, toDate, null);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (bills != null && !bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item/{from}/{to}")
    @Produces("application/json")
    public String getBillItem(@PathParam("from") String fromString, @PathParam("to") String toString,
            @Context HttpServletRequest requestContext) {
        String format = "dd-MM-yyyy-hh:mm:ss";
        Date fromDate = CommonFunctions.parseDate(fromString, format);
        Date toDate = CommonFunctions.parseDate(toString, format);
        List<Bill> bills = billList(0, fromDate, toDate, null);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        if (!bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item_cat/{bill_category}")
    @Produces("application/json")
    public String getBillItemByCategory(@Context HttpServletRequest requestContext,
            @PathParam("bill_category") String billCategory) {
//        String ipadd = requestContext.getHeader("X-FORWARDED-FOR");
//        if (ipadd == null) {
//            ipadd = requestContext.getRemoteAddr();
//        }

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        BillType bt;
        try {
            bt = BillType.valueOf(billCategory);
            if (bt == null) {
                jSONObjectOut = errorMessageNotValidPathParameter();
                String json = jSONObjectOut.toString();
                return json;
            }
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<Bill> bills = billList(0, null, null, bt);

        if (!bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item_cat/{date}/{bill_category}")
    @Produces("application/json")
    public String getBillItemByCategory(@PathParam("date") String dateString, @PathParam("bill_category") String billCategory, @Context HttpServletRequest requestContext
    ) {
        String fromat = "dd-MM-yyyy";
        Date date = CommonFunctions.parseDate(dateString, fromat);
        Date fromDate = CommonFunctions.getStartOfDay(date);
        Date toDate = CommonFunctions.getEndOfDay(date);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        BillType bt;
        try {
            bt = BillType.valueOf(billCategory);
            if (bt == null) {
                jSONObjectOut = errorMessageNotValidPathParameter();
                String json = jSONObjectOut.toString();
                return json;
            }
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<Bill> bills = billList(0, fromDate, toDate, bt);

        if (bills != null && !bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/bill_item_cat/{from}/{to}/{bill_category}")
    @Produces("application/json")
    public String getBillItemByCategory(@PathParam("from") String fromString, @PathParam("bill_category") String billCategory, @PathParam("to") String toString,
            @Context HttpServletRequest requestContext
    ) {
        String format = "dd-MM-yyyy-hh:mm:ss";
        Date fromDate = CommonFunctions.parseDate(fromString, format);
        Date toDate = CommonFunctions.parseDate(toString, format);

        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        BillType bt;
        try {
            bt = BillType.valueOf(billCategory);
            if (bt == null) {
                jSONObjectOut = errorMessageNotValidPathParameter();
                String json = jSONObjectOut.toString();
                return json;
            }
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        List<Bill> bills = billList(0, fromDate, toDate, bt);

        if (!bills.isEmpty()) {
            array = billAndBillItemsToJSONArray(bills);
            jSONObjectOut.put("data", array);
            jSONObjectOut.put("status", successMessage());
        } else {
            jSONObjectOut = errorMessageNoData();
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    public List<Bill> billList(Integer recordCount, Date fromDate, Date toDate, BillType bt) {

        List<Bill> bills;
        String j;
        Map m = new HashMap();

        j = " select b "
                + " from Bill b "
                + " where b.retired<>:ret "
                + " and b.createdAt between :fd and :td ";
        if (bt != null) {
            j += " and b.billType=:bt";
            m.put("bt", bt);
        }
        j = j + " order by b.id ";

        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ret", true);
        if (recordCount == null || recordCount == 0) {
            bills = getBillFacade().findBySQL(j, m, TemporalType.TIMESTAMP);
        } else {
            bills = getBillFacade().findBySQL(j, m, TemporalType.TIMESTAMP, recordCount);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public List<BillSession> fillBillSessions(long ses_id) {

        HashMap m = new HashMap();

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                //                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        m.put("bt", bts);
        m.put("class", BilledBill.class);
//        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        m.put("ss", ses_id);

        return getBillSessionFacade().findBySQL(sql, m);

    }

    public List<Object[]> sessionsListObject(String doc_code, Date fromDate, Date toDate) {

        List<Object[]> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select s.id, "
                + " s.sessionDate, "
                + " s.startingTime, "
                + " s.endingTime, "
                + " s.maxNo, "
                + " s.refundable, "
                + " s.duration, "
                + " s.roomNo, "
                + " 0, "
                + " 0,"
                + " s.deactivated "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", commonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m.put("class", ServiceSession.class);

        sessions = getStaffFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

        return sessions;
    }

    public List<ServiceSession> sessionsList(String doc_code, Date fromDate, Date toDate) {

        List<ServiceSession> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select s "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", commonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m.put("class", ServiceSession.class);

        sessions = getServiceSessionFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
//        //System.out.println("sessions.size() = " + sessions.size());
        List<ServiceSession> reList = new ArrayList<>();
        for (ServiceSession session : sessions) {
//            //System.out.println("session.getId() = " + session.getId());
//            //System.out.println("session.getId() = " + session.getStartingTime());
            Calendar date = Calendar.getInstance();
            date.setTime(session.getSessionDate());
//            //System.out.println("date.getTime() = " + date.getTime());
            Calendar time = Calendar.getInstance();
            time.setTime(session.getStartingTime());
//            //System.out.println("time.getTime() = " + time.getTime());
            time.set(Calendar.YEAR, date.get(Calendar.YEAR));
            time.set(Calendar.MONTH, date.get(Calendar.MONTH));
            time.set(Calendar.DATE, date.get(Calendar.DATE));
//            //System.out.println("time.getTime() = " + time.getTime());
            if (time.getTime().before(new Date())) {
                reList.add(session);
            }
        }
//        //System.out.println("reList.size() = " + reList.size());
        sessions.removeAll(reList);
//        //System.out.println("sessions.size() = " + sessions.size());

//        List<Object[]> objects = new ArrayList<>();
//        for (ServiceSession s : sessions) {
//            Object[] ob = null;
//            ob[0] =s.getId();
//            ob[1] =s.getSessionDate();
//            ob[2] =s.getStartingTime();
//            ob[3] =s.getEndingTime();
//            ob[4] =s.getMaxNo();
//            ob[5] =s.isRefundable();
//            ob[6] =s.getDuration();
//            ob[7] =s.getRoomNo();
//            ob[8] =0;
//            ob[9] =0;
//            ob[10] =s.isDeactivated();
//            if (ob!=null) {
//                objects.add(ob);
//            }
//        }
//        //System.out.println("objects.size() = " + objects.size());
        return sessions;
    }

    public JSONArray sessionsDatesList(String doc_code, Date fromDate, Date toDate) {
        JSONArray array = new JSONArray();
        List<ServiceSession> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select distinct(s) "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", commonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m.put("class", ServiceSession.class);

        sessions = getServiceSessionFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        List<ServiceSession> reList = new ArrayList<>();
        for (ServiceSession session : sessions) {
//            //System.out.println("session.getId() = " + session.getId());
//            //System.out.println("session.getId() = " + session.getStartingTime());
            Calendar date = Calendar.getInstance();
            date.setTime(session.getSessionDate());
//            //System.out.println("date.getTime() = " + date.getTime());
            Calendar time = Calendar.getInstance();
            time.setTime(session.getStartingTime());
//            //System.out.println("time.getTime() = " + time.getTime());
            time.set(Calendar.YEAR, date.get(Calendar.YEAR));
            time.set(Calendar.MONTH, date.get(Calendar.MONTH));
            time.set(Calendar.DATE, date.get(Calendar.DATE));
//            //System.out.println("time.getTime() = " + time.getTime());
            if (time.getTime().before(new Date())) {
                reList.add(session);
            }
        }
//        //System.out.println("reList.size() = " + reList.size());
        sessions.removeAll(reList);
//        //System.out.println("sessions.size() = " + sessions.size());

//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
//        //System.out.println("sessions.size() = " + sessions.size());
        Date beforeDate = null;
        for (ServiceSession s : sessions) {
//            //System.out.println("s = " + s.getSessionAt());
//            //System.out.println("beforeDate = " + beforeDate);
            if (beforeDate == null) {
//                System.err.println("add Null");
                Date d = (Date) s.getSessionDate();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                array.put(df.format(d));
                beforeDate = s.getSessionDate();
            } else {
//                //System.out.println("beforeDate.getTime() = " + beforeDate.getTime());
//                //System.out.println("s.getSessionDate().getTime() = " + s.getSessionDate().getTime());
                if (beforeDate.getTime() != s.getSessionDate().getTime()) {
//                    System.err.println("add");
                    Date d = (Date) s.getSessionDate();
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    array.put(df.format(d));
                }
                beforeDate = s.getSessionDate();
            }
        }
//        for (Object s : sessions) {
//            Date d = (Date) s;
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//            array.put(df.format(d));
//        }

        return array;
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
                map.put("bill_session_date", getCommonController().getDateFormat(billObjects.get(0).getBill().getSingleBillSession().getSessionDate()));
                map.put("bill_session_start_time", getCommonController().getTimeFormat24(billObjects.get(0).getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", getCommonController().getDateTimeFormat24(billObjects.get(0).getBill().getCreatedAt()));
                map.put("bill_total", getCommonController().getDouble(billObjects.get(0).getBill().getNetTotal()));
                map.put("bill_vat", getCommonController().getDouble(billObjects.get(0).getBill().getVat()));
                map.put("bill_vat_plus_total", getCommonController().getDouble(billObjects.get(0).getBill().getNetTotal() + billObjects.get(0).getBill().getVat()));
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
                map.put("bill_session_date", getCommonController().getDateFormat(o.getBill().getSingleBillSession().getSessionDate()));
                map.put("bill_session_start_time", getCommonController().getTimeFormat24(o.getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", getCommonController().getDateTimeFormat24(o.getBill().getCreatedAt()));
                map.put("bill_total", getCommonController().getDouble(o.getBill().getNetTotal() + o.getBill().getVat()));
                array.put(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return array;
    }

    //----------------------------------------------------
    double fetchLocalFee(long id, PaymentMethod paymentMethod, boolean forign) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        if (forign) {
            jpql = "Select sum(f.ffee)";
        } else {
            jpql = "Select sum(f.fee)";
        }

        jpql += " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", serviceSessionFacade.find(id).getOriginatingSession().getId());
//        //System.out.println("paymentMethod = " + paymentMethod);
//        //System.out.println("feeTypes = " + feeTypes);
//        //System.out.println("m = " + m);
        Double obj = ItemFeeFacade.findDoubleByJpql(jpql, m);
//        //System.out.println("obj = " + obj);
        if (obj == null) {
            return 0;
        }

        return obj;
    }

    double fetchLocalFeeVat(long id, PaymentMethod paymentMethod, boolean forign) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        if (forign) {
            jpql = "Select sum(f.ffee)";
        } else {
            jpql = "Select sum(f.fee)";
        }

        jpql += " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses "
                + " and f.feeType in :fts ";
        m.put("fts", feeTypes);

        m.put("ses", serviceSessionFacade.find(id).getOriginatingSession().getId());

        Double obj = ItemFeeFacade.findDoubleByJpql(jpql, m);
//        //System.out.println("obj = " + obj);
        if (obj == null) {
            return 0;
        }

        return obj * 0.15;
    }

    String fetchErrors(String name, String phone, String doc, long ses, long agent, String agent_ref, String st_foriegn) {
//    String fetchErrors(String name, String phone, String doc, long ses, long agent, long agent_ref) {
        String s = "";
        if (name == null || "".equals(name)) {
            s = "Please Enter Name";
            return s;
        }
        if (phone == null || "".equals(phone)) {
            s = "Please Enter Phone Number";
            return s;
        }
        if (phone.length() != 10) {
            s = "Please Enter Phone Number Length";
            return s;
        }
        if ("".equals(doc)) {
            s = "Please Enter Docctor";
            return s;
        }
        if ("".equals(ses)) {
            s = "Please Enter Session";
            return s;
        }
        if ("".equals(agent)) {
            s = "Please Enter Agency";
            return s;
        }
        Institution institution = institutionFacade.find(agent);
        if (institution == null) {
            s = "Incorrect Agency Id";
            return s;
        }
        if ("".equals(agent_ref)) {
            s = "Please Enter Agency Reference No";
            return s;
        }
        if (checkAgentRefNo(agent_ref, institution)) {
            s = "This Reference No Already Exists";
            return s;
        }
        if ("".equals(st_foriegn)) {
            s = "Please Enter Foriegner Or Not";
            return s;
        }
        if (!("0".equals(st_foriegn) || "1".equals(st_foriegn))) {
            s = "Please Enter Foriegner Status 0 or 1";
            return s;
        }
//        if (checkAgentRefNo(agent_ref,institution)) {
//            s = "This Reference No Already Exists";
//            return s;
//        }

        return s;
    }

    private Bill saveBilledBill(ServiceSession ss, String name, String phone, String doc, long agent, String agent_ref, boolean foriegn) {
//    private Bill saveBilledBill(ServiceSession ss, String name, String phone, String doc, long agent, long agent_ref) {
        Bill savingBill = createBill(ss, name, phone, agent);
        BillItem savingBillItem = createBillItem(savingBill, agent_ref, ss);
        BillSession savingBillSession = createBillSession(savingBill, savingBillItem, ss);

        List<BillFee> savingBillFees = createBillFee(savingBill, savingBillItem, ss, foriegn);
        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);

        getAmount(ss);

        getBillItemFacade().edit(savingBillItem);

        //Update Bill Session
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);

        //Update Bill
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);

        if (savingBill.getBillType() == BillType.ChannelAgent) {
            updateBallance(savingBill.getCreditCompany(), 0 - (savingBill.getNetTotal() + savingBill.getVat()), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
            savingBill.setBalance(savingBill.getNetTotal());
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    private Bill createBill(ServiceSession ss, String name, String phone, long agent) {
        Bill bill = new BilledBill();
        bill.setStaff(ss.getOriginatingSession().getStaff());
        bill.setAppointmentAt(ss.getSessionDate());
        bill.setTotal(getAmount(ss));
        bill.setNetTotal(getAmount(ss));
        bill.setPaymentMethod(PaymentMethod.Agent);

        Patient p = new Patient();
        p.setPerson(new Person());
        p.getPerson().setName(name);
        p.getPerson().setPhone(phone.substring(0, 3) + "-" + phone.substring(3, 10));
        getPersonFacade().create(p.getPerson());
        bill.setPatient(p);
        getPatientFacade().create(p);

        bill.setBillType(BillType.ChannelAgent);
        Institution institution = institutionFacade.find(agent);
        bill.setCreditCompany(institution);

        String insId = generateBillNumberInsId(bill, ss);

        if (insId.equals("")) {
            return null;
        }
        bill.setInsId(insId);

        String deptId = generateBillNumberDeptId(bill, ss);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(getBillNumberBean().bookingIdGenerator(ss.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getAmount(ss));
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
//        bill.setCreater(null);
        bill.setDepartment(ss.getDepartment());
        bill.setInstitution(ss.getInstitution());

        bill.setToDepartment(ss.getDepartment());
        bill.setToInstitution(ss.getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
//            //System.out.println("paidBill 1= " + bill.getPaidBill());
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }

        return bill;
    }

    private BillItem createBillItem(Bill bill, String agent_ref, ServiceSession ss) {
//    private BillItem createBillItem(Bill bill, long agent_ref, ServiceSession ss) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agent_ref);
//        bi.setAgentRefNo(String.valueOf(agent_ref));
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(null);
        bi.setGrossValue(ss.getOriginatingSession().getTotal());
        bi.setItem(ss);
        bi.setNetRate(ss.getOriginatingSession().getTotal());
        bi.setNetValue(ss.getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(ss.getOriginatingSession().getTotal());
        bi.setSessionDate(ss.getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, ServiceSession ss) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(null);
        bs.setDepartment(ss.getOriginatingSession().getDepartment());
        bs.setInstitution(ss.getOriginatingSession().getInstitution());
        bs.setItem(ss);

        bs.setServiceSession(ss);
        bs.setSessionDate(ss.getSessionDate());
        bs.setSessionTime(ss.getSessionTime());
        bs.setStaff(ss.getStaff());

        int count = getServiceSessionBean().getSessionNumber(ss, ss.getSessionDate(), bs);
//        System.err.println("count" + count);

        bs.setSerialNo(count);

        getBillSessionFacade().create(bs);

        return bs;
    }

    private List<BillFee> createBillFee(Bill bill, BillItem billItem, ServiceSession ss, boolean foriegn) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpTotalNet = 0;
        double tmpTotalVat = 0;
        double tmpTotalVatPlusNet = 0;
        double tmpDiscount = 0;
//        //System.out.println("ss.getOriginatingSession().getItemFees() = " + ss.getOriginatingSession().getItemFees().size());
        for (ItemFee f : ss.getOriginatingSession().getItemFees()) {
            if (bill.getPaymentMethod() != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (bill.getPaymentMethod() != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
//            bf.setCreater(null);
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(bill.getInstitution());
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
//                //System.out.println("bf.getSpeciality() = " + bf.getSpeciality());
                bf.setStaff(f.getStaff());
//                //System.out.println("bf.getStaff() = " + bf.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (foriegn) {
                bf.setFeeValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
            }

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
                bf.setFeeGrossValue(bf.getFeeValue());
                bf.setFeeVat(bf.getFeeValue() * 0.15);
                bf.setFeeVatPlusValue(bf.getFeeValue() * 1.15);
                bf.setFeeDiscount(0.0);
            } else {
                bf.setFeeGrossValue(bf.getFeeValue());
                bf.setFeeVat(0.0);
                bf.setFeeVatPlusValue(bf.getFeeValue());
                bf.setFeeDiscount(0.0);
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(bill.getInstitution());
            }

            tmpTotal += bf.getFeeGrossValue();
            tmpTotalVat += bf.getFeeVat();
            tmpTotalVatPlusNet += bf.getFeeVatPlusValue();
            tmpTotalNet += bf.getFeeValue();
            tmpDiscount += bf.getFeeDiscount();

            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }
        bill.setDiscount(tmpDiscount);
        bill.setNetTotal(tmpTotalNet);
        bill.setTotal(tmpTotal);
        bill.setVat(tmpTotalVat);
        bill.setVatPlusNetTotal(tmpTotalVatPlusNet);
//        //System.out.println("tmpDiscount = " + tmpDiscount);
//        //System.out.println("tmpTotal = " + tmpTotal);
//        //System.out.println("bill.getNetTotal() = " + bill.getNetTotal());
//        //System.out.println("bill.getTotal() = " + bill.getTotal());
        getBillFacade().edit(bill);

        billItem.setDiscount(tmpDiscount);
        billItem.setGrossValue(tmpTotal);
        billItem.setNetValue(tmpTotalNet);
        billItem.setVat(tmpTotalVat);
        billItem.setVatPlusNetValue(tmpTotalVatPlusNet);
//        //System.out.println("billItem.getNetValue() = " + billItem.getNetValue());
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

    public double getAmount(ServiceSession ss) {
        double amount = 0.0;
        amount = ss.getOriginatingSession().getTotalFee();
//        System.err.println("ss.getOriginatingSession().getTotalFee() = " + ss.getOriginatingSession().getTotalFee());

        return amount;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //System.out.println("paymentMethod = " + paymentMethod);
//        //System.out.println("feeTypes = " + feeTypes);
//        //System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //System.out.println("paymentMethod = " + paymentMethod);
//        //System.out.println("feeTypes = " + feeTypes);
//        //System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    private String generateBillNumberInsId(Bill bill, ServiceSession ss) {
        String suffix = ss.getInstitution().getInstitutionCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String insId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
        }

//        //System.out.println("billClassType = " + billClassType);
//        //System.out.println("insId = " + insId);
        return insId;
    }

    private String generateBillNumberDeptId(Bill bill, ServiceSession ss) {
        String suffix = ss.getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
        }

//        //System.out.println("billClassType = " + billClassType);
//        //System.out.println("deptId = " + deptId);
        return deptId;
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession, String refNo) {
//        //System.out.println("updating agency balance");
//        //System.out.println("ins.getName() = " + ins.getName());
//        //System.out.println("ins.getBallance() before " + ins.getBallance());
//        //System.out.println("transactionValue = " + transactionValue);
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
//        agentHistory.setCreater(null);
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(billSession);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNo(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public List<Object[]> specilityList() {

        List<Object[]> specilities = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = " select c.id,c.name "
                + " from DoctorSpeciality c "
                + " where c.retired=false "
                + " order by c.name";

        specilities = getStaffFacade().findAggregates(sql);

//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
//        //System.out.println("consultants.size() = " + specilities.size());
        return specilities;
    }

    private boolean checkAgentRefNo(long agent_ref, Institution institution) {
        if (getAgentReferenceBookController().checkAgentReferenceNumberAlredyExsist(Long.toString(agent_ref), institution, BillType.ChannelAgent, PaymentMethod.Agent)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAgentRefNo(String agent_ref, Institution institution) {
        if (getAgentReferenceBookController().checkAgentReferenceNumberAlredyExsist(agent_ref, institution, BillType.ChannelAgent, PaymentMethod.Agent)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * PUT method for updating or creating an instance of Api
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public UriInfo getContext() {
        return context;
    }

    public void setContext(UriInfo context) {
        this.context = context;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
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

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public AgentReferenceBookController getAgentReferenceBookController() {
        return AgentReferenceBookController;
    }

    public void setAgentReferenceBookController(AgentReferenceBookController AgentReferenceBookController) {
        this.AgentReferenceBookController = AgentReferenceBookController;
    }

}
