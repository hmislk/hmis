package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.PaymentSchemeUpdateDto;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.PaymentSchemeApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("payment-scheme")
@RequestScoped
public class PaymentSchemeApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PaymentSchemeApiService paymentSchemeApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            String query = requestContext.getParameter("query");
            String limitStr = requestContext.getParameter("limit");
            int limit = 500;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try { limit = Integer.parseInt(limitStr.trim()); } catch (NumberFormatException ignored) {}
            }

            return successResponse(paymentSchemeApiService.list(query, limit));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            PaymentSchemeUpdateDto request;
            try {
                request = gson.fromJson(body, PaymentSchemeUpdateDto.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON: " + e.getMessage(), 400);
            }
            if (request == null) return errorResponse("Request body is required", 400);

            return successResponse(paymentSchemeApiService.update(id, request));
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 400);
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) return null;
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        return user;
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error");
        r.put("code", code);
        r.put("message", message);
        return Response.status(code).entity(gson.toJson(r)).build();
    }

    private Response successResponse(Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "success");
        r.put("code", 200);
        r.put("data", data);
        return Response.status(200).entity(gson.toJson(r)).build();
    }
}
