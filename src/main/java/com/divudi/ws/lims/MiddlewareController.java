package com.divudi.ws.lims;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/middleware")
public class MiddlewareController {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkService() {
        return Response.ok("Middleware service is working").build();
    }

    // Add your additional middleware-related methods here
}
