package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.entity.ApiKey;
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
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }
        configOptionApplicationController.setBooleanValueByKey(key, value);
        return successResponse();
    }

    @POST
    @Path("setLongText/{key}/{value}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response setLongTextValue(@PathParam("key") String key,
            @PathParam("value") String value,
            @Context HttpHeaders headers) {
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }
        configOptionApplicationController.setLongTextValueByKey(key, value);
        return successResponse();
    }

    /**
     * Validate the Config API key from the request headers, returning the
     * ApiKey when valid or null otherwise.
     */
    private ApiKey validateConfigKey(HttpHeaders headers) {
        String apiKeyValue = headers.getHeaderString("Config");
        ApiKey apiKey = apiKeyController.findApiKey(apiKeyValue);
        // API key hardening
        if (apiKeyController.resolveConfigKeyOwner(apiKey) == null) {
            return null;
        }
        return apiKey;
    }

    private Response successResponse() {
        return Response.ok("Configuration updated successfully.").build();
    }

    private Response unauthorizedResponse() {
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized").build();
    }

    // Removed unnecessary default methods
}
