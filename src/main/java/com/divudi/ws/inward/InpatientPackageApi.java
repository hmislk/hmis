package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.inward.InpatientPackageDto;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.inward.InpatientPackageApiService;
import com.divudi.service.inward.InpatientPackageValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("inpatient-packages")
@RequestScoped
public class InpatientPackageApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InpatientPackageApiService inpatientPackageApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Create a full package (header + items[]) in one call.
     * POST /api/inpatient-packages
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            InpatientPackageDto request;
            try {
                request = gson.fromJson(requestBody, InpatientPackageDto.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            InpatientPackageDto response = inpatientPackageApiService.createPackage(request, user);
            return successResponse(201, response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * List non-retired packages, optionally filtered by admissionTypeId/roomCategoryId.
     * GET /api/inpatient-packages?admissionTypeId=&roomCategoryId=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Long admissionTypeId = parseLongParam("admissionTypeId");
            Long roomCategoryId = parseLongParam("roomCategoryId");

            List<InpatientPackageDto> results =
                    inpatientPackageApiService.listPackages(admissionTypeId, roomCategoryId);
            return successResponse(results);

        } catch (NumberFormatException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Fetch one package with its items.
     * GET /api/inpatient-packages/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            InpatientPackageDto response = inpatientPackageApiService.getPackage(id);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Full-replace update. Header fields are always overwritten; items is only replaced
     * when the request body includes an "items" key (see InpatientPackageApiService).
     * PUT /api/inpatient-packages/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            InpatientPackageDto request;
            try {
                request = gson.fromJson(requestBody, InpatientPackageDto.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            InpatientPackageDto response = inpatientPackageApiService.updatePackage(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Soft-retire a package. Body: {"retireComments": "..."} (optional).
     * POST /api/inpatient-packages/{id}/retire
     */
    @POST
    @Path("/{id}/retire")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retire(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = null;
            if (requestBody != null && !requestBody.trim().isEmpty()) {
                Map<?, ?> body;
                try {
                    body = gson.fromJson(requestBody, Map.class);
                } catch (JsonSyntaxException e) {
                    return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
                }
                if (body != null) {
                    Object rc = body.get("retireComments");
                    if (rc != null && !(rc instanceof String)) {
                        return errorResponse("retireComments must be a string", 400);
                    }
                    retireComments = (String) rc;
                }
            }

            InpatientPackageDto response = inpatientPackageApiService.retirePackage(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private Long parseLongParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid " + name + " format");
        }
    }

    private Response mapError(Exception e) {
        String msg = e.getMessage();
        if (e instanceof InpatientPackageValidationException) {
            return errorResponse(msg, 400);
        }
        if (msg == null) {
            return errorResponse("An error occurred: Unknown error", 500);
        }
        if (msg.contains("not found")) {
            return errorResponse(msg, 404);
        }
        return errorResponse("An error occurred: " + msg, 500);
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        if (apiKey.isRetired()) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
        return user;
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    private Response successResponse(Object data) {
        return successResponse(200, data);
    }

    private Response successResponse(int status, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", status);
        response.put("data", data);
        return Response.status(status).entity(gson.toJson(response)).build();
    }
}
