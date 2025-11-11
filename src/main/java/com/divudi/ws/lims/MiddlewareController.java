package com.divudi.ws.lims;

import com.divudi.bean.common.SecurityController;
import com.divudi.core.data.lab.Analyzer;
import java.util.ArrayList;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.WebUserFacade;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.google.gson.Gson;
import org.carecode.lims.libraries.PatientDataBundle;
import org.carecode.lims.libraries.OrderRecord;
import org.carecode.lims.libraries.PatientRecord;
import org.carecode.lims.libraries.QueryRecord;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import org.carecode.lims.libraries.AnalyzerDetails;
import org.carecode.lims.libraries.DataBundle;
import org.carecode.lims.libraries.ResultsRecord;

@Path("/middleware")
public class MiddlewareController {

    @EJB
    WebUserFacade webUserFacade;
    @EJB
    PatientSampleFacade patientSampleFacade;

    @Inject
    LimsMiddlewareController limsMiddlewareController;

    private static final Gson gson = new Gson();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkService() {
        return Response.ok("Middleware service is working").build();
    }

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkServiceTest() {
        return Response.ok("Middleware service is working").build();
    }

    @POST
    @Path("/test_orders_for_sample_requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processTestOrdersForSampleRequests(String jsonInput) {
        try {
            // Deserialize the incoming JSON into QueryRecord
            System.out.println("Deserializing JSON input...");
// Deserialize the incoming JSON into QueryRecord
            // Deserialize the incoming JSON into QueryRecord
            DataBundle dataBundle = gson.fromJson(jsonInput, DataBundle.class);
            String analyzerName = dataBundle.getMiddlewareSettings().getAnalyzerDetails().getAnalyzerName();
            QueryRecord queryRecord = dataBundle.getQueryRecords().get(0);

            if (queryRecord == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid input data").build();
            }

            // Logic to create a PatientDataBundle based on the QueryRecord
            PatientDataBundle pdb = new PatientDataBundle();

            System.out.println("Generating test codes for analyzer...");
            List<String> testNames = limsMiddlewareController.generateTestCodesForAnalyzer(queryRecord.getSampleId(), analyzerName);
            if (testNames == null || testNames.isEmpty()) {
                testNames = Arrays.asList("");
            }

            System.out.println("Fetching patient sample for Sample ID: " + queryRecord.getSampleId());
            PatientSample ptSample = limsMiddlewareController.patientSampleFromId(queryRecord.getSampleId());
            if (ptSample == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Patient sample not found").build();
            }

            OrderRecord or = new OrderRecord(0, queryRecord.getSampleId(), testNames, "S", new Date(), "testInformation");
            pdb.getOrderRecords().add(or);

            System.out.println("Creating PatientRecord...");
            if (ptSample.getPatient() == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Invalid patient data").build();
            }
            if (ptSample.getPatient().getPerson() == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Invalid person data").build();
            }
            String referringDoc = "";
            if (ptSample.getBill() == null || ptSample.getBill().getReferredBy() == null || ptSample.getBill().getReferredBy().getPerson() == null) {

            } else {
                referringDoc = ptSample.getBill().getReferredBy().getPerson().getNameWithTitle();
            }

            System.out.println("AA");

            PatientRecord pr = new PatientRecord(0,
                    extractLastFromBillNumber(ptSample.getBill().getDeptId()),
                    ptSample.getIdStr(),
                    ptSample.getPatient().getPerson().getNameWithTitle(),
                    "", ptSample.getPatient().getPerson().getSex().getLabel(),
                    "", null,
                    ptSample.getPatient().getPerson().getAddress(),
                    ptSample.getPatient().getPerson().getPhone(),
                    referringDoc);
            pdb.setPatientRecord(pr);
            // Convert the PatientDataBundle to JSON and send it in the response
            // Convert the PatientDataBundle to JSON and send it in the response
            System.out.println("BB");
            // Convert the PatientDataBundle to JSON and send it in the response

            String jsonResponse = gson.toJson(pdb);
            return Response.ok(jsonResponse).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
        }
    }
    
    private static String extractLastFromBillNumber(String input) {
        if (input == null || !input.contains("/")) {
            return input; // return as-is if null or no slashes
        }
        int last = input.lastIndexOf('/');
        String lastPart = input.substring(last + 1);
        String billNumber = lastPart.replaceFirst("^0+", "");
        return billNumber;
    }

    @POST
    @Path("/test_results")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receivePatientResults(String jsonInput) {
        System.out.println("receivePatientResults");
        try {
            Gson gson = new Gson();
            DataBundle dataBundle = gson.fromJson(jsonInput, DataBundle.class);
            if (dataBundle != null) {

                WebUser requestSendingUser
                        = findRequestSendingUser(dataBundle.getMiddlewareSettings().getLimsSettings().getUsername(),
                                dataBundle.getMiddlewareSettings().getLimsSettings().getPassword());

                System.out.println("requestSendingUser = " + requestSendingUser);

                if (requestSendingUser == null) {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }

                AnalyzerDetails analyzerDetails = dataBundle.getMiddlewareSettings().getAnalyzerDetails();
                System.out.println("analyzerDetails = " + analyzerDetails);
                Analyzer analyzer = Analyzer.valueOf(analyzerDetails.getAnalyzerName().replace(" ", "_")); // Ensuring enum compatibility
                System.out.println("analyzer = " + analyzer);
                switch (analyzer) {
                    case BioRadD10:
                        return processBioRadD10(dataBundle);


                    case Dimension_Clinical_Chemistry_System:
                        return processDimensionClinicalChemistrySystem(dataBundle);
                    case Gallery_Indiko:
                        return processGalleryIndiko(dataBundle);
                    case Celltac_MEK:
                        return processCelltacMEK(dataBundle);
                    case BA400:
                        return processBA400(dataBundle);
                    case Sysmex_XS_Series:
//                         return processSysmexXSSeries(dataBundle);
                    case MaglumiX3HL7:
                    case MindrayBC5150:
                    case IndikoPlus:
                    case SmartLytePlus:
                    case SwelabLumi:
                    case HumaCount5D:
                    case HumaLyte:
                    case HumaStar600:
                    case XL_200:
                    case AIA_360:
                        System.out.println("going to direct to processResultsCommon");
                        return processResultsCommon(dataBundle);
                    default:
                        throw new IllegalArgumentException("Unsupported analyzer type: " + analyzerDetails.getAnalyzerName());
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\",\"message\":\"Invalid or missing data bundle.\"}")
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"status\":\"error\",\"message\":\"An error occurred while processing results: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public WebUser findRequestSendingUser(String temUserName, String temPassword) {
        if (temUserName == null) {
            return null;
        }
        if (temPassword == null) {
            return null;
        }
        String temSQL;

        temSQL = "SELECT u "
                + " FROM WebUser u "
                + " WHERE u.retired=:ret"
                + " and u.name=:n";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        m.put("ret", false);
        WebUser u = webUserFacade.findFirstByJpql(temSQL, m);

        if (u == null) {
            return null;
        }

        if (SecurityController.matchPassword(temPassword, u.getWebUserPassword())) {
            return u;
        }
        return null;
    }

    public Response processBioRadD10(DataBundle dataBundle) {
        // Process data specific to BioRadD10
        return Response.ok("{\"status\":\"BioRadD10 processed successfully.\"}").build();
    }

    public Response processSysmexXSSeries(DataBundle dataBundle) {
        // Process data specific to Sysmex XS Series
        return Response.ok("{\"status\":\"Sysmex XS Series processed successfully.\"}").build();
    }

    public Response processDimensionClinicalChemistrySystem(DataBundle dataBundle) {
        // Process data specific to Dimension Clinical Chemistry System
        return Response.ok("{\"status\":\"Dimension Clinical Chemistry System processed successfully.\"}").build();
    }

    public Response processGalleryIndiko(DataBundle dataBundle) {
        // Process data specific to Gallery Indiko
        return Response.ok("{\"status\":\"Gallery Indiko processed successfully.\"}").build();
    }

    public Response processCelltacMEK(DataBundle dataBundle) {
        // Process data specific to Celltac MEK
        return Response.ok("{\"status\":\"Celltac MEK processed successfully.\"}").build();
    }

    public Response processBA400(DataBundle dataBundle) {
        // Process data specific to BA400
        return Response.ok("{\"status\":\"BA400 processed successfully.\"}").build();
    }

//    public Response processIndikoPlus(DataBundle dataBundle) {
//        // Process data specific to Indiko Plus
//        return Response.ok("{\"status\":\"Indiko Plus processed successfully.\"}").build();
//    }
    public Response processMaglumiX3HL7(DataBundle dataBundle) {
        // Process data specific to Maglumi X3 HL7
        return Response.ok("{\"status\":\"Maglumi X3 HL7 processed successfully.\"}").build();
    }

    public Response processMindrayBC5150(DataBundle dataBundle) {
        // Process data specific to Mindray BC5150
        return Response.ok("{\"status\":\"Mindray BC5150 processed successfully.\"}").build();
    }

    public Response processResultsCommon(DataBundle dataBundle) {
        System.out.println("processResultsCommon");
        List<ResultsRecord> observationDetails = new ArrayList<>();
        System.out.println("observationDetails = " + observationDetails);

        for (ResultsRecord rr : dataBundle.getResultsRecords()) {
            System.out.println("dataBundle = " + dataBundle);
            String sampleId = rr.getSampleId();
            System.out.println("sampleId = " + sampleId);
            String testStr = rr.getTestCode();
            System.out.println("testStr = " + testStr);

            String result = rr.getResultValueString();

            System.out.println("Initial result = " + result);

            System.out.println("Final result = " + result);

            String unit = rr.getResultUnits();
            String error = "";

            boolean thisOk = limsMiddlewareController.addResultToReport(sampleId, testStr, result, unit, error);

            // Set the result status in the ResultsRecord object
            if (thisOk) {
                rr.setStatus("Success"); // Assuming ResultsRecord has a setStatus method
            } else {
                rr.setStatus("Failure"); // Assuming ResultsRecord has a setStatus method
            }

            // Add the result record to the observation details
            observationDetails.add(rr);
        }

        // Create the response data as a map
        String statusMessage = "Results Plus processed with details.";
        Gson gson = new Gson();
        String jsonResponse = gson.toJson(observationDetails);

        // Return the JSON response
        return Response.ok("{\"status\":\"" + statusMessage + "\", \"details\":" + jsonResponse + "}").build();
    }

    public PatientSample patientSampleFromId(Long id) {
        PatientSample ps = patientSampleFacade.find(id);
        if (ps != null) {
            return ps;
        }
        String j = "Select ps "
                + " from PatientSample ps "
                + " where ps.sampleId=:sid ";
        Map m = new HashMap<>();
        m.put("sid", id);
        return patientSampleFacade.findFirstByJpql(j, m);
    }

}
