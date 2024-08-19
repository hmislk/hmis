package com.divudi.ws.lims;

import com.divudi.bean.common.ConfigOptionApplicationController;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import java.util.List;

@Path("/middleware")
public class MiddlewareController {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

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
            QueryRecord queryRecord = gson.fromJson(jsonInput, QueryRecord.class);

            // Logic to create a PatientDataBundle based on the QueryRecord
            PatientDataBundle pdb = new PatientDataBundle();
            List<String> testNames = Arrays.asList("HDL", "RF2", "GLU");
            OrderRecord or = new OrderRecord(0, queryRecord.getSampleId(), testNames, "S", new Date(), "testInformation");
            pdb.getOrderRecords().add(or);
            PatientRecord pr = new PatientRecord(0, "1010101", "111111", "Buddhika Ariyaratne", "M H B", "Male", "Sinhalese", null, "Galle", "0715812399", "Dr Niluka");
            pdb.setPatientRecord(pr);

            // Convert the PatientDataBundle to JSON and send it in the response
            String jsonResponse = gson.toJson(pdb);
            return Response.ok(jsonResponse).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/test_results")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receivePatientResults(String jsonInput) {
        try {
            // Deserialize the incoming JSON to a PatientDataBundle object
            PatientDataBundle patientDataBundle = gson.fromJson(jsonInput, PatientDataBundle.class);

            // Process the received PatientDataBundle (e.g., store in the database)
            if (patientDataBundle != null) {
                // Example: output details for verification
                System.out.println("Received Patient Data Bundle:");
                System.out.println("Patient ID: " + patientDataBundle.getPatientRecord().getPatientId());
                System.out.println("Order Records: " + patientDataBundle.getOrderRecords().size());
                System.out.println("Results Records: " + patientDataBundle.getResultsRecords().size());
                System.out.println("Query Records: " + patientDataBundle.getQueryRecords().size());
            }

            // Return a success response
            return Response.ok("{\"status\":\"success\",\"message\":\"Results received and processed\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            // Return an error response if something goes wrong
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"status\":\"error\",\"message\":\"An error occurred while processing results.\"}")
                    .build();
        }
    }
    
    
    // Add your additional middleware-related methods here
}
