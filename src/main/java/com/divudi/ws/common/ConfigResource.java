package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.facade.ConfigOptionFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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

    @EJB
    ConfigOptionFacade configOptionFacade;

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
        if (!apiKeyController.isValidKey(apiKey)) {
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
        String apiKey = headers.getHeaderString("Config");
        if (!apiKeyController.isValidKey(apiKey)) {
            return unauthorizedResponse();
        }
        configOptionApplicationController.setLongTextValueByKey(key, value);
        return successResponse();
    }

    @POST
    @Path("setInteger/{key}/{value}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response setIntegerValue(@PathParam("key") String key,
            @PathParam("value") int value,
            @Context HttpHeaders headers) {
        String apiKey = headers.getHeaderString("Config");
        if (!apiKeyController.isValidKey(apiKey)) {
            return unauthorizedResponse();
        }
        configOptionApplicationController.setIntegerValueByKey(key, value);
        return successResponse();
    }

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchConfigOptions(@QueryParam("keyword") String keyword,
            @Context HttpHeaders headers) {
        String apiKey = headers.getHeaderString("Config");
        if (!apiKeyController.isValidKey(apiKey)) {
            return unauthorizedResponse();
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"keyword query parameter is required\"}")
                    .build();
        }
        String jpql = "SELECT o FROM ConfigOption o "
                + "WHERE o.retired = false "
                + "AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL "
                + "AND LOWER(o.optionKey) LIKE :kw "
                + "ORDER BY o.optionKey";
        Map<String, Object> params = new HashMap<>();
        params.put("kw", "%" + keyword.toLowerCase().trim() + "%");

        List<ConfigOption> options = configOptionFacade.findByJpql(jpql, params);

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (ConfigOption opt : options) {
            String value = maskSensitiveValue(opt.getOptionKey(), opt.getOptionValue());
            JsonObjectBuilder obj = Json.createObjectBuilder()
                    .add("key", opt.getOptionKey() != null ? opt.getOptionKey() : "")
                    .add("type", opt.getValueType() != null ? opt.getValueType().toString() : "")
                    .add("value", value != null ? value : "");
            arrayBuilder.add(obj);
        }
        return Response.ok(arrayBuilder.build().toString()).build();
    }

    private String maskSensitiveValue(String key, String value) {
        if (key == null || value == null || value.isEmpty()) {
            return value;
        }
        String lk = key.toLowerCase();
        if (lk.contains("password") || lk.contains("secret")
                || lk.contains("api key") || lk.contains("token")
                || lk.contains("apikey") || lk.contains("private")) {
            return "***masked***";
        }
        return value;
    }

    private Response successResponse() {
        return Response.ok("Configuration updated successfully.").build();
    }

    private Response unauthorizedResponse() {
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized").build();
    }

    // Removed unnecessary default methods
}
