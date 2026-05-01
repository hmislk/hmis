package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.*;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.InvestigationApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Path("investigations") @RequestScoped
public class InvestigationApi {
    @Context private HttpServletRequest requestContext;
    @Context private UriInfo uriInfo;
    @Inject private ApiKeyController apiKeyController;
    @Inject private InvestigationApiService investigationApiService;
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @GET @Path("/search") @Produces(MediaType.APPLICATION_JSON)
    public Response search(){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); String q=uriInfo.getQueryParameters().getFirst("query"); String inactiveStr=uriInfo.getQueryParameters().getFirst("inactive"); String limitStr=uriInfo.getQueryParameters().getFirst("limit"); Boolean inactive = (inactiveStr==null||inactiveStr.trim().isEmpty())?null:Boolean.parseBoolean(inactiveStr.trim()); int limit=20; if(limitStr!=null&&!limitStr.trim().isEmpty()) limit=Math.min(Math.max(Integer.parseInt(limitStr.trim()),1),100); return ok(investigationApiService.search(q,inactive,limit)); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") Long id){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); return ok(investigationApiService.findById(id)); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); InvestigationCreateRequestDTO req=gson.fromJson(body, InvestigationCreateRequestDTO.class); InvestigationResponseDTO dto=investigationApiService.create(req,u); return Response.status(201).entity(gson.toJson(success(dto))).build(); } catch(IllegalStateException ex){ try{ Long id=Long.valueOf(ex.getMessage()); InvestigationResponseDTO existing=investigationApiService.findById(id); Map<String,Object> m=success(existing); m.put("status","already_exists"); m.put("id",id); return Response.status(409).entity(gson.toJson(m)).build(); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }

    @PUT @Path("/{id}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String body){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); return ok(investigationApiService.update(id,gson.fromJson(body, InvestigationUpdateRequestDTO.class),u)); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }

    @PATCH @Path("/{id}/activate") @Produces(MediaType.APPLICATION_JSON) public Response activate(@PathParam("id") Long id){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); return ok(investigationApiService.setActive(id,false,u)); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }
    @PATCH @Path("/{id}/deactivate") @Produces(MediaType.APPLICATION_JSON) public Response deactivate(@PathParam("id") Long id){ try{ WebUser u=validateApiKey(requestContext.getHeader("Finance")); if(u==null) return error("Not a valid key",401); return ok(investigationApiService.setActive(id,true,u)); } catch(Exception e){ return error("An error occurred: "+e.getMessage(),500);} }

    private WebUser validateApiKey(String key){ if(key==null||key.trim().isEmpty()) return null; ApiKey ak=apiKeyController.findAndUpdateLastUsedApiKey(key); return ak==null?null:ak.getWebUser(); }
    private Response ok(Object d){ return Response.ok(gson.toJson(success(d))).build(); }
    private Response error(String m,int c){ return Response.status(c).entity(gson.toJson(err(m,c))).build(); }
    private Map<String,Object> success(Object d){ Map<String,Object> m=new HashMap<>(); m.put("status","success"); m.put("code",200); m.put("timestamp",new Date()); m.put("data",d); return m; }
    private Map<String,Object> err(String m,int c){ Map<String,Object> x=new HashMap<>(); x.put("status","error"); x.put("code",c); x.put("message",m); x.put("timestamp",new Date()); x.put("data",Collections.emptyMap()); return x; }
}
