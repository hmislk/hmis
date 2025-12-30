package com.divudi.ws.report;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.service.PatientInvestigationService;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * REST Web Service
 *
 * @author Imesh Ranawella
 */
@Path("report")
@RequestScoped
public class ReportApi {
    private static final Logger LOGGER = Logger.getLogger(ReportApi.class.getName());

    @EJB
    private PatientInvestigationService patientInvestigationService;
    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    ApiKeyController apiKeyController;

    private SearchKeyword searchKeyword;

    public ReportApi() {
    }

    /**
     * Get the reports by mobile number
     * Endpoint: /report/by-mobile/{mobile}
     */
    @GET
    @Path("by-mobile/{mobile}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportsByMobile(
            @Context HttpServletRequest requestContext,
            @PathParam("mobile") String mobile,
            @QueryParam("fromDate") @NotNull String fromDate,
            @QueryParam("toDate") @NotNull String toDate) {
        final String key = requestContext.getHeader("Token");

        if (!isValidKey(key)) {
            JSONObject responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(json).build();
        }

        try {
            validateDates(fromDate, toDate);
            validateMobileNumber(mobile);
        } catch (WebApplicationException ex) {
            LOGGER.warning("Validation error: " + ex.getMessage());
            return Response.status(ex.getResponse().getStatus())
                    .entity(ex.getMessage())
                    .build();
        }

        getSearchKeyword().setPatientPhone(mobile);
        getSearchKeyword().setPatientInvestigationStatus(PatientInvestigationStatus.REPORT_APPROVED);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime fromLocal = LocalDateTime.parse(fromDate.trim(), formatter);
        LocalDateTime toLocal = LocalDateTime.parse(toDate.trim(), formatter);

        List<PatientInvestigation> investigations = patientInvestigationService.fetchPatientInvestigations(
                Date.from(fromLocal.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(toLocal.atZone(ZoneId.systemDefault()).toInstant()),
                getSearchKeyword()
        );

        ViewReportsResponseDTO responseDTO = new ViewReportsResponseDTO(
                mobile,
                fromDate,
                toDate,
                investigations
        );

        return Response.status(Response.Status.OK)
                .entity(responseDTO)
                .build();
    }

    private void validateDates(final String fromDate, final String toDate) {
        if (fromDate == null || fromDate.isEmpty() || toDate == null || toDate.isEmpty()) {
            throw new WebApplicationException("From date and To date must be provided", Response.Status.BAD_REQUEST);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            LocalDateTime fromLocal = LocalDateTime.parse(fromDate.trim(), formatter);
            LocalDateTime toLocal = LocalDateTime.parse(toDate.trim(), formatter);
            Date from = Date.from(fromLocal.atZone(ZoneId.systemDefault()).toInstant());
            Date to = Date.from(toLocal.atZone(ZoneId.systemDefault()).toInstant());

            if (from.after(to)) {
                throw new WebApplicationException("From date must be before To date", Response.Status.BAD_REQUEST);
            }

            LocalDateTime oneMonthAfterFrom = fromLocal.plusMonths(1);
            if (toLocal.isAfter(oneMonthAfterFrom)) {
                throw new WebApplicationException("Date range cannot exceed 1 month", Response.Status.BAD_REQUEST);
            }
        } catch (DateTimeParseException ex) {
            LOGGER.warning("Invalid date format: " + ex.getMessage());

            throw new WebApplicationException("Invalid date format. Please use ISO format (yyyy-MM-dd HH:mm:ss).", Response.Status.BAD_REQUEST);
        }
    }

    private void validateMobileNumber(final String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            throw new WebApplicationException("Mobile number must be provided", Response.Status.BAD_REQUEST);
        }
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null) {
            return false;
        }
        if (k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired()) {
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
    }

    private JSONObject errorMessageNotValidKey() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid key.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
}
