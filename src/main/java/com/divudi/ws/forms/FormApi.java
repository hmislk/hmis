package com.divudi.ws.forms;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.forms.FormChoiceDto;
import com.divudi.core.data.dto.forms.FormEntryDto;
import com.divudi.core.data.dto.forms.FormFieldDto;
import com.divudi.core.data.dto.forms.FormTemplateDto;
import com.divudi.core.data.dto.forms.FormValueDto;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.forms.FormApiService;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("forms")
@RequestScoped
public class FormApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private FormApiService formApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public FormApi() {}

    // -------------------------------------------------------------------------
    // Form Templates
    // -------------------------------------------------------------------------

    @GET
    @Path("/templates")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTemplates() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            List<FormTemplateDto> list = formApiService.listFormTemplates();
            return successResponse(list);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @GET
    @Path("/templates/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplate(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            FormTemplateDto dto = formApiService.getFormTemplateWithFields(id);
            return successResponse(dto);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/templates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTemplate(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            FormTemplateDto dto = formApiService.createFormTemplate(
                    (String) body.get("name"),
                    (String) body.get("description"),
                    (String) body.get("formCssClass"),
                    user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/templates/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTemplate(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            FormTemplateDto dto = formApiService.updateFormTemplate(
                    id,
                    (String) body.get("name"),
                    (String) body.get("description"),
                    (String) body.get("formCssClass"),
                    user);
            return successResponse(dto);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), e.getMessage().contains("not found") ? 404 : 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/templates/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireTemplate(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            formApiService.retireFormTemplate(id, null, user);
            return successResponse("Form template retired");
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    @GET
    @Path("/templates/{id}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFields(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            List<FormFieldDto> list = formApiService.listFormFields(id);
            return successResponse(list);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/templates/{id}/fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addField(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            FormFieldDto dto = formApiService.addFormField(id, body, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), e.getMessage().contains("not found") ? 404 : 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/fields/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateField(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            FormFieldDto dto = formApiService.updateFormField(id, body, user);
            return successResponse(dto);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), e.getMessage().contains("not found") ? 404 : 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/fields/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireField(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            formApiService.retireFormField(id, user);
            return successResponse("Field retired");
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Choices
    // -------------------------------------------------------------------------

    @GET
    @Path("/fields/{id}/choices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listChoices(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            List<FormChoiceDto> list = formApiService.listChoices(id);
            return successResponse(list);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/fields/{id}/choices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addChoice(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            Integer orderNo = body.get("orderNo") != null ? (int) Double.parseDouble(body.get("orderNo").toString()) : null;
            FormChoiceDto dto = formApiService.addChoice(
                    id,
                    (String) body.get("label"),
                    (String) body.get("value"),
                    orderNo,
                    user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), e.getMessage().contains("not found") ? 404 : 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/choices/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChoice(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            Map body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);
            Integer orderNo = body.get("orderNo") != null ? (int) Double.parseDouble(body.get("orderNo").toString()) : null;
            FormChoiceDto dto = formApiService.updateChoice(
                    id,
                    (String) body.get("label"),
                    (String) body.get("value"),
                    orderNo,
                    user);
            return successResponse(dto);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), e.getMessage().contains("not found") ? 404 : 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/choices/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireChoice(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            formApiService.retireChoice(id, user);
            return successResponse("Choice retired");
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Entries and Values
    // -------------------------------------------------------------------------

    @GET
    @Path("/entries/{admissionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listEntries(@PathParam("admissionId") Long admissionId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            List<FormEntryDto> list = formApiService.listEntriesForAdmission(admissionId);
            return successResponse(list);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @GET
    @Path("/entries/{entryId}/values")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listValues(@PathParam("entryId") Long entryId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            List<FormValueDto> list = formApiService.listValuesForEntry(entryId);
            return successResponse(list);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) return null;
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        return user;
    }

    @SuppressWarnings("unchecked")
    private Map parseBody(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            return gson.fromJson(body, Map.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    private Response successResponse(Object data) {
        return Response.status(200).entity(gson.toJson(successData(data))).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
