/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.AuthenticateController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;

import com.divudi.entity.ApiKey;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.facade.BillFacade;
import com.divudi.java.CommonFunctions;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.commons.codec.binary.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;
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


    @EJB
    private BillFacade billFacade;
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
            if (bill.getReferenceNumber() != null) {
                jSONObject.put("referral_number", bill.getReferenceNumber());
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
        try {
            byte[] decoded = Base64.decodeBase64(authString);
            String decodedAuth = new String(decoded, "UTF-8") + "\n";

            String[] authParts = decodedAuth.split("\\s+");
            String username = authParts[0];
            String password = authParts[1];
            return authenticateController.userAuthenticated(username, password);
        } catch (UnsupportedEncodingException ex) {
            return false;
        }
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().equals("")) {
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null) {
            return false;
        }
        if (k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired()) {
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
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
            bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        } else {
            bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP, recordCount);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

}
