package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.AgentHistoryDTO;
import com.divudi.core.data.dto.DrawerEntryDTO;
import com.divudi.core.data.dto.PatientDepositHistoryDto;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.DrawerEntryFacade;
import com.divudi.core.facade.PatientDepositHistoryFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for Balance History queries
 * Provides endpoints for drawer entries, patient deposits, and agent histories
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Path("balance_history")
@RequestScoped
public class BalanceHistoryApi {

    @Context
    private HttpServletRequest requestContext;

    @EJB
    private DrawerEntryFacade drawerEntryFacade;

    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;

    @EJB
    private AgentHistoryFacade agentHistoryFacade;

    @Inject
    ApiKeyController apiKeyController;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BalanceHistoryApi() {
    }

    /**
     * Helper method to parse date string to Date object using thread-safe DateTimeFormatter
     */
    private Date parseDate(String dateStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
        return java.sql.Timestamp.valueOf(localDateTime);
    }

    /**
     * Get drawer entries with optional filters
     * Endpoint: /balance_history/drawer_entries
     */
    @GET
    @Path("/drawer_entries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrawerEntries(
            @QueryParam("billId") Long billId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("limit") Integer limit) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            // Build JPQL query
            StringBuilder jpql = new StringBuilder("SELECT de FROM DrawerEntry de WHERE de.retired = false");
            Map<String, Object> params = new HashMap<>();

            if (billId != null) {
                jpql.append(" AND de.bill.id = :billId");
                params.put("billId", billId);
            }

            if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                try {
                    PaymentMethod pm = PaymentMethod.valueOf(paymentMethod.trim());
                    jpql.append(" AND de.paymentMethod = :paymentMethod");
                    params.put("paymentMethod", pm);
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid payment method: " + paymentMethod, 400);
                }
            }

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                try {
                    Date fromDate = parseDate(fromDateStr);
                    jpql.append(" AND de.createdAt >= :fromDate");
                    params.put("fromDate", fromDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                try {
                    Date toDate = parseDate(toDateStr);
                    jpql.append(" AND de.createdAt <= :toDate");
                    params.put("toDate", toDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            jpql.append(" ORDER BY de.id DESC");

            // Apply limit
            int maxResults = (limit != null && limit > 0) ? limit : 100;

            List<DrawerEntry> entries = drawerEntryFacade.findByJpql(jpql.toString(), params, maxResults);

            // Convert to DTOs
            List<DrawerEntryDTO> dtos = new ArrayList<>();
            if (entries != null) {
                for (DrawerEntry entry : entries) {
                    dtos.add(convertDrawerEntryToDTO(entry));
                }
            }

            return successResponse(dtos);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get patient deposit histories with optional filters
     * Endpoint: /balance_history/patient_deposits
     */
    @GET
    @Path("/patient_deposits")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientDepositHistories(
            @QueryParam("billId") Long billId,
            @QueryParam("patientId") Long patientId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("limit") Integer limit) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            // Build JPQL query
            StringBuilder jpql = new StringBuilder("SELECT pdh FROM PatientDepositHistory pdh WHERE pdh.retired = false");
            Map<String, Object> params = new HashMap<>();

            if (billId != null) {
                jpql.append(" AND pdh.bill.id = :billId");
                params.put("billId", billId);
            }

            if (patientId != null) {
                jpql.append(" AND pdh.patientDeposit.patient.id = :patientId");
                params.put("patientId", patientId);
            }

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                try {
                    Date fromDate = parseDate(fromDateStr);
                    jpql.append(" AND pdh.createdAt >= :fromDate");
                    params.put("fromDate", fromDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                try {
                    Date toDate = parseDate(toDateStr);
                    jpql.append(" AND pdh.createdAt <= :toDate");
                    params.put("toDate", toDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            jpql.append(" ORDER BY pdh.id DESC");

            // Apply limit
            int maxResults = (limit != null && limit > 0) ? limit : 100;

            List<PatientDepositHistory> histories = patientDepositHistoryFacade.findByJpql(jpql.toString(), params, maxResults);

            // Convert to DTOs
            List<PatientDepositHistoryDto> dtos = new ArrayList<>();
            if (histories != null) {
                for (PatientDepositHistory history : histories) {
                    dtos.add(convertPatientDepositHistoryToDTO(history));
                }
            }

            return successResponse(dtos);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get agent histories (collecting centres) with optional filters
     * Endpoint: /balance_history/agent_histories
     */
    @GET
    @Path("/agent_histories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgentHistories(
            @QueryParam("billId") Long billId,
            @QueryParam("agencyId") Long agencyId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("limit") Integer limit) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            // Build JPQL query
            StringBuilder jpql = new StringBuilder("SELECT ah FROM AgentHistory ah WHERE ah.retired = false");
            Map<String, Object> params = new HashMap<>();

            if (billId != null) {
                jpql.append(" AND ah.bill.id = :billId");
                params.put("billId", billId);
            }

            if (agencyId != null) {
                jpql.append(" AND ah.agency.id = :agencyId");
                params.put("agencyId", agencyId);
            }

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                try {
                    Date fromDate = parseDate(fromDateStr);
                    jpql.append(" AND ah.createdAt >= :fromDate");
                    params.put("fromDate", fromDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                try {
                    Date toDate = parseDate(toDateStr);
                    jpql.append(" AND ah.createdAt <= :toDate");
                    params.put("toDate", toDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            jpql.append(" ORDER BY ah.id DESC");

            // Apply limit
            int maxResults = (limit != null && limit > 0) ? limit : 100;

            List<AgentHistory> histories = agentHistoryFacade.findByJpql(jpql.toString(), params, maxResults);

            // Convert to DTOs
            List<AgentHistoryDTO> dtos = new ArrayList<>();
            if (histories != null) {
                for (AgentHistory history : histories) {
                    dtos.add(convertAgentHistoryToDTO(history));
                }
            }

            return successResponse(dtos);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get staff welfare histories (uses DrawerEntry filtered by Staff_Welfare)
     * Endpoint: /balance_history/staff_welfare_histories
     */
    @GET
    @Path("/staff_welfare_histories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStaffWelfareHistories(
            @QueryParam("billId") Long billId,
            @QueryParam("staffId") Long staffId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("limit") Integer limit) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            // Build JPQL query - filter by Staff_Welfare payment method
            StringBuilder jpql = new StringBuilder("SELECT de FROM DrawerEntry de WHERE de.retired = false AND de.paymentMethod = com.divudi.core.data.PaymentMethod.Staff_Welfare");
            Map<String, Object> params = new HashMap<>();

            if (billId != null) {
                jpql.append(" AND de.bill.id = :billId");
                params.put("billId", billId);
            }

            if (staffId != null) {
                jpql.append(" AND de.staff.id = :staffId");
                params.put("staffId", staffId);
            }

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                try {
                    Date fromDate = parseDate(fromDateStr);
                    jpql.append(" AND de.createdAt >= :fromDate");
                    params.put("fromDate", fromDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                try {
                    Date toDate = parseDate(toDateStr);
                    jpql.append(" AND de.createdAt <= :toDate");
                    params.put("toDate", toDate);
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            jpql.append(" ORDER BY de.id DESC");

            // Apply limit
            int maxResults = (limit != null && limit > 0) ? limit : 100;

            List<DrawerEntry> entries = drawerEntryFacade.findByJpql(jpql.toString(), params, maxResults);

            // Convert to DTOs
            List<DrawerEntryDTO> dtos = new ArrayList<>();
            if (entries != null) {
                for (DrawerEntry entry : entries) {
                    dtos.add(convertDrawerEntryToDTO(entry));
                }
            }

            return successResponse(dtos);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Convert DrawerEntry entity to DrawerEntryDTO
     */
    private DrawerEntryDTO convertDrawerEntryToDTO(DrawerEntry entry) {
        DrawerEntryDTO dto = new DrawerEntryDTO();
        dto.setId(entry.getId());
        dto.setDrawerId(entry.getDrawer() != null ? entry.getDrawer().getId() : null);
        dto.setDrawerName(entry.getDrawer() != null ? entry.getDrawer().getName() : null);
        dto.setPaymentMethod(entry.getPaymentMethod() != null ? entry.getPaymentMethod().toString() : null);
        dto.setBeforeBalance(entry.getBeforeBalance());
        dto.setAfterBalance(entry.getAfterBalance());
        dto.setTransactionValue(entry.getTransactionValue());
        dto.setBeforeInHandValue(entry.getBeforeInHandValue());
        dto.setAfterInHandValue(entry.getAfterInHandValue());
        dto.setBillId(entry.getBill() != null ? entry.getBill().getId() : null);
        dto.setBillNumber(entry.getBill() != null ? entry.getBill().getDeptId() : null);
        dto.setPaymentId(entry.getPayment() != null ? entry.getPayment().getId() : null);
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setCreaterName(entry.getCreater() != null ? entry.getCreater().getName() : null);
        return dto;
    }

    /**
     * Convert PatientDepositHistory entity to PatientDepositHistoryDto
     */
    private PatientDepositHistoryDto convertPatientDepositHistoryToDTO(PatientDepositHistory history) {
        PatientDepositHistoryDto dto = new PatientDepositHistoryDto();
        dto.setId(history.getId());
        dto.setBillId(history.getBill() != null ? history.getBill().getId() : null);
        dto.setBillNumber(history.getBill() != null ? history.getBill().getDeptId() : null);
        dto.setBillTypeAtomic(history.getBill() != null ? history.getBill().getBillTypeAtomic() : null);
        dto.setCreatedAt(history.getCreatedAt());
        dto.setHistoryType(history.getHistoryType());

        // Patient info
        if (history.getPatientDeposit() != null && history.getPatientDeposit().getPatient() != null) {
            dto.setPatientId(history.getPatientDeposit().getPatient().getId());
            dto.setPatientName(history.getPatientDeposit().getPatient().getPerson() != null
                    ? history.getPatientDeposit().getPatient().getPerson().getName() : null);
            dto.setPatientPhn(history.getPatientDeposit().getPatient().getPhn());
        }

        // Balance info
        dto.setBalanceBeforeTransaction(history.getBalanceBeforeTransaction());
        dto.setBalanceAfterTransaction(history.getBalanceAfterTransaction());
        dto.setTransactionValue(history.getTransactionValue());

        // Location info
        dto.setDepartmentId(history.getDepartment() != null ? history.getDepartment().getId() : null);
        dto.setDepartmentName(history.getDepartment() != null ? history.getDepartment().getName() : null);
        dto.setInstitutionId(history.getInstitution() != null ? history.getInstitution().getId() : null);
        dto.setInstitutionName(history.getInstitution() != null ? history.getInstitution().getName() : null);

        // Bill status
        if (history.getBill() != null) {
            dto.setCancelled(history.getBill().isCancelled());
            dto.setRefunded(history.getBill().isRefunded());
        }

        // Creator info
        dto.setCreaterName(history.getCreater() != null ? history.getCreater().getName() : null);

        return dto;
    }

    /**
     * Convert AgentHistory entity to AgentHistoryDTO
     */
    private AgentHistoryDTO convertAgentHistoryToDTO(AgentHistory history) {
        AgentHistoryDTO dto = new AgentHistoryDTO();
        dto.setId(history.getId());
        dto.setAgencyId(history.getAgency() != null ? history.getAgency().getId() : null);
        dto.setAgencyName(history.getAgency() != null ? history.getAgency().getName() : null);
        dto.setBillId(history.getBill() != null ? history.getBill().getId() : null);
        dto.setBillNumber(history.getBill() != null ? history.getBill().getDeptId() : null);
        dto.setBalanceBeforeTransaction(history.getBalanceBeforeTransaction());
        dto.setBalanceAfterTransaction(history.getBalanceAfterTransaction());
        dto.setTransactionValue(history.getTransactionValue());
        dto.setAgentBalanceBefore(history.getAgentBalanceBefore());
        dto.setAgentBalanceAfter(history.getAgentBalanceAfter());
        dto.setCompanyBalanceBefore(history.getCompanyBalanceBefore());
        dto.setCompanyBalanceAfter(history.getCompanyBalanceAfter());
        dto.setHistoryType(history.getHistoryType() != null ? history.getHistoryType().name() : null);
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }

    /**
     * Validate API key
     */
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
        // Treat null expiry date as expired
        if (k.getDateOfExpiary() == null || k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
    }

    /**
     * Create error response with proper HTTP status code
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response with HTTP 200 status code
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
