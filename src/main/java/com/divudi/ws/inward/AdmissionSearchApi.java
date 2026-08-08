/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * General-purpose admission search — unlike ApiInward's /admissions
 * endpoints (a financial worklist scoped to unpaid/open admissions, capped
 * at 20 rows), this endpoint mirrors AdmissionController.searchAdmissions()
 * (the /inward/inpatient_search.xhtml backing query): it can list all
 * currently-admitted (not-discharged) patients, or search past or current
 * admissions by BHT, name, MRN/PHN, phone, or NIC — with no financial
 * scoping and no row cap (paginated instead).
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Path("inward/admissions")
@RequestScoped
public class AdmissionSearchApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @EJB
    private BillItemFacade billItemFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * GET /api/inward/admissions
     *
     * Query params (all optional):
     * status (AdmissionStatus name, default ADMITTED_BUT_NOT_DISCHARGED),
     * bhtNo, patientName, mrn, phone, nic, admissionTypeId, institutionId,
     * departmentId, fromDate, toDate (yyyy-MM-dd HH:mm:ss, both required
     * together), page (default 1), size (default 50, max 200).
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAdmissions() {
        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) {
            return errorResponse("Not a valid key", 401);
        }

        try {
            Map<String, String> q = new HashMap<>();
            for (String key : uriInfo.getQueryParameters().keySet()) {
                q.put(key, uriInfo.getQueryParameters().getFirst(key));
            }

            AdmissionStatus status = AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
            if (nonEmpty(q.get("status"))) {
                try {
                    status = AdmissionStatus.valueOf(q.get("status").trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid status. Valid values: ADMITTED_BUT_NOT_DISCHARGED, "
                            + "DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED, DISCHARGED_AND_FINAL_BILL_COMPLETED, "
                            + "ANY_STATUS", 400);
                }
            }

            Date fromDate = null;
            Date toDate = null;
            if (nonEmpty(q.get("fromDate")) || nonEmpty(q.get("toDate"))) {
                if (!nonEmpty(q.get("fromDate")) || !nonEmpty(q.get("toDate"))) {
                    return errorResponse("fromDate and toDate must both be supplied together.", 400);
                }
                try {
                    fromDate = new SimpleDateFormat(DATE_PATTERN).parse(q.get("fromDate").trim());
                    toDate = new SimpleDateFormat(DATE_PATTERN).parse(q.get("toDate").trim());
                } catch (Exception e) {
                    return errorResponse("Invalid fromDate/toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            Long admissionTypeId;
            Long institutionId;
            Long departmentId;
            try {
                admissionTypeId = parseRequiredLongParam(q.get("admissionTypeId"));
                institutionId = parseRequiredLongParam(q.get("institutionId"));
                departmentId = parseRequiredLongParam(q.get("departmentId"));
            } catch (NumberFormatException e) {
                return errorResponse("Invalid admissionTypeId/institutionId/departmentId value. Must be numeric.", 400);
            }

            int page = 1;
            if (nonEmpty(q.get("page"))) {
                try {
                    page = Math.max(1, Integer.parseInt(q.get("page").trim()));
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid page value.", 400);
                }
            }
            int size = DEFAULT_PAGE_SIZE;
            if (nonEmpty(q.get("size"))) {
                try {
                    size = Math.min(MAX_PAGE_SIZE, Math.max(1, Integer.parseInt(q.get("size").trim())));
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid size value.", 400);
                }
            }

            Map<String, Object> params = new HashMap<>();
            String where = buildWhereClause(params, status, q.get("bhtNo"), q.get("patientName"),
                    q.get("mrn"), q.get("phone"), q.get("nic"), admissionTypeId, institutionId,
                    departmentId, fromDate, toDate);

            long totalCount = getPatientEncounterFacade().findLongByJpql(
                    "select count(c) from Admission c " + where, params);

            int fromRecord = (page - 1) * size;
            int toRecord = fromRecord + size - 1;
            List<PatientEncounter> encounters = getPatientEncounterFacade().findByJpql(
                    "select c from Admission c " + where + " order by c.id desc", params, fromRecord, toRecord);

            List<Map<String, Object>> admissions = new ArrayList<>();
            for (PatientEncounter pe : encounters) {
                admissions.add(toResponseMap(pe));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("admissions", admissions);
            data.put("page", page);
            data.put("size", size);
            data.put("totalCount", totalCount);

            return successResponse(data);
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(AdmissionSearchApi.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Admission search failed", e);
            return errorResponse("An error occurred while searching admissions.", 500);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String buildWhereClause(Map<String, Object> params, AdmissionStatus status, String bhtNo,
            String patientName, String mrn, String phone, String nic, Long admissionTypeId,
            Long institutionId, Long departmentId, Date fromDate, Date toDate) {
        StringBuilder j = new StringBuilder("where c.retired=:ret and type(c)=:class ");
        params.put("ret", false);
        params.put("class", Admission.class);

        if (fromDate != null && toDate != null) {
            j.append(" and c.dateOfAdmission between :fd and :td ");
            params.put("fd", fromDate);
            params.put("td", toDate);
        }

        if (nonEmpty(bhtNo)) {
            j.append(" and c.bhtNo like :bht ");
            params.put("bht", "%" + bhtNo.trim() + "%");
        }

        if (nonEmpty(patientName)) {
            j.append(" and c.patient.person.name like :name ");
            params.put("name", "%" + patientName.trim() + "%");
        }

        if (nonEmpty(mrn)) {
            j.append(" and (c.patient.code =:mrn or c.patient.phn =:mrn) ");
            params.put("mrn", mrn.trim());
        }

        if (nonEmpty(phone)) {
            j.append(" and (c.patient.person.phone =:phone or c.patient.person.mobile =:phone"
                    + " or c.guardian.phone =:phone or c.guardian.mobile =:phone) ");
            params.put("phone", phone.trim());
        }

        if (nonEmpty(nic)) {
            j.append(" and c.patient.person.nic =:nic ");
            params.put("nic", nic.trim());
        }

        if (admissionTypeId != null) {
            j.append(" and c.admissionType.id =:atId ");
            params.put("atId", admissionTypeId);
        }

        if (institutionId != null) {
            j.append(" and c.institution.id =:insId ");
            params.put("insId", institutionId);
        }

        if (departmentId != null) {
            j.append(" and c.department.id =:deptId ");
            params.put("deptId", departmentId);
        }

        switch (status) {
            case ADMITTED_BUT_NOT_DISCHARGED:
                j.append(" and c.discharged=:dis ");
                params.put("dis", false);
                break;
            case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                j.append(" and c.discharged=:dis and c.paymentFinalized=:bf ");
                params.put("dis", true);
                params.put("bf", false);
                break;
            case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                j.append(" and c.discharged=:dis and c.paymentFinalized=:bf ");
                params.put("dis", true);
                params.put("bf", true);
                break;
            case ANY_STATUS:
            default:
                break;
        }

        return j.toString();
    }

    private Map<String, Object> toResponseMap(PatientEncounter pe) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientEncounterId", pe.getId());
        m.put("bhtNo", pe.getBhtNo());

        if (pe.getPatient() != null) {
            m.put("patientMrn", pe.getPatient().getPhn());
            m.put("patientCode", pe.getPatient().getCode());
            if (pe.getPatient().getPerson() != null) {
                m.put("patientName", pe.getPatient().getPerson().getName());
                m.put("patientPhone", pe.getPatient().getPerson().getPhone());
                m.put("patientMobile", pe.getPatient().getPerson().getMobile());
                m.put("patientNic", pe.getPatient().getPerson().getNic());
                m.put("patientAddress", pe.getPatient().getPerson().getAddress());
                m.put("patientArea", pe.getPatient().getPerson().getArea() != null
                        ? pe.getPatient().getPerson().getArea().getName() : null);
            }
        }

        m.put("referringDoctor", pe.getReferringDoctor() != null ? pe.getReferringDoctor().getName() : null);
        m.put("admissionType", pe.getAdmissionType() != null ? pe.getAdmissionType().getName() : null);
        m.put("institution", pe.getInstitution() != null ? pe.getInstitution().getName() : null);
        m.put("department", pe.getDepartment() != null ? pe.getDepartment().getName() : null);
        m.put("currentRoom", pe.getCurrentPatientRoom() != null ? pe.getCurrentPatientRoom().getName() : null);
        m.put("dateOfAdmission", pe.getDateOfAdmission());
        m.put("dateOfDischarge", pe.getDateOfDischarge());
        m.put("discharged", pe.getDischarged() != null && pe.getDischarged());
        m.put("paymentFinalized", pe.isPaymentFinalized());

        if (pe.getFinalBill() != null) {
            double netTotal = pe.getFinalBill().getNetTotal();
            double paidAmount = pe.getFinalBill().getPaidAmount() + fetchCreditPaymentTotal(pe);
            m.put("netTotal", netTotal);
            m.put("paidAmount", paidAmount);
            m.put("balance", netTotal - paidAmount);
        }

        return m;
    }

    private double fetchCreditPaymentTotal(PatientEncounter pe) {
        String sql = "SELECT sum(bi.netValue) FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter=:bhtno";
        Map<String, Object> m = new HashMap<>();
        m.put("bty", BillType.CashRecieveBill);
        m.put("bhtno", pe);
        return getBillItemFacade().findDoubleByJpql(sql, m);
    }

    private boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Parses an optional numeric filter param. Returns null when absent/blank;
     * throws NumberFormatException (rather than silently dropping the filter)
     * when present but not numeric.
     */
    private Long parseRequiredLongParam(String s) {
        if (!nonEmpty(s)) {
            return null;
        }
        return Long.parseLong(s.trim());
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }

        WebUser user = apiKey.getWebUser();
        if (user == null) {
            return null;
        }

        if (user.isRetired()) {
            return null;
        }

        if (!user.isActivated()) {
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }
}
