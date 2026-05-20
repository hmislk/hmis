package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.*;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.InvestigationFormatApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("investigations/{investigationId}/format")
@RequestScoped
public class InvestigationFormatApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InvestigationFormatApiService formatService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // Items
    // =========================================================================

    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listItems(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listItems(investigationId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @GET
    @Path("/items/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItem(@PathParam("investigationId") Long investigationId,
                            @PathParam("itemId") Long itemId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.getItem(investigationId, itemId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createItem(@PathParam("investigationId") Long investigationId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationItemCreateDTO req = gson.fromJson(body, InvestigationItemCreateDTO.class);
            InvestigationItemDTO dto = formatService.createItem(investigationId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/items/{itemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("investigationId") Long investigationId,
                               @PathParam("itemId") Long itemId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationItemUpdateDTO req = gson.fromJson(body, InvestigationItemUpdateDTO.class);
            return successResponse(formatService.updateItem(investigationId, itemId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/items/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteItem(@PathParam("investigationId") Long investigationId,
                               @PathParam("itemId") Long itemId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteItem(investigationId, itemId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Values
    // =========================================================================

    @GET
    @Path("/items/{itemId}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listValues(@PathParam("investigationId") Long investigationId,
                               @PathParam("itemId") Long itemId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listValues(investigationId, itemId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/items/{itemId}/values")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createValue(@PathParam("investigationId") Long investigationId,
                                @PathParam("itemId") Long itemId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationItemValueDTO req = gson.fromJson(body, InvestigationItemValueDTO.class);
            InvestigationItemValueDTO dto = formatService.createValue(investigationId, itemId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/values/{valueId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateValue(@PathParam("investigationId") Long investigationId,
                                @PathParam("valueId") Long valueId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationItemValueDTO req = gson.fromJson(body, InvestigationItemValueDTO.class);
            return successResponse(formatService.updateValue(investigationId, valueId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/values/{valueId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteValue(@PathParam("investigationId") Long investigationId,
                                @PathParam("valueId") Long valueId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteValue(investigationId, valueId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Calculations
    // =========================================================================

    @GET
    @Path("/calculations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCalculations(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listCalculations(investigationId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/calculations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCalculation(@PathParam("investigationId") Long investigationId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            IxCalDTO req = gson.fromJson(body, IxCalDTO.class);
            IxCalDTO dto = formatService.createCalculation(investigationId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/calculations/{calcId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCalculation(@PathParam("investigationId") Long investigationId,
                                      @PathParam("calcId") Long calcId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            IxCalDTO req = gson.fromJson(body, IxCalDTO.class);
            return successResponse(formatService.updateCalculation(investigationId, calcId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/calculations/{calcId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCalculation(@PathParam("investigationId") Long investigationId,
                                      @PathParam("calcId") Long calcId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteCalculation(investigationId, calcId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Flags
    // =========================================================================

    @GET
    @Path("/flags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFlags(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listFlags(investigationId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/flags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlag(@PathParam("investigationId") Long investigationId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            TestFlagDTO req = gson.fromJson(body, TestFlagDTO.class);
            TestFlagDTO dto = formatService.createFlag(investigationId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/flags/{flagId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFlag(@PathParam("investigationId") Long investigationId,
                               @PathParam("flagId") Long flagId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            TestFlagDTO req = gson.fromJson(body, TestFlagDTO.class);
            return successResponse(formatService.updateFlag(investigationId, flagId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/flags/{flagId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFlag(@PathParam("investigationId") Long investigationId,
                               @PathParam("flagId") Long flagId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteFlag(investigationId, flagId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Dynamic Labels
    // =========================================================================

    @GET
    @Path("/dynamic-labels")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDynamicLabels(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listDynamicLabels(investigationId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/dynamic-labels")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDynamicLabel(@PathParam("investigationId") Long investigationId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            DynamicLabelDTO req = gson.fromJson(body, DynamicLabelDTO.class);
            DynamicLabelDTO dto = formatService.createDynamicLabel(investigationId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/dynamic-labels/{labelId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDynamicLabel(@PathParam("investigationId") Long investigationId,
                                       @PathParam("labelId") Long labelId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            DynamicLabelDTO req = gson.fromJson(body, DynamicLabelDTO.class);
            return successResponse(formatService.updateDynamicLabel(investigationId, labelId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/dynamic-labels/{labelId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDynamicLabel(@PathParam("investigationId") Long investigationId,
                                       @PathParam("labelId") Long labelId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteDynamicLabel(investigationId, labelId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired()) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        Date expiry = apiKey.getDateOfExpiary();
        if (expiry == null || expiry.before(new Date())) {
            return null;
        }
        return user;
    }

    private Response successResponse(Object data) {
        return Response.ok(gson.toJson(successData(data))).build();
    }

    private Response errorResponse(String message, int statusCode) {
        return Response.status(statusCode).entity(gson.toJson(errorData(message, statusCode))).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("code", 200);
        map.put("timestamp", new Date());
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorData(String message, int statusCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "error");
        map.put("code", statusCode);
        map.put("message", message);
        map.put("timestamp", new Date());
        map.put("data", Collections.emptyMap());
        return map;
    }
}
