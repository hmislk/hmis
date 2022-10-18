/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

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
        boolean failed = false;
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        String errMsg = "";
        if (billId == null || billId.trim().equals("")) {
            failed = true;
            errMsg += "Bill Number not entered";
        }
        WebUser requestSendingUser = findRequestSendingUser(username, password);
        if (requestSendingUser == null) {
            errMsg += "Username / password mismatch.";
            failed = true;
        }
        List<Bill> patientBills = getPatientBillsForId(billId, requestSendingUser);
        if (patientBills == null || patientBills.isEmpty()) {
            errMsg += "Bill Not Found. Pease reenter}";
            failed = true;
        }

        List<PatientSample> ptSamples = getPatientSamplesForBillId(patientBills, requestSendingUser);

        if (ptSamples == null || ptSamples.isEmpty()) {
            errMsg += "Error in Sample Generation. Pease check investigation settings.}";
            failed = true;
        }

        if (failed) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("error", true);
            jSONObject.put("errorMessage", errMsg);
            jSONObject.put("errorCode", 1);
            jSONObject.put("ErrorBillId", billId);
            return jSONObject.toString();
        }

        String tbis = "";

        errMsg += "|message=";

        if (ptSamples == null || ptSamples.isEmpty()) {

            JSONObject jSONObject = new JSONObject();
            jSONObject.put("error", true);
            jSONObject.put("errorMessage", "Could not generate Samples for the Bill.");
            jSONObject.put("errorCode", 2);
            jSONObject.put("ErrorBillId", billId);

            for (Bill b : patientBills) {
                List<BillItem> tpiics = b.getBillItems();
                tbis = "";
                String temTube = "";
                for (BillItem i : tpiics) {
                    tbis += i.getItem().getName() + ", ";
                    if (i.getItem() instanceof Investigation) {
                        Investigation temIx = (Investigation) i.getItem();
                        temTube = temIx.getInvestigationTube().getName();
                    }
                }
                tbis = tbis.substring(0, tbis.length() - 2);
                tbis += " - " + temTube;
            }
            jSONObject.put("tests", tbis);
            return jSONObject.toString();

        } else {
            for (PatientSample ps : ptSamples) {

                JSONObject jSONObject = new JSONObject();
                jSONObject.put("name", ps.getPatient().getPerson().getName());
                jSONObject.put("barcode", ps.getIdStr());
                jSONObject.put("insid", ps.getBill().getInsId());

                List<Item> tpiics = testComponantsForPatientSample(ps);
                tbis = "";
                String temTube = "";
                for (Item i : tpiics) {
                    tbis += i.getName() + ", ";
                    if (i instanceof Investigation) {
                        Investigation temIx = (Investigation) i;
                        temTube = temIx.getInvestigationTube().getName();
                    }
                }
                if (tbis.length() > 3) {
                    tbis = tbis.substring(0, tbis.length() - 2);
                }

                tbis += " - " + temTube;

                jSONObject.put("tests", tbis);
                array.put(jSONObject);

            }
        }
        jSONObjectOut.put("Barcodes", array);
        String json = jSONObjectOut.toString();
        return json;
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

        ts = itemFacade.findBySQL(j, m);
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
            List<PatientInvestigation> pis = patientInvestigationFacade.findBySQL(j, m);


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

                        PatientSample pts = patientSampleFacade.findFirstBySQL(j, m);
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
                        ptsc = patientSampleComponantFacade.findFirstBySQL(j, m);
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

            iis = investigationItemFacade.findBySQL(temSql, m);
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
        Bill b = billFacade.findFirstBySQL(j, m);
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
        return billFacade.findBySQL(j, m);
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

        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and lower(u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = webUserFacade.findFirstBySQL(temSQL, m);

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
