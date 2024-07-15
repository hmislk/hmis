/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.pharmacy.StockController;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.lab.PatientSampleComponant;
import com.divudi.facade.BillFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientSampleComponantFacade;
import com.divudi.facade.PatientSampleFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.ItemBarcodeGenerationStrategy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import org.json.JSONArray;
import org.json.JSONObject;
import com.divudi.data.LoginRequest;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillItemFacade;
import java.text.DecimalFormat;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome;

/**
 * REST Web Service
 *
 * @author Dr M H B Ariyaratne
 */
@Path("lims")
@RequestScoped
public class Lims {

    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    PatientSampleFacade patientSampleFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    WebUserFacade webUserFacade;
    @EJB
    ItemFacade itemFacade;
    @Inject
    ItemController itemController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    StockController stockController;

    /**
     * Creates a new instance of LIMS
     */
    public Lims() {
    }

    public OperationOutcome createOperationOutcomeForSuccess(String details) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION).setCode(OperationOutcome.IssueType.INFORMATIONAL).setDetails(new CodeableConcept().setText(details));
        return outcome;
    }

    public OperationOutcome createOperationOutcomeForFailure(String details) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR).setCode(OperationOutcome.IssueType.SECURITY);
        return outcome;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/login/mw")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // Validate the username and password, such as checking them against a database or LDAP directory
        WebUser requestSendingUser = findRequestSendingUser(username, password);

        if (requestSendingUser != null) {
// Return a 200 OK response indicating success
            return Response.ok().entity(createOperationOutcomeForSuccess("Logged Successfully")).build();
        } else {
// Return an OperationOutcome resource indicating failure
            // Return an OperationOutcome resource indicating failure
            OperationOutcome outcome = createOperationOutcomeForFailure("Invalid username or password");
            return Response.status(Response.Status.UNAUTHORIZED).entity(outcome).build();
        }
    }

//    @GET
//    @Path("/samples/login/{username}/{password}")
//    @Produces("application/json")
//    public String checkUserCredentails(
//            @PathParam("username") String username,
//            @PathParam("password") String password) {
//        boolean failed = false;
//        JSONArray array = new JSONArray();
//        JSONObject jSONObjectOut = new JSONObject();
//        String errMsg = "";
//        WebUser requestSendingUser = findRequestSendingUser(username, password);
//        if (requestSendingUser == null) {
//            errMsg += "Username / password mismatch.";
//            failed = true;
//        }
//        if (failed) {
//            JSONObject jSONObject = new JSONObject();
//            jSONObject.put("result", "error");
//            jSONObject.put("error", true);
//            jSONObject.put("errorMessage", errMsg);
//            jSONObject.put("errorCode", 1);
//            return jSONObject.toString();
//        } else {
//            JSONObject jSONObject = new JSONObject();
//            jSONObject.put("result", "success");
//            jSONObject.put("error", false);
//            jSONObject.put("successMessage", "Successfully Logged.");
//            jSONObject.put("successCode", -1);
//            return jSONObject.toString();
//        }
//    }
    @GET
    @Path("/samples/login/{username}/{password}")
    @Produces("application/json")
    public String login(
            @PathParam("username") String username,
            @PathParam("password") String password) {
        boolean failed = false;
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        String errMsg = "";
        WebUser requestSendingUser = findRequestSendingUser(username, password);
        if (requestSendingUser == null) {
            errMsg += "Username / password mismatch.";
            failed = true;
        }
        if (failed) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("result", "error");
            jSONObject.put("error", true);
            jSONObject.put("errorMessage", errMsg);
            jSONObject.put("errorCode", 1);
            return jSONObject.toString();
        } else {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("result", "success");
            jSONObject.put("error", false);
            jSONObject.put("successMessage", "Successfully Logged.");
            jSONObject.put("successCode", -1);
            return jSONObject.toString();
        }
    }

    @GET
    @Path("/samples/{billId}/{username}/{password}")
    @Produces("application/json")
    public String generateSamplesFromBill(
            @PathParam("billId") String billId,
            @PathParam("username") String username,
            @PathParam("password") String password) {
        return processSamplesFromBill(billId, username, password);
    }

    @GET
    @Path("/opticianPoBillBarcodes/{billid}/{username}/{password}")
    @Produces("application/json")
    public String opticianPoBillBarcodes(
            @PathParam("billid") String billId,
            @PathParam("username") String username,
            @PathParam("password") String password) {
        return processItemBarcodesFromBillID(billId, username, password);
    }

    @GET
    @Path("/samples1/{billId}/{username}/{password}")
    @Produces("application/json")
    public String generateSamplesFromBill1(
            @PathParam("billId") String billId,
            @PathParam("username") String username,
            @PathParam("password") String password) {

// Validation
        String validationError = validateInput(billId, username, password);
        if (validationError != null) {
            return constructErrorJson(1, validationError, billId);
        }

        // Fetch necessary data
        WebUser requestSendingUser = findRequestSendingUser(username, password);
        List<Bill> patientBills = getPatientBillsForId(billId, requestSendingUser);
        List<PatientSample> ptSamples = getPatientSamplesForBillId(patientBills, requestSendingUser);
        // Check if necessary data is present
        if (requestSendingUser == null) {
            return constructErrorJson(1, "Username / password mismatch.", billId);
        }
        if (patientBills == null || patientBills.isEmpty()) {
            return constructErrorJson(1, "Bill Not Found. Please reenter.", billId);
        }
        Set<Long> uniqueIds = new HashSet<>();
        JSONArray array = new JSONArray();
        if (ptSamples == null || ptSamples.isEmpty()) {
            for (Bill b : patientBills) {
                JSONObject j = constructPatientSampleJson(b);
                if (j != null) {
                    array.put(j);
                }
            }
        } else {
            for (PatientSample ps : ptSamples) {
                if (uniqueIds.add(ps.getId())) { // Only proceed if the ID is unique
                    JSONObject j = constructPatientSampleJson(ps);
                    if (j != null) {
                        array.put(j);
                    }
                }
            }
        }

        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("Barcodes", array);
        String js = jSONObjectOut.toString();
        return js;
    }

    public String processSamplesFromBill(String billId, WebUser requestSendingUser) {
//        System.out.println("Processing generateSamplesFromBill for User");
//        System.out.println("billId = " + billId);

        List<Bill> patientBills = getPatientBillsForId(billId, requestSendingUser);
//        System.out.println("patientBills = " + patientBills);
        if (patientBills == null || patientBills.isEmpty()) {
            return constructErrorJson(1, "Bill Not Found. Please reenter.", billId);
        }

        List<PatientSample> ptSamples = getPatientSamplesForBillId(patientBills, requestSendingUser);

        JSONArray array = new JSONArray();
        Set<Long> uniqueIds = new HashSet<>();

        if (ptSamples != null && !ptSamples.isEmpty()) {
            for (PatientSample ps : ptSamples) {
                if (uniqueIds.add(ps.getId())) {
                    JSONObject j = constructPatientSampleJson(ps);
                    if (j != null) {
                        array.put(j);
                    }
                }
            }
        } else {
            for (Bill b : patientBills) {
                JSONObject j = constructPatientSampleJson(b);
                if (j != null) {
                    array.put(j);
                }
            }
        }

        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("Barcodes", array);
        return jSONObjectOut.toString();
    }

    public String processBarcodeFromBill(String barcode, WebUser requestSendingUser) {
        JSONArray array = new JSONArray();
        if (barcode == null) {
            return "";
        }

        Bill bill = billBeanController.fetchBill(barcode);
        if (bill == null) {
            return "";
        }

        List<BillItem> billItems = billBeanController.fillBillItems(bill);
        for (BillItem bi : billItems) {
            Item i = bi.getItem();
            if (i == null || i.getItemBarcodeGenerationStrategy() == null) {
                continue;
            }

            switch (i.getItemBarcodeGenerationStrategy()) {
                case BY_INDIVIDUAL_UNIT:
                    JSONArray jArray = constructUnitBarcodesJson(bi, bi.getPharmaceuticalBillItem().getStock().getStartBarcode(), bi.getPharmaceuticalBillItem().getStock().getEndBarcode());
                    if (jArray != null) {
                        for (int index = 0; index < jArray.length(); index++) {
                            array.put(jArray.get(index));
                        }
                    }
                    break;
                case BY_BATCH:
                    JSONObject bcBatch = constructBatchBarcodeJson(bi.getPharmaceuticalBillItem().getStock());
                    if (bcBatch != null) {
                        array.put(bcBatch);
                    }
                    break;
                case BY_ITEM:
                    JSONObject bcItem = constructItemBarcodeJson(bi.getItem());
                    if (bcItem != null) {
                        array.put(bcItem);
                    }
                    break;
                default:
                    break;
            }
        }

        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("Barcodes", array);
        return jSONObjectOut.toString();
    }

    public String processBarcodeFromStock(String barcode, WebUser requestSendingUser) {
        JSONArray array = new JSONArray();
        if (barcode == null) {
            return "";
        }

        Stock stock = stockController.findStockById(barcode);
        if (stock == null) {
            return "";
        }

        Item i = stock.getItemBatch().getItem();
        if (i == null || i.getItemBarcodeGenerationStrategy() == null) {
            return "";
        }

        switch (i.getItemBarcodeGenerationStrategy()) {
            case BY_INDIVIDUAL_UNIT:
                JSONArray jArray = constructUnitBarcodesJson(stock);
                if (jArray != null) {
                    for (int index = 0; index < jArray.length(); index++) {
                        array.put(jArray.get(index));
                    }
                }
                break;
            case BY_BATCH:
                JSONObject bcBatch = constructBatchBarcodeJson(stock);
                if (bcBatch != null) {
                    array.put(bcBatch);
                }
                break;
            case BY_ITEM:
                JSONObject bcItem = constructItemBarcodeJson(i);
                if (bcItem != null) {
                    array.put(bcItem);
                }
                break;
            default:
                break;
        }

        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("Barcodes", array);
        return jSONObjectOut.toString();
    }

    public String processSamplesFromBill(String billId, String username, String password) {

        String validationError = validateInput(billId, username, password);
        if (validationError != null) {
            return constructErrorJson(1, validationError, billId);
        }

        WebUser requestSendingUser = findRequestSendingUser(username, password);
        if (requestSendingUser == null) {
            return constructErrorJson(1, "Username / password mismatch.", billId);
        }

        return processSamplesFromBill(billId, requestSendingUser);
    }

    public String processItemBarcodesFromBillID(String billId, String username, String password) {

        String validationError = validateItemBarcodeInput(billId, username, password);
        if (validationError != null) {
            return constructErrorJson(1, validationError, billId);
        }

        WebUser requestSendingUser = findRequestSendingUser(username, password);
        if (requestSendingUser == null) {
            return constructErrorJson(1, "Username / password mismatch.", billId);
        }

        return processBarcodeFromStock(billId, requestSendingUser);
    }

    public String generateSamplesForInternalUse(String billId, WebUser user) {
        return processSamplesFromBill(billId, user);
    }

    private String validateInput(String billId, String username, String password) {
        if (billId == null || billId.trim().equals("")) {
            return "Bill Number not entered";
        }
        if (username == null || username.trim().equals("")) {
            return "Username not entered";
        }
        if (password == null || password.trim().equals("")) {
            return "Password not entered";
        }
        return null;
    }

    private String validateItemBarcodeInput(String barcode, String username, String password) {
        if (barcode == null || barcode.trim().equals("")) {
            return "Barcode not entered";
        }
        if (username == null || username.trim().equals("")) {
            return "Username not entered";
        }
        if (password == null || password.trim().equals("")) {
            return "Password not entered";
        }
        return null;
    }

    private String constructErrorJson(int errorCode, String errorMessage, String billId) {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("error", true);
        jSONObject.put("errorMessage", errorMessage);
        jSONObject.put("errorCode", errorCode);
        jSONObject.put("ErrorBillId", billId);
        return jSONObject.toString();
    }

    private JSONObject constructPatientSampleJson(PatientSample ps) {
        JSONObject jSONObject = new JSONObject();
        if (ps == null) {
            return null;
        } else {
            Patient patient = ps.getPatient();
            if (patient == null) {
                return null;
            } else {
                Person person = patient.getPerson();
                if (person != null) {
                    jSONObject.put("name", person.getName() != null ? person.getName() : "");
                    jSONObject.put("age", person.getAgeAsString() != null ? person.getAgeAsString() : "");
                    jSONObject.put("sex", person.getSex() != null ? person.getSex().toString() : "");
                }
            }
            jSONObject.put("barcode", ps.getIdStr() != null ? ps.getIdStr() : "");

            Bill bill = ps.getBill();
            if (bill == null) {
                return null;
            } else {
                jSONObject.put("insid", bill.getInsId() != null ? bill.getInsId() : "");
                jSONObject.put("deptid", bill.getDeptId() != null ? bill.getDeptId() : "");
                jSONObject.put("billDate", CommonController.formatDate(bill.getCreatedAt(), "dd MMM yy"));
            }
            jSONObject.put("id", ps.getIdStr() != null ? ps.getIdStr() : "");
        }
        List<Item> tpiics = testComponantsForPatientSample(ps);

        String tbis = "";
        String temTube = "";

        if (tpiics == null || tpiics.isEmpty()) {
            return null;
        } else {
            for (Item i : tpiics) {
                tbis += i.getName() + ", ";
                if (i instanceof Investigation) {
                    Investigation temIx = (Investigation) i;
                    if (temIx.getInvestigationTube() == null) {
                        continue;
                    }
                    temTube = temIx.getInvestigationTube().getName();
                } else {
                    continue;
                }
            }
        }
        jSONObject.put("tube", temTube);
        if (tbis.length() > 3) {
            tbis = tbis.substring(0, tbis.length() - 2);
        }
        jSONObject.put("container", temTube != null ? temTube : "");
        jSONObject.put("tests", tbis != null ? tbis : "");
        return jSONObject;
    }

    private JSONObject constructPatientSampleJson(Bill bill) {
        JSONObject jSONObject = new JSONObject();
        if (bill == null) {
            return null;
        } else {
            Patient patient = bill.getPatient();
            if (patient == null) {
                return null;
            } else {
                Person person = patient.getPerson();
                if (person != null) {
                    jSONObject.put("name", person.getName() != null ? person.getName() : "");
                    jSONObject.put("age", person.getAgeAsString() != null ? person.getAgeAsString() : "");
                    jSONObject.put("sex", person.getSex() != null ? person.getSex().toString() : "");
                }
            }
            jSONObject.put("barcode", bill.getIdStr() != null ? bill.getIdStr() : "");

            jSONObject.put("insid", bill.getInsId() != null ? bill.getInsId() : "");
            jSONObject.put("deptid", bill.getDeptId() != null ? bill.getDeptId() : "");
            jSONObject.put("billDate", CommonController.formatDate(bill.getCreatedAt(), "dd MMM yy"));

            jSONObject.put("id", bill.getIdStr() != null ? bill.getIdStr() : "");
        }

        List<BillItem> bis = findBillItems(bill);

        String tbis = "";
        String temTube = "";
        if (bis == null || bis.isEmpty()) {
            return null;
        } else {
            for (BillItem i : bis) {
                tbis += i.getItem().getName() + ", ";
            }
        }
        jSONObject.put("tube", temTube);
        if (tbis.length() > 3) {
            tbis = tbis.substring(0, tbis.length() - 2);
        }
        tbis += " - " + temTube;
        jSONObject.put("tests", tbis);
        return jSONObject;
    }

    private JSONObject constructItemBarcodeJson(Bill bill) {
        JSONObject jSONObject = new JSONObject();
        if (bill == null) {
            return null;
        } else {
            Patient patient = bill.getPatient();
            if (patient == null) {
                return null;
            } else {
                Person person = patient.getPerson();
                if (person != null) {
                    jSONObject.put("name", person.getName() != null ? person.getName() : "");
                    jSONObject.put("age", person.getAgeAsString() != null ? person.getAgeAsString() : "");
                    jSONObject.put("sex", person.getSex() != null ? person.getSex().toString() : "");
                }
            }
            jSONObject.put("barcode", bill.getIdStr() != null ? bill.getIdStr() : "");

            jSONObject.put("insid", bill.getInsId() != null ? bill.getInsId() : "");
            jSONObject.put("deptid", bill.getDeptId() != null ? bill.getDeptId() : "");
            jSONObject.put("billDate", CommonController.formatDate(bill.getCreatedAt(), "dd MMM yy"));

            jSONObject.put("id", bill.getIdStr() != null ? bill.getIdStr() : "");
        }

        List<BillItem> bis = findBillItems(bill);

        String tbis = "";
        String temTube = "";
        if (bis == null || bis.isEmpty()) {
            return null;
        } else {
            for (BillItem i : bis) {
                tbis += i.getItem().getName() + ", ";
            }
        }
        jSONObject.put("tube", temTube);
        if (tbis.length() > 3) {
            tbis = tbis.substring(0, tbis.length() - 2);
        }
        tbis += " - " + temTube;
        jSONObject.put("tests", tbis);
        return jSONObject;
    }

    private JSONArray constructUnitBarcodesJson(BillItem bi, Long startBarcode, Long endBarcode) {
        System.out.println("constructUnitBarcodesJson - startBarcode: " + startBarcode + ", endBarcode: " + endBarcode);
        JSONArray jsonArray = new JSONArray();

        if (bi == null || startBarcode == null || endBarcode == null || startBarcode > endBarcode) {
            System.out.println("Invalid parameters in constructUnitBarcodesJson");
            return jsonArray;  // Return an empty array if input is invalid
        }

        DecimalFormat rateFormatter = new DecimalFormat("#,##0.00");
        String formattedRate = "";
        try {
            Double rate = bi.getPharmaceuticalBillItem().getRetailRate();
            if (rate != null) {
                formattedRate = rateFormatter.format(rate);
            }
        } catch (Exception e) {
            System.out.println("Error in formatting rate: " + e);
        }

        Long barcode = startBarcode;
        try {
            while (barcode <= endBarcode) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("itemName", bi.getItem().getName() != null ? bi.getItem().getName() : "");
                jsonObject.put("itemCode", bi.getItem().getCode()!= null ? bi.getItem().getCode() : "");
                jsonObject.put("rate", formattedRate);
                jsonObject.put("barcode", barcode);
                jsonArray.put(jsonObject);
                barcode++;
            }
        } catch (Exception e) {
            System.out.println("Error while constructing JSON array: " + e);
        }

        return jsonArray;
    }

    public Long createBarcode(String prefix, Long number) {
        String formattedNumber = String.format("%06d", number);
        String combinedString = prefix + formattedNumber;
        Long barcode = Long.parseLong(combinedString);
        return barcode;
    }

    private JSONArray constructUnitBarcodesJson(Stock stock) {

        String initialPartOfBarcode = stock.getItemBatch().getItem().getBarcode();
        Long startLongSecondPart = stock.getItemBatch().getItem().getLastBarcode() + 1;
        Double qty = stock.getStock();
        Long endLongSecondPart = stock.getItemBatch().getItem().getLastBarcode() + Math.round(qty);
        Long startFullBarcode = createBarcode(initialPartOfBarcode, startLongSecondPart);
        Long endFullBarcode = createBarcode(initialPartOfBarcode, endLongSecondPart);
        stock.setStartBarcode(startFullBarcode);
        stock.setEndBarcode(endFullBarcode);
        stock.getItemBatch().getItem().setLastBarcode(endLongSecondPart);
        itemController.saveSelected(stock.getItemBatch().getItem());
        stockController.saveSilantly(stock);

        JSONArray jsonArray = new JSONArray();

        DecimalFormat rateFormatter = new DecimalFormat("#,##0.00");
        String formattedRate = "";
        try {
            Double rate = stock.getItemBatch().getRetailsaleRate();
            if (rate != null) {
                formattedRate = rateFormatter.format(rate);
            }
        } catch (Exception e) {
            System.out.println("Error in formatting rate: " + e);
        }

        Long barcode = stock.getStartBarcode();
        Long endBarcode = stock.getEndBarcode();
        try {
            while (barcode <= endBarcode) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("itemName", stock.getItemBatch().getItem().getName() != null ? stock.getItemBatch().getItem().getName() : "");
                jsonObject.put("itemCode", stock.getItemBatch().getItem().getCode() != null ? stock.getItemBatch().getItem().getCode() : "");
                jsonObject.put("rate", formattedRate);
                jsonObject.put("barcode", barcode);
                jsonArray.put(jsonObject);
                barcode++;
            }
        } catch (Exception e) {
            System.out.println("Error while constructing JSON array: " + e);
        }

        return jsonArray;
    }

    private JSONObject constructBatchBarcodeJson(Stock stock) {
        if (stock == null || stock.getStartBarcode() == null) {
            return null;  // Return an empty array if input is invalid
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("itemName", stock.getItemBatch().getItem().getName() != null ? stock.getItemBatch().getItem().getName() : "");
        jsonObject.put("itemCode", stock.getItemBatch().getItem().getCode() != null ? stock.getItemBatch().getItem().getCode() : "");
        jsonObject.put("barcode", stock.getStartBarcode());
        return jsonObject;
    }

    private JSONObject constructItemBarcodeJson(Item item) {
        if (item == null) {
            return null;  // Return an empty array if input is invalid
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("itemName", item.getName() != null ? item.getName() : "");
        jsonObject.put("itemCode", item.getCode() != null ? item.getCode() : "");
        jsonObject.put("barcode", item.getBarcode());
        return jsonObject;
    }

    private List<BillItem> findBillItems(Bill b) {
        List<BillItem> bits;
        String j = "Select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret "
                + " and bi.bill=:b";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("b", b);
        return billItemFacade.findByJpql(j, m);
    }

    @GET
    @Path("/middleware/{machine}/{message}/{username}/{password}")
    @Produces("application/json")
    public String requestLimsResponseForAnalyzer(
            @PathParam("machine") String machine,
            @PathParam("message") String message,
            @PathParam("username") String username,
            @PathParam("password") String password) {

        //// // System.out.println("password = " + password);
        //// // System.out.println("username = " + username);
        boolean failed = false;
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        String errMsg = "";
        if (machine == null || machine.trim().equals("")) {
            failed = true;
            errMsg += "Machine not entered";
        }
        if (message == null || message.trim().equals("")) {
            failed = true;
            errMsg += "No Message";
        }

        WebUser requestSendingUser = findRequestSendingUser(username, password);
        if (requestSendingUser == null) {
            errMsg += "Username / password mismatch.";
            failed = true;
        }

        if (failed) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("result", "error");
            jSONObject.put("error", true);
            jSONObject.put("errorMessage", errMsg);
            jSONObject.put("errorCode", 1);
            return jSONObject.toString();
        }

        if (!failed) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("result", "success");
            jSONObject.put("error", false);
            jSONObject.put("successMessage", errMsg);
            jSONObject.put("successCode", 1);
            return jSONObject.toString();
        }

        return null;

    }

    public List<Item> testComponantsForPatientSample(PatientSample ps) {
        if (ps == null) {
            return new ArrayList<>();
        }
        List<Item> ts = new ArrayList<>();
        Map m = new HashMap();
        String j = "select ps.investigationComponant "
                + " from PatientSampleComponant ps "
                + " where ps.patientSample=:pts "
                + " group by ps.investigationComponant";
        m = new HashMap();
        m.put("pts", ps);
        ts = itemFacade.findByJpql(j, m);
        return ts;
    }

    public List<PatientSample> prepareSampleCollectionByBillsForRequestss(List<Bill> bills, WebUser wu) {
        String j = "";
        Map m;
        Map<Long, PatientSample> rPatientSamplesMap = new HashMap<>();

        if (bills == null) {
            return null;
        }

        for (Bill b : bills) {
            m = new HashMap();
            m.put("can", false);
            m.put("bill", b);
            j = "Select pi from PatientInvestigation pi "
                    + " where pi.cancelled=:can "
                    + " and pi.billItem.bill=:bill";
            List<PatientInvestigation> pis = patientInvestigationFacade.findByJpql(j, m);

            if (pis == null) {
                return null;
            }

            for (PatientInvestigation ptix : pis) {

                Investigation ix = ptix.getInvestigation();

                if (ix == null) {
                    continue;
                }

                ptix.setCollected(true);
                ptix.setSampleCollecter(wu);
                ptix.setSampleDepartment(wu.getDepartment());
                ptix.setSampleInstitution(wu.getInstitution());
                ptix.setSampledAt(new Date());
                patientInvestigationFacade.edit(ptix);

                List<InvestigationItem> ixis = getItems(ix);

                if (ixis == null) {
                    continue;
                }

                for (InvestigationItem ixi : ixis) {

                    if (ixi.getIxItemType() == InvestigationItemType.Value) {

                        if (ixi.getTube() == null) {
                            continue;
                        }
                        if (ixi.getSample() == null) {
                            continue;
                        }

                        j = "select ps from PatientSample ps "
                                + " where ps.tube=:tube "
                                + " and ps.sample=:sample "
                                + " and ps.machine=:machine "
                                + " and ps.patient=:pt "
                                + " and ps.bill=:bill ";
//                                + " and ps.collected=:ca
                        m = new HashMap();
                        m.put("tube", ixi.getTube());

                        m.put("sample", ixi.getSample());

                        m.put("machine", ixi.getMachine());

                        m.put("pt", b.getPatient());

                        m.put("bill", b);
//                        m.put("ca", false);
                        if (ix.isHasMoreThanOneComponant()) {
                            j += " and ps.investigationComponant=:sc ";
                            m.put("sc", ixi.getSampleComponent());
                        }

                        PatientSample pts = patientSampleFacade.findFirstByJpql(j, m);
                        if (pts == null) {
                            pts = new PatientSample();

                            pts.setTube(ixi.getTube());
                            pts.setSample(ixi.getSample());
                            if (ix.isHasMoreThanOneComponant()) {
                                pts.setInvestigationComponant(ixi.getSampleComponent());
                            }
                            pts.setMachine(ixi.getMachine());
                            pts.setPatient(b.getPatient());
                            pts.setBill(b);

                            pts.setSampleDepartment(wu.getDepartment());
                            pts.setSampleInstitution(wu.getInstitution());
                            pts.setSampleCollecter(wu);
                            pts.setSampledAt(new Date());
                            pts.setCreatedAt(new Date());
                            pts.setCreater(wu);
                            pts.setCollected(false);
                            pts.setReadyTosentToAnalyzer(false);
                            pts.setSentToAnalyzer(false);
                            patientSampleFacade.create(pts);
                        }
                        rPatientSamplesMap.put(pts.getId(), pts);

                        PatientSampleComponant ptsc;
                        j = "select ps from PatientSampleComponant ps "
                                + " where ps.patientSample=:pts "
                                + " and ps.bill=:bill "
                                + " and ps.patient=:pt "
                                + " and ps.patientInvestigation=:ptix "
                                + " and ps.investigationComponant=:ixc";
                        m = new HashMap();
                        m.put("pts", pts);
                        m.put("bill", b);
                        m.put("pt", b.getPatient());
                        m.put("ptix", ptix);
                        m.put("ixc", ixi.getSampleComponent());
                        m.put("pts", pts);

                        m.put("bill", b);

                        m.put("pt", b.getPatient());

                        m.put("ptix", ptix);

                        m.put("ixc", ixi.getSampleComponent());

                        ptsc = patientSampleComponantFacade.findFirstByJpql(j, m);

                        if (ptsc == null) {
                            ptsc = new PatientSampleComponant();
                            ptsc.setPatientSample(pts);
                            ptsc.setBill(b);
                            ptsc.setPatient(b.getPatient());
                            ptsc.setPatientInvestigation(ptix);
                            ptsc.setInvestigationComponant(ixi.getSampleComponent());
                            ptsc.setCreatedAt(new Date());
                            ptsc.setCreater(wu);
                            patientSampleComponantFacade.create(ptsc);
                        }
                    }
                }
            }

        }

        List<PatientSample> rPatientSamples = new ArrayList<>(rPatientSamplesMap.values());
        return rPatientSamples;
    }

    public List<InvestigationItem> getItems(Investigation ix) {
        List<InvestigationItem> iis;
        if (ix == null) {
            return new ArrayList<>();
        }
        Investigation temIx = ix;
        if (ix.getReportedAs() != null) {
            if (ix.getReportedAs() instanceof Investigation) {
                temIx = (Investigation) ix.getReportedAs();
            }
        }

        if (ix.getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired<>true and i.item=:item order by i.riTop, i.riLeft";
            Map m = new HashMap();
            m.put("item", temIx);

            iis = investigationItemFacade.findByJpql(temSql, m);
        } else {
            iis = new ArrayList<>();
        }
        return iis;
    }

    public List<Bill> getPatientBillsForId(String strBillId, WebUser wu) {
        Long billId = stringToLong(strBillId);
        List<Bill> temBills;
        if (billId != null) {
            temBills = prepareSampleCollectionByBillId(billId);
        } else {
            temBills = prepareSampleCollectionByBillNumber(strBillId);
        }
        return temBills;
    }

    public List<Bill> prepareSampleCollectionByBillId(Long bill) {
        Bill b = billFacade.find(bill);
        if (b == null) {
            return null;
        }
        List<Bill> bs = new ArrayList<>();
        if (b.getBillTypeAtomic() != null) {
            if (b.getBillTypeAtomic() == BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT
                    || b.getBillTypeAtomic() == BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER) {
                bs.addAll(validBillsOfBatchBill(b));
                return bs;
            } else {
                bs.add(b);
                return bs;
            }
        }
        if (b.getBillType() == BillType.OpdBathcBill) {
            bs.addAll(validBillsOfBatchBill(b));
            return bs;
        } else {
            bs.add(b);
            return bs;
        }
    }

    public List<Bill> prepareSampleCollectionByBillNumber(String insId) {
        String j = "Select b from Bill b where b.insId=:id order by b.id desc";
        Map m = new HashMap();
        m.put("id", insId);
        Bill b = billFacade.findFirstByJpql(j, m);
        if (b == null) {
            return null;
        }
        List<Bill> bs = validBillsOfBatchBill(b.getBackwardReferenceBill());
        if (bs == null || bs.isEmpty()) {
//            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    public List<Bill> validBillsOfBatchBill(Bill batchBill) {
        String j = "Select b "
                + " from Bill b "
                + " where b.backwardReferenceBill=:bb "
                + " and b.cancelled=false";
        Map m = new HashMap();
        m.put("bb", batchBill);
        List<Bill> tbs = billFacade.findByJpql(j, m);
        return tbs;
    }

    public List<PatientSample> getPatientSamplesForBillId(List<Bill> temBills, WebUser wu) {
        List<PatientSample> pss = prepareSampleCollectionByBillsForRequestss(temBills, wu);
        return pss;
    }

    public WebUser findRequestSendingUser(String temUserName, String temPassword) {
        if (temUserName == null) {
            return null;
        }
        if (temPassword == null) {
            return null;
        }
        String temSQL;

        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = webUserFacade.findFirstByJpql(temSQL, m);

        //// // System.out.println("temSQL = " + temSQL);
        if (u == null) {
            return null;
        }

        if (SecurityController.matchPassword(temPassword, u.getWebUserPassword())) {
            return u;
        }
        return null;
    }

    public Long stringToLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
