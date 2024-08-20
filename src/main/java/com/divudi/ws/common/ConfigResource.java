package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

/**
 * REST Web Service
 *
 * @author Dr M H B Ariyaratne
 */
@Path("config")
@RequestScoped
public class ConfigResource {

    @Context
    private UriInfo context;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    ApiKeyController apiKeyController;

    private static final Logger LOGGER = Logger.getLogger(ConfigResource.class.getName());

    /**
     * Creates a new instance of ConfigResource
     */
    public ConfigResource() {
    }

    @POST
    @Path("setBoolean/{key}/{value}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response setBooleanValue(@PathParam("key") String key,
            @PathParam("value") boolean value,
            @Context HttpHeaders headers) {
        System.out.println("setBooleanValue" );
        System.out.println("headers = " + headers);
        System.out.println("key = " + key);
        System.out.println("value = " + value);
        String apiKey = headers.getHeaderString("Config");
        System.out.println("apiKey = " + apiKey);
        if (!apiKeyController.isValidKey(apiKey)) {
            System.out.println("unautherized");
            return unauthorizedResponse();
        }
        System.out.println("setting");
        configOptionApplicationController.setBooleanValueByKey(key, value);
        System.out.println("success = " );
        return successResponse();
    }

    @POST
    @Path("setLongText/{key}/{value}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response setLongTextValue(@PathParam("key") String key,
            @PathParam("value") String value,
            @Context HttpHeaders headers) {
        String apiKey = headers.getHeaderString("Config");
        if (!apiKeyController.isValidKey(apiKey)) {
            return unauthorizedResponse();
        }
        configOptionApplicationController.setLongTextValueByKey(key, value);
        return successResponse();
    }

    private Response successResponse() {
        return Response.ok("Configuration updated successfully.").build();
    }

    private Response unauthorizedResponse() {
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized").build();
    }

    // Removed unnecessary default methods
}
