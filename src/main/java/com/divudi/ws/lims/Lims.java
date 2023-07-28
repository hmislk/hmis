/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SecurityController;
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
import com.divudi.facade.util.JsfUtil;
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
    WebUserFacade webUserFacade;
    @EJB
    ItemFacade itemFacade;

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
        System.out.println("login");
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
        if (ptSamples == null || ptSamples.isEmpty()) {
            return constructErrorJson(2, "Error in Sample Generation. Please check investigation settings.", billId);
        }

        // Generate JSON response
        JSONArray array = new JSONArray();
        for (PatientSample ps : ptSamples) {
            array.put(constructPatientSampleJson(ps));
        }
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("Barcodes", array);
        return jSONObjectOut.toString();
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
        jSONObject.put("name", ps.getPatient().getPerson().getName());
        jSONObject.put("age", ps.getPatient().getPerson().getAgeAsString());
        jSONObject.put("sex", ps.getPatient().getPerson().getSex().toString());
        jSONObject.put("barcode", ps.getIdStr());
        jSONObject.put("insid", ps.getBill().getInsId());
        jSONObject.put("deptid", ps.getBill().getDeptId());
        jSONObject.put("billDate", CommonController.formatDate(ps.getBill().getCreatedAt(),"dd MMM yy"));
        jSONObject.put("id", ps.getIdStr());
        List<Item> tpiics = testComponantsForPatientSample(ps);
        String tbis = "";
        String temTube = "";
        for (Item i : tpiics) {
            tbis += i.getName() + ", ";
            if (i instanceof Investigation) {
                Investigation temIx = (Investigation) i;
                if (temIx.getInvestigationTube() != null) {
                    temTube = temIx.getInvestigationTube().getName();
                } else {
                    temTube = "";
                }
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
        String j = "select ps.investigationComponant from PatientSampleComponant ps "
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
        //// // System.out.println("prepareSampleCollectionByBillsForRequestss");
        Set<PatientSample> rPatientSamplesSet = new HashSet<>();

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

            for (PatientInvestigation ptix : pis) {

                //// // System.out.println("ptix = " + ptix);
                Investigation ix = ptix.getInvestigation();

                ptix.setCollected(true);
                ptix.setSampleCollecter(wu);
                ptix.setSampleDepartment(wu.getDepartment());
                ptix.setSampleInstitution(wu.getInstitution());
                ptix.setSampledAt(new Date());
                patientInvestigationFacade.edit(ptix);

                List<InvestigationItem> ixis = getItems(ix);

                for (InvestigationItem ixi : ixis) {

                    if (ixi.getIxItemType() == InvestigationItemType.Value || ixi.getIxItemType() == InvestigationItemType.Template) {
                        j = "select ps from PatientSample ps "
                                + " where ps.tube=:tube "
                                + " and ps.sample=:sample "
                                + " and ps.machine=:machine "
                                + " and ps.patient=:pt "
                                + " and ps.bill=:bill "
                                + " and ps.collected=:ca";
                        m = new HashMap();
                        m.put("tube", ixi.getTube());
                        m.put("sample", ixi.getSample());
                        m.put("machine", ixi.getMachine());
                        m.put("pt", b.getPatient());
                        m.put("bill", b);
                        m.put("ca", false);
                        if (ix.isHasMoreThanOneComponant()) {
                            j += " and ps.investigationComponant=:sc ";
                            m.put("sc", ixi.getSampleComponent());
                        }
                        //// // System.out.println("j = " + j);
                        //// // System.out.println("m = " + m);

                        PatientSample pts = patientSampleFacade.findFirstByJpql(j, m);
                        //// // System.out.println("pts = " + pts);
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
                        rPatientSamplesSet.add(pts);

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
                        //// // System.out.println("j = " + j);
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

        List<PatientSample> rPatientSamples = new ArrayList<>(rPatientSamplesSet);
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
        //// // System.out.println("strBillId = " + strBillId);
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
        //// // System.out.println("prepareSampleCollectionByBillId = ");
        Bill b = billFacade.find(bill);
        List<Bill> bs = validBillsOfBatchBill(b.getBackwardReferenceBill());
        if (bs == null || bs.isEmpty()) {
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
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
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    public List<Bill> validBillsOfBatchBill(Bill batchBill) {
        String j = "Select b from Bill b where b.backwardReferenceBill=:bb and b.cancelled=false";
        Map m = new HashMap();
        m.put("bb", batchBill);
        return billFacade.findByJpql(j, m);
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
