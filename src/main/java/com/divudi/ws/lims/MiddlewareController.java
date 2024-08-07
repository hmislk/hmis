package com.divudi.ws.lims;

import com.divudi.bean.common.ConfigOptionApplicationController;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/middleware")
public class MiddlewareController {
    
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

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
    
    @GET
    @Path("/pullSampleData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pullSampleData() {
        String jsonInput = configOptionApplicationController.getLongTextValueByKey("Test Json");

        // Check if the retrieved JSON string is not null or empty
        if (jsonInput == null || jsonInput.trim().isEmpty()) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        // Return the retrieved JSON string
        return Response.ok(jsonInput).build();
    }

    // Add your additional middleware-related methods here
}
