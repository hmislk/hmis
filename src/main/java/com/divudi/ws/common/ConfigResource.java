package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.ApiKeyType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

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
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"keyword query parameter is required\"}")
                    .build();
        }
        String jpql = "SELECT o FROM ConfigOption o "
                + "WHERE o.retired = false "
                + "AND o.scope = :scope "
                + "AND LOWER(o.optionKey) LIKE :kw "
                + "ORDER BY o.optionKey";
        Map<String, Object> params = new HashMap<>();
        params.put("scope", OptionScope.APPLICATION);
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

    /**
     * List application-scoped config options, optionally filtered by a scope
     * tag. {@code scope} is matched as a case-insensitive substring of the
     * option key, so {@code scope=inward} returns every option whose key
     * contains "inward" (e.g. the inward-billing flags). With no scope the
     * full application option set is returned.
     *
     * GET /api/config?scope=inward
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConfigOptions(@QueryParam("scope") String scope,
            @Context HttpHeaders headers) {
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("scope", OptionScope.APPLICATION);
        StringBuilder jpql = new StringBuilder(
                "SELECT o FROM ConfigOption o WHERE o.retired = false AND o.scope = :scope ");
        if (scope != null && !scope.trim().isEmpty()) {
            jpql.append("AND LOWER(o.optionKey) LIKE :scopeTag ");
            params.put("scopeTag", "%" + scope.toLowerCase().trim() + "%");
        }
        jpql.append("ORDER BY o.optionKey");

        List<ConfigOption> options = configOptionFacade.findByJpql(jpql.toString(), params);

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (ConfigOption opt : options) {
            arrayBuilder.add(toJson(opt));
        }
        return Response.ok(arrayBuilder.build().toString()).build();
    }

    /**
     * Read a single config option by its exact key.
     *
     * GET /api/config/{key}
     */
    @GET
    @Path("{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigOption(@PathParam("key") String key,
            @Context HttpHeaders headers) {
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }
        ConfigOption option = findApplicationOption(key);
        if (option == null) {
            return notFoundResponse(key);
        }
        return Response.ok(toJson(option).build().toString()).build();
    }

    /**
     * Update a single config option's value by key. Body: {"value":"true"}.
     *
     * The in-memory {@code @ApplicationScoped} cache is reloaded via
     * {@code loadApplicationOptions()} so the change takes effect immediately
     * without a restart. The option must already exist (this endpoint does not
     * create new keys).
     *
     * PUT /api/config/{key}
     */
    @PUT
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfigOption(@PathParam("key") String key,
            String requestBody,
            @Context HttpHeaders headers) {
        ApiKey apiKey = validateConfigKey(headers);
        if (apiKey == null) {
            return unauthorizedResponse();
        }

        String newValue;
        try {
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return badRequestResponse("Request body with a \"value\" field is required");
            }
            JsonReader reader = Json.createReader(new StringReader(requestBody));
            JsonObject body = reader.readObject();
            if (!body.containsKey("value") || body.isNull("value")) {
                return badRequestResponse("\"value\" field is required");
            }
            // Accept a JSON string ("true") or a raw JSON scalar (true / 5);
            // config values are persisted as strings regardless.
            javax.json.JsonValue jv = body.get("value");
            if (jv.getValueType() == javax.json.JsonValue.ValueType.STRING) {
                newValue = body.getString("value");
            } else {
                newValue = jv.toString();
            }
        } catch (JsonException | IllegalStateException e) {
            return badRequestResponse("Invalid JSON body: " + e.getMessage());
        }

        ConfigOption option = findApplicationOption(key);
        if (option == null) {
            return notFoundResponse(key);
        }

        // Sanitize LONG_TEXT the same way ConfigOptionApplicationController
        // .setLongTextValueByKey() does, since some LONG_TEXT options are
        // rendered with escape="false" in JSF — an unsanitized value would be a
        // stored-XSS vector.
        if (option.getValueType() == OptionValueType.LONG_TEXT) {
            newValue = Jsoup.clean(newValue, Safelist.basic());
        }

        String oldValue = option.getOptionValue();
        option.setOptionValue(newValue);
        configOptionFacade.edit(option);
        // Flush the @ApplicationScoped cache so the change is effective immediately.
        configOptionApplicationController.loadApplicationOptions();

        // Audit trail: who changed which key, from what to what, and when.
        WebUser user = apiKey.getWebUser();
        LOGGER.log(Level.INFO, "CONFIG_UPDATED key=[{0}] oldValue=[{1}] newValue=[{2}] by user=[{3}] at=[{4}]",
                new Object[]{key,
                    maskSensitiveValue(key, oldValue),
                    maskSensitiveValue(key, newValue),
                    user != null ? user.getName() : "unknown",
                    new java.util.Date()});

        return Response.ok(toJson(option).build().toString()).build();
    }

    /**
     * Find an application-scoped, non-retired config option by exact key.
     */
    private ConfigOption findApplicationOption(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("scope", OptionScope.APPLICATION);
        params.put("k", key);
        return configOptionFacade.findFirstByJpql(
                "SELECT o FROM ConfigOption o WHERE o.retired = false AND o.scope = :scope AND o.optionKey = :k",
                params);
    }

    /**
     * Validate the Config API key from the request headers, returning the
     * ApiKey when valid (active, Config type, not expired) or null otherwise.
     */
    private ApiKey validateConfigKey(HttpHeaders headers) {
        String apiKeyValue = headers.getHeaderString("Config");
        ApiKey apiKey = apiKeyController.findApiKey(apiKeyValue);
        if (apiKey == null || apiKey.isRetired()
                || apiKey.getKeyType() != ApiKeyType.Config
                || apiKey.getDateOfExpiary() == null
                || apiKey.getDateOfExpiary().before(new java.util.Date())) {
            return null;
        }
        return apiKey;
    }

    /**
     * Build a JSON representation of a config option with sensitive values masked.
     */
    private JsonObjectBuilder toJson(ConfigOption opt) {
        String value = maskSensitiveValue(opt.getOptionKey(), opt.getOptionValue());
        return Json.createObjectBuilder()
                .add("key", opt.getOptionKey() != null ? opt.getOptionKey() : "")
                .add("type", opt.getValueType() != null ? opt.getValueType().toString() : "")
                .add("scope", opt.getScope() != null ? opt.getScope().toString() : "")
                .add("value", value != null ? value : "");
    }

    private Response notFoundResponse(String key) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Config option not found: " + key + "\"}")
                .build();
    }

    private Response badRequestResponse(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + message + "\"}")
                .build();
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
