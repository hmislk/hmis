/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.channel;

import com.divudi.bean.channel.AgentReferenceBookController;
import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConsultantController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PersonInstitutionType;
import com.divudi.core.data.SessionStatusForOnlineBooking;
import com.divudi.core.data.Title;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.OnlineBooking;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.ServiceSessionFacade;
import com.divudi.core.facade.SessionInstanceFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.ChannelService;
import com.divudi.service.PatientService;
import com.divudi.service.WebSocketService;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * REST Web Service
 *
 * @author Dr M H Buddhika Ariyaratne
 */
@Path("channel")
@RequestScoped
public class ChannelApi {

    @Context
    private UriInfo context;

    @EJB
    StaffFacade staffFacade;
    @EJB
    private ItemFeeFacade ItemFeeFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private AgentHistoryFacade agentHistoryFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;

    @EJB
    private ChannelBean channelBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    private SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private SpecialityFacade specialityFacade;

    @Inject
    private BillBeanController billBeanController;
    @Inject
    AgentReferenceBookController AgentReferenceBookController;
    @Inject
    ConsultantController consultantController;
    @Inject
    ApiKeyController apiKeyController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    PatientService patientService;
    @EJB
    ChannelService channelService;

    private static final Logger LOGGER = Logger.getLogger(ChannelApi.class.getName());

    /**
     * Creates a new instance of Api
     */
    public ChannelApi() {
    }

    @POST
    @Path("/specializations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSpecializations(@Context HttpServletRequest requestContext, String requestBody) {
        JSONObject requestJson = new JSONObject(requestBody);
        String type = requestJson.getString("type");
        String bookingChannel = requestJson.getString("bookingChannel");
        String key = requestContext.getHeader("Token");
        JSONObject response = new JSONObject();
        if (!isValidKey(key)) {
            response = errorMessageNotValidKey();
            String json = response.toString();
            return Response.status(Response.Status.ACCEPTED).entity(response.toString()).build();
        }

        try {
            validateAndFetchAgency(null, bookingChannel);
        } catch (ValidationException e) {
            response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        List<Object[]> specializations = specilityList();
        Map<String, String> specialityMap = new HashMap<>();

        for (Object[] spec : specializations) {
            specialityMap.put(String.valueOf(spec[0]), String.valueOf(spec[1]));
        }

        JSONObject data = new JSONObject();
        data.put("specialityMap", specialityMap);

        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", data);

        return Response.status(Response.Status.ACCEPTED).entity(response.toString()).build();
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

    private JSONObject errorMessageNoData() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "No Data.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    public JSONObject commonFunctionToErrorResponse(String msg) {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("code", 406);
        jSONObject.put("type", "Error");
        jSONObject.put("message", msg);
        return jSONObject;
    }

    private JSONObject errorMessageNotValidKey() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid key.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidPathParameter() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid path parameter.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidInstitution() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid institution code.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject notValidId() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid code.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    @POST
    @Path("/hospitals")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getHospitalList(@Context HttpServletRequest requestContext, Map<String, String> requestBody) {
        // Extract the type and bookingChannel from the request body
        String type = requestBody.get("type");
        String bookingChannel = requestBody.get("bookingChannel");
        String key = requestContext.getHeader("Token");

        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }

        try {
            validateAndFetchAgency(null, bookingChannel);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        // Get the list of institutions from the controller
        List<Institution> institutions = channelService.findHospitals();
        if (institutions == null || institutions.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("No channeling centers available");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        // Prepare the response map to hold hospital details
        List<Map<String, String>> hosMap = new ArrayList<>();

        for (Institution hospital : institutions) {
            String hospitalName = hospital.getName() != null ? hospital.getName() : "";
            String hospitalId = hospital.getId() != null ? hospital.getId().toString() : "";
            String hospitalCode = hospital.getCode() != null ? hospital.getCode() : "";
            String hospitalCity = hospital.getAddress() != null ? hospital.getAddress() : "";

            // Create a map to hold individual hospital details
            Map<String, String> hospitalDetails = new HashMap<>();
            hospitalDetails.put("hospitalName", hospitalName);
            hospitalDetails.put("hospitalId", hospitalId);
            hospitalDetails.put("hospitalCode", hospitalCode);
            hospitalDetails.put("hospitalCity", hospitalCity);

            // Use hospitalId or hospitalCode as the key for the hosMap, assuming hospitalCode is unique
            hosMap.add(hospitalDetails);
        }

        // Prepare the response data
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("hosMap", hosMap);

        // Construct the response
        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", responseData);
        response.put("detailMessage", "Success");

        // Return the response
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/doctors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDoctorList(@Context HttpServletRequest requestContext, Map<String, String> requestBody) {

        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }

        String type = requestBody.get("type");
        String bookingChannel = requestBody.get("bookingChannel");

        // Prepare the resultMap to hold doctor details
        Map<String, Map<String, String>> resultMap = new HashMap<>();

        for (Consultant doctor : consultantController.getItems()) {
            String hosTown = doctor.getInstitution() != null && doctor.getInstitution().getAddress() != null ? doctor.getInstitution().getAddress() : "";
            String specName = doctor.getSpeciality() != null && doctor.getSpeciality().getName() != null ? doctor.getSpeciality().getName() : "";
            String hosName = doctor.getInstitution() != null && doctor.getInstitution().getName() != null ? doctor.getInstitution().getName() : "";
            String specializationId = doctor.getSpeciality() != null && doctor.getSpeciality().getId() != null ? doctor.getSpeciality().getId().toString() : "";
            String specialization = doctor.getSpeciality() != null && doctor.getSpeciality().getName() != null ? doctor.getSpeciality().getName() : "";

            String hosCode = doctor.getInstitution() != null && doctor.getInstitution().getCode() != null ? doctor.getInstitution().getCode() : "";
            String hosId = doctor.getInstitution() != null && doctor.getInstitution().getId() != null ? doctor.getInstitution().getId().toString() : "";
            String doctorNotes = doctor.getDescription() != null ? doctor.getDescription() : "";
            String docName = doctor.getPerson() != null && doctor.getPerson().getNameWithTitle() != null ? doctor.getPerson().getNameWithTitle() : "";
            String docId = doctor.getId() != null ? doctor.getId().toString() : "";

            // Create a map to hold individual doctor details
            Map<String, String> doctorDetails = new HashMap<>();
            doctorDetails.put("HosTown", hosTown);
            doctorDetails.put("SpecName", specName);
            doctorDetails.put("SpecId", specName);
            doctorDetails.put("HosName", hosName);
            doctorDetails.put("HosId", hosName);
            doctorDetails.put("Specialization", specialization);
            doctorDetails.put("SpecializationId", specializationId);
            doctorDetails.put("HosCode", hosCode);
            doctorDetails.put("HosId", hosId);
            doctorDetails.put("DoctorNotes", doctorNotes);
            doctorDetails.put("DocName", docName);
            doctorDetails.put("DoctorId", docId);

            // Use doctorNo as the key for the resultMap
            resultMap.put(docId, doctorDetails);
        }

        // Construct the response JSON
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("resultMap", resultMap);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", responseData);
        response.put("detailMessage", "Success");

        // Return the response
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/doctorAvailability")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDoctorAvailabilityList(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) throws ParseException {
        // Extract parameters from the request body
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }
        String type = (String) requestBody.get("type");
        String agencyCode = (String) requestBody.get("bookingChannel");
        String hosIdStr = (String) requestBody.get("hosID");
        String specIDStr = (String) requestBody.get("specID");
        String dateStr = (String) requestBody.get("date");
        String name = (String) requestBody.get("name");

        try {
            validateAndFetchAgency(null, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Long hosId = channelService.checkSafeParseLong(hosIdStr);
        Long specId = channelService.checkSafeParseLong(specIDStr);

        List<Institution> hospitals = channelService.findInstitutionFromId(hosId);
        if (hospitals == null || hospitals.isEmpty()) {
            if (hosId != null && !hosId.toString().isEmpty()) {
                JSONObject response = commonFunctionToErrorResponse("No Hospital available with that Id.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        List<Speciality> specialities = channelService.findSpecilityFromId(specId);

        if (specialities == null || specialities.isEmpty()) {
            if (specId != null && !specId.toString().isEmpty()) {
                JSONObject response = commonFunctionToErrorResponse("No Speciality available with that Id.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        List<Doctor> doctorList = channelService.findDoctorsFromName(name, null);

        if (doctorList == null || doctorList.isEmpty()) {
            if (name != null && !name.isEmpty()) {
                JSONObject response = commonFunctionToErrorResponse("No Doctor available with that name.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        // Convert dateStr to Date
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = null;

        try {
            date = formatter.parse(dateStr);
        } catch (ParseException e) {
            date = null;
        }

        // Prepare the resultMap to hold doctor details
        List<Map<String, String>> resultMap = new ArrayList<>();
        List<SessionInstance> sessionInstances = channelService.findSessionInstance(hospitals, specialities, doctorList, date);

        if (sessionInstances == null || sessionInstances.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("No Data for this criterias.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }
        SimpleDateFormat dayFormat = new SimpleDateFormat("E");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        //Long additionalProp1 = 1L;
        for (SessionInstance si : sessionInstances) {

            Map<String, String> doctorDetails = new HashMap<>();
            doctorDetails.put("AppDay", dayFormat.format(si.getSessionDate()));
            doctorDetails.put("HosTown", si.getInstitution().getAddress());
            doctorDetails.put("SpecName", si.getOriginatingSession().getStaff().getSpeciality().getName());
            doctorDetails.put("HosName", si.getOriginatingSession().getInstitution().getName());
            doctorDetails.put("SpecializationId", si.getOriginatingSession().getStaff().getSpeciality().getId().toString());
            doctorDetails.put("HosCode", si.getInstitution().getCode());
            doctorDetails.put("AppDate", dateFormat.format(si.getSessionDate()));
            doctorDetails.put("DocName", si.getOriginatingSession().getStaff().getPerson().getNameWithTitle());
            doctorDetails.put("DoctorNotes", si.getOriginatingSession().getSpecialNotice() == null ? "" : si.getOriginatingSession().getSpecialNotice());
            doctorDetails.put("DoctorNo", si.getStaff().getId().toString());
            doctorDetails.put("SessionStart", timeFormat.format(si.getOriginatingSession().getStartingTime()));
            resultMap.add(doctorDetails);
        }

        // Construct the response JSON
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("resultMap", resultMap);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", responseData);
        response.put("detailMessage", "Success");

        // Return the response
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @GET
    @Path("/searchData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchDoctors(
            @Context HttpServletRequest requestContext,
            @QueryParam("hosID") Integer hosID,
            @QueryParam("docNo") Integer docNo,
            @QueryParam("docName") String docName,
            @QueryParam("specID") Integer specID,
            @QueryParam("offset") Integer offset,
            @QueryParam("page") Integer page,
            @QueryParam("sessionDate") String sessionDate) {

        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }

        if ((docName == null || docName.isEmpty()) && docNo == null) {
            JSONObject json = commonFunctionToErrorResponse("Both Doctor name and Doctor number are missing.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json.toString()).build();
        }

        // Search logic and build the JSON response
        JSONObject results = searchDoctor(hosID, docNo, docName, specID, offset, page, sessionDate);

        try {
            if (results.get("code") != null) {
                if ((Integer) results.get("code") == 406) {
                    return Response.status(Response.Status.NOT_ACCEPTABLE).entity(results.toString()).build();
                }
            }
        } catch (Exception e) {
        }

        JSONArray resultArray = (JSONArray) results.get("data");
        if (resultArray.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("No data for this criteria.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        // Constructing the detailed response
        JSONObject response = new JSONObject();
        response.put("code", 200);
        response.put("message", "OK");
        response.put("data", results);
        response.put("detailMessage", "Success");

        return Response.status(Response.Status.OK)
                .entity(response.toString())
                .build();
    }

    @POST
    @Path("/doctorSessions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDoctorSessions(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {

        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }
        String hospitalId = (String) requestBody.get("hosID");
        String doctorId = (String) requestBody.get("docNo");
        System.out.println(hospitalId + " " + doctorId);

        Long hospitalIdLong = null;
        if (hospitalId != null && !hospitalId.isEmpty()) {
            try {
                hospitalIdLong = Long.parseLong(hospitalId);
            } catch (Exception e) {
                JSONObject json = commonFunctionToErrorResponse("Invalid hospital id format");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json.toString()).build();
            }
        }

        String agencyCode = (String) requestBody.get("bookingChannel");

        try {
            validateAndFetchAgency(null, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        List<Institution> hospitals = channelService.findInstitutionFromId(hospitalIdLong);

        if (hospitals == null || hospitals.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Invalid hospital id.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Long doctorIdLong = null;
        if (doctorId != null && !doctorId.isEmpty()) {
            try {
                doctorIdLong = Long.parseLong(doctorId);
            } catch (Exception e) {
                JSONObject json = commonFunctionToErrorResponse("Invalid doctor No.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json.toString()).build();
            }

        }

        List<Doctor> doctorList = channelService.findDoctorsFromName(null, doctorIdLong);
        if (doctorIdLong != null && !doctorIdLong.toString().isEmpty()) {
            if (doctorList == null || doctorList.isEmpty()) {
                JSONObject json = commonFunctionToErrorResponse("No doctor available with that doctor No.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json.toString()).build();
            }
        }

        List<SessionInstance> sessions = channelService.findSessionInstanceForDoctorSessions(hospitals, null, doctorList, null);
        if (sessions == null || sessions.isEmpty()) {
            JSONObject json = commonFunctionToErrorResponse("No data for this criteria.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json.toString()).build();
        }

        List<Object> sessionData = new ArrayList<>();
        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        for (SessionInstance s : sessions) {

            if (s.getOriginatingSession().getTotal() == 0) {
                continue;
            }

            String sessionStatus = SessionStatusForOnlineBooking.Available.toString();
            if (s.isDoctorHoliday()) {
                sessionStatus = SessionStatusForOnlineBooking.Holiday.toString();
            } else if (!s.isAcceptOnlineBookings()) {
                sessionStatus = SessionStatusForOnlineBooking.Hold.toString();
            } else if (channelService.isFullyBookedSession(s)) {
                sessionStatus = SessionStatusForOnlineBooking.Full.toString();
            }
            Map<String, Object> session = new HashMap<>();

            Map foreignFees = channelService.getForeignFeesForDoctorAndInstitutionFromServiceSession(s.getOriginatingSession());
            Map localFees = channelService.getLocalFeesForDoctorAndInstitutionFromServiceSession(s.getOriginatingSession());

            session.put("sessionID", s.getId().intValue());
            session.put("appTimeInterval", null);
            session.put("hosFee", localFees != null ? localFees.get("hosFee") : 0);
            session.put("docName", s.getStaff().getPerson().getNameWithTitle());
            session.put("docNo", s.getStaff().getId());
            session.put("docForeignFee", foreignFees != null ? foreignFees.get("docForeignFee") : 0);
            session.put("nextNo", channelService.nextAvailableAppoinmentNumberForSession(s).get("nextNumber"));
            session.put("hosId", s.getInstitution().getId().toString());
            session.put("remarks", s.getOriginatingSession().getSpecialNotice() == null ? "" : s.getOriginatingSession().getSpecialNotice());
            session.put("vatDocCharge", null);
            session.put("docFee", localFees != null ? localFees.get("docFee") : 0);
            session.put("hosName", s.getInstitution().getName());
            session.put("startTime", forTime.format(s.getStartingTime()));
            session.put("vatHosCharge", null);
            session.put("amount", s.getOriginatingSession().getTotal());
            session.put("hosForeignFee", foreignFees != null ? foreignFees.get("hosForeignFee") : 0);
            session.put("vatDocForeignCharge", null);
            session.put("specID", s.getStaff().getSpeciality().getId().toString());
            session.put("maxPatient", s.getMaxNo());
            session.put("activePatient", channelService.nextAvailableAppoinmentNumberForSession(s).get("activePatients"));
            session.put("foreignAmount", s.getOriginatingSession().getTotalForForeigner());
            session.put("appDate", forDate.format(s.getSessionDate()));
            session.put("vatHosForeignCharge", null);
            session.put("appDay", forDay.format(s.getSessionDate()));
            session.put("status", sessionStatus);

            sessionData.add(session);
        }

        Map<String, Object> sessionResults = new HashMap<>();
        sessionResults.put("result", sessionData);

        Map<String, Integer> pageData = new HashMap();
        pageData.put("pageNo", 0);
        pageData.put("offset", 0);
        pageData.put("pages", 0);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", sessionResults);
        response.put("page", pageData);
        response.put("detailMessage", "Succeess");

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

    @POST
    @Path("/doctorSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDoctorSession(@Context HttpServletRequest requestContext, Map<String, String> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }
        String sessionIdString = requestBody.get("sessionID");

        if (sessionIdString == null || sessionIdString.isEmpty()) {
            JSONObject responseError = commonFunctionToErrorResponse("Invalid session id");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseError.toString()).build();
        }
        long sessionId;
        try {
            sessionId = Integer.parseInt(requestBody.get("sessionID"));
        } catch (Exception e) {
            JSONObject responseError = commonFunctionToErrorResponse("Invalid format of Session id.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseError.toString()).build();
        }

        String agencyCode = requestBody.get("bookingChannel");

        try {
            validateAndFetchAgency(null, agencyCode);
        } catch (ValidationException e) {
            JSONObject responseError = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseError.toString()).build();
        }

        SessionInstance session = channelService.findActiveChannelSession(sessionId);

        if (session == null) {
            JSONObject responseError = commonFunctionToErrorResponse("NO session available for online booking!");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseError.toString()).build();
        }

        String sessionStatus = SessionStatusForOnlineBooking.Available.toString();
        if (session.isDoctorHoliday()) {
            sessionStatus = SessionStatusForOnlineBooking.Holiday.toString();
        } else if (!session.isAcceptOnlineBookings()) {
            sessionStatus = SessionStatusForOnlineBooking.Hold.toString();
        } else if (channelService.isFullyBookedSession(session)) {
            sessionStatus = SessionStatusForOnlineBooking.Full.toString();
        }

        String remark = "Session will have on time";
        if (session.isCancelled()) {
            remark = "session is cancelled.";
        } else if (session.isCompleted()) {
            remark = "session is completed now.";
        } else if (session.isStarted()) {
            remark = "session is ongoing now.";
        }

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        Map hosAndDocForeignFees = channelService.getForeignFeesForDoctorAndInstitutionFromServiceSession(session.getOriginatingSession());
        Map hosAndDocLocalFees = channelService.getLocalFeesForDoctorAndInstitutionFromServiceSession(session.getOriginatingSession());

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionID", session.getId().intValue());
        sessionData.put("appTimeInterval", null);
        sessionData.put("hosFee", hosAndDocLocalFees != null ? hosAndDocLocalFees.get("hosFee") : (Object) 0);
        sessionData.put("docName", session.getStaff().getPerson().getNameWithTitle());
        sessionData.put("docNo", session.getStaff().getId().toString());
        sessionData.put("docForeignFee", hosAndDocForeignFees != null ? hosAndDocForeignFees.get("docForeignFee") : 0);
        sessionData.put("nextNo", channelService.nextAvailableAppoinmentNumberForSession(session).get("nextNumber"));
        sessionData.put("hosId", session.getInstitution().getId().toString());
        sessionData.put("remarks", remark);
        sessionData.put("vatDocCharge", null);
        sessionData.put("docFee", hosAndDocLocalFees != null ? hosAndDocLocalFees.get("docFee") : (Object) 0);
        sessionData.put("hosName", session.getInstitution().getName());
        sessionData.put("startTime", forTime.format(session.getSessionTime()));
        sessionData.put("vatHosCharge", null);
        sessionData.put("amount", session.getOriginatingSession().getTotal());
        sessionData.put("hosForeignFee", hosAndDocForeignFees != null ? hosAndDocForeignFees.get("hosForeignFee") : 0);
        sessionData.put("vatDocForeignCharge", null);
        sessionData.put("specID", session.getOriginatingSession().getStaff().getSpeciality().getId().toString());
        sessionData.put("maxPatient", session.getMaxNo());
        sessionData.put("activePatient", channelService.nextAvailableAppoinmentNumberForSession(session).get("activePatients"));
        sessionData.put("foreignAmount", session.getOriginatingSession().getTotalForForeigner());
        sessionData.put("appDate", forDate.format(session.getSessionDate()));
        sessionData.put("vatHosForeignCharge", null);
        sessionData.put("appDay", forDay.format(session.getSessionDate()));
        sessionData.put("status", sessionStatus);

        Map<String, Object> allSessionData = new HashMap<>();
        allSessionData.put("result", sessionData);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", allSessionData);
        response.put("detailMessage", "Success");

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

    private static class ValidationException extends RuntimeException {

        private final String field;

        ValidationException(String field, String message) {
            super(message);
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    private boolean validateForeignStatus(String status) {
        if (Integer.parseInt(status) == 1) {
            return true;
        } else if (Integer.parseInt(status) == 0) {
            return false;
        } else {
            throw new ValidationException("Foreigner Status : ", "Invalid patient status(Foreign/Local) data.");
        }
    }

    private void validatePhoneNumber(String patientPhoneNo) {
        if (patientPhoneNo == null || patientPhoneNo.isEmpty()) {
            throw new ValidationException("Phone Number : ", "Patient Phone number is mandotary.");
        } else if (patientPhoneNo.length() < 10) {
            throw new ValidationException("Phone Number : ", "Phone number must be 10 digits.");
        }

    }

    private SessionInstance validateAndFetchAppoinmentSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new ValidationException("SessionID", " Missing SessionId field");
        }

        Long sessionIdLong = 0L;

        try {
            sessionIdLong = Long.parseLong(sessionId);
        } catch (Exception e) {
            throw new ValidationException("SessionID", " Invalid Session ID" + sessionId);
        }

        SessionInstance session = channelService.findActiveChannelSession(Long.parseLong(sessionId));
        if (session == null) {
            //JSONObject response = commonFunctionToErrorResponse("Session id is invalid");
            throw new ValidationException("SessionID", " No session associate with this id" + sessionId);
        }

        if (session.getMaxNo() != 0 && configOptionApplicationController.getBooleanValueByKey("Limited appoinments session can't get appoinement more than max amount.")) {
            if (session.getBookedPatientCount() != null) {
                int maxNo = session.getMaxNo();
                long bookedPatientCount = session.getBookedPatientCount();
                long totalPatientCount;

                List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(session.getReserveNumbers());
                if (false) {
                    bookedPatientCount = bookedPatientCount;
                } else {
                    bookedPatientCount = bookedPatientCount + reservedNumbers.size();
                }

                if (session.getCancelPatientCount() != null) {
                    long canceledPatientCount = session.getCancelPatientCount();
                    totalPatientCount = bookedPatientCount - canceledPatientCount;
                } else {
                    totalPatientCount = bookedPatientCount;
                }
                if (maxNo <= totalPatientCount) {
                    //JSONObject response = commonFunctionToErrorResponse("No of appoinments has been reach to max appoinemtns amount. ");
                    throw new ValidationException(null, " No of appoinments has been reach to max appoinemtns amount.");
                }
            }
        }

        if (session.isCancelled()) {
            //JSONObject response = commonFunctionToErrorResponse("Sorry!. Session is calcelled due to unavoidable reason.");
            throw new ValidationException(null, " Sorry!. Session is calcelled due to unavoidable reason.");
        }

        if (session.isCompleted()) {
            //JSONObject response = commonFunctionToErrorResponse("Sorry!. You are Late. Session is already finished.");
            throw new ValidationException(null, " Sorry!. You are Late. Session is already finished.");
        }

        String msgForStartSession = null;
        if (session.isStarted()) {
            msgForStartSession = "Session is starting now. Please complete booking and visit quickly";
        }

        return session;
    }

    private Institution validateAndFetchAgency(String agencyName, String agencyCode) {
        if ((agencyName == null || agencyName.isEmpty()) && (agencyCode == null || agencyCode.isEmpty())) {
            throw new ValidationException("Agency attributes : ", "Missing agency code and name. Check fields");
        }

        Institution creditCompany = channelService.findCreditCompany(agencyCode, agencyName, InstitutionType.OnlineBookingAgent);

        if (creditCompany == null) {
            throw new ValidationException("Agency : ", "Your agency is not registered in the hospital system.");
        } else if (creditCompany != null && creditCompany.isInactive()) {
            throw new ValidationException("Agency : ", "Your agency is deactivated from the hospital system.");
        }

        return creditCompany;
    }

    private void validateReferenceNumber(String refNo, Institution agency) {
        List<Bill> billList = channelService.findBillFromRefNo(refNo, agency, BillClassType.BilledBill);

        if (billList != null && !billList.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Duplicate Ref No occured");
            throw new ValidationException("Client Ref No : ", "Duplicate Ref No occured. Please use unique ref no for each bookings");
        }
    }

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBooking(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = errorMessageNotValidKey();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }
        Map<String, String> patientDetails = (Map<String, String>) requestBody.get("patient");
        String sessionId = requestBody.get("sessionID").toString();
        Map<String, String> payment = (Map<String, String>) requestBody.get("payment");

        String agencyCode = payment.get("paymentChannel");
        try {
            validateAndFetchAgency(null, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (patientDetails == null || patientDetails.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Patien details not in the request");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        SessionInstance session = null;

        try {
            session = validateAndFetchAppoinmentSession(sessionId);
        } catch (ValidationException e) {
            String message = e.getField() == null ? e.getMessage() : e.getField() + e.getMessage();
            JSONObject response = commonFunctionToErrorResponse(message);
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (session.isDoctorHoliday()) {
            JSONObject response = commonFunctionToErrorResponse("Doctor is on Holiday.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        } else if (!session.isAcceptOnlineBookings()) {
            JSONObject response = commonFunctionToErrorResponse("Session is hold for online bookings");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        } else if (channelService.isFullyBookedSession(session)) {
            JSONObject response = commonFunctionToErrorResponse("Session is Full. Can't add online bookings");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (patientDetails.isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("Code", "406");
            response.put("type", "error");
            response.put("message", "No patient details");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        boolean isForeigner = false;
        String patientPhoneNo = patientDetails.get("teleNo");
        String patientName = patientDetails.get("patientName");
        String patientType = patientDetails.get("foreign");
        String patientTitle = patientDetails.get("title");
        String nic = patientDetails.get("nid");
        String clientsReferanceNo = patientDetails.get("clientRefNumber");
        String email = patientDetails.get("email");
        String address = patientDetails.get("address");

        try {
            isForeigner = validateForeignStatus(patientType);
            validatePhoneNumber(patientPhoneNo);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (clientsReferanceNo == null || clientsReferanceNo.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Invalid Ref No");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (patientTitle == null || patientTitle.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Patient title is mandotary.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        String agencyName = payment.get("paymentMode");
        String bookingForm = payment.get("channelFrom");

        Institution bookingAgency;
        try {
            bookingAgency = validateAndFetchAgency(agencyName, agencyCode);
            validateReferenceNumber(clientsReferanceNo, bookingAgency);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        String ipAddress = requestContext.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = requestContext.getRemoteAddr();
        }

        OnlineBooking newBooking = new OnlineBooking();
        newBooking.setPatientName(patientName);
        newBooking.setPhoneNo(patientPhoneNo);
        newBooking.setTitle(patientTitle);
        newBooking.setNic(nic);
        newBooking.setReferenceNo(clientsReferanceNo);
        newBooking.setForeignStatus(isForeigner);
        newBooking.setAgency(bookingAgency);
        newBooking.setRequestIp(ipAddress);
        newBooking.setCreatedAt(new Date());
        newBooking.setEmail(email);
        newBooking.setAddress(address);

        Bill bill = channelService.addToReserveAgentBookingThroughApi(false, newBooking, session, clientsReferanceNo, null, bookingAgency);

        try {
            WebSocketService.broadcastToSessions("Online Temporary Booking Added - " + session.getId());
        } catch (Exception e) {
            LOGGER.severe("Web socket communication error at temporary booking" + e.getMessage());
        }

        if (bill == null) {
            JSONObject response = commonFunctionToErrorResponse("Can't create booking. Session is not confirmed yet.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        Map<String, Object> sessionDetailsResponse = new HashMap<>();
        sessionDetailsResponse.put("hosId", session.getInstitution().getId().toString());
        sessionDetailsResponse.put("amount", bill.getNetTotal());
        sessionDetailsResponse.put("appTimeInterval", null);
        sessionDetailsResponse.put("hosAmount", bill.getHospitalFee());
        sessionDetailsResponse.put("docAmount", bill.getStaffFee());
        sessionDetailsResponse.put("docId", session.getStaff().getId().toString());
        sessionDetailsResponse.put("theDate", forDate.format(session.getSessionDate()));
        sessionDetailsResponse.put("theDay", forDay.format(session.getSessionDate()));
        sessionDetailsResponse.put("startTime", forTime.format(session.getSessionTime()));
        sessionDetailsResponse.put("arriveTime", null);
        sessionDetailsResponse.put("hosLocation", session.getInstitution().getAddress());
        sessionDetailsResponse.put("sessionStarted", session.isStarted());

        OnlineBooking savedBooking = bill.getOnlineBooking();

        Map<String, Object> patientDetailsResponse = new HashMap<>();
        patientDetailsResponse.put("member", null);
        patientDetailsResponse.put("needSMS", savedBooking.isNeedSms());
        patientDetailsResponse.put("nsr", savedBooking.isNsr());
        patientDetailsResponse.put("foreign", savedBooking.isForeignStatus());
        patientDetailsResponse.put("teleNo", savedBooking.getPhoneNo());
        patientDetailsResponse.put("title", savedBooking.getTitle());
        patientDetailsResponse.put("patientName", savedBooking.getPatientName());
        patientDetailsResponse.put("nid", savedBooking.getNic());
        patientDetailsResponse.put("memberId", null);
        patientDetailsResponse.put("patientFullName", savedBooking.getTitle() + ". " + savedBooking.getPatientName());
        patientDetailsResponse.put("patientFullNameWithMobile", savedBooking.getTitle() + ". " + savedBooking.getPatientName() + " " + savedBooking.getPhoneNo());

        Map<String, Object> priceDetailsResponse = new HashMap<>();
        priceDetailsResponse.put("totalAmount", bill.getNetTotal());
        priceDetailsResponse.put("nsrFee", null);
        priceDetailsResponse.put("charge", savedBooking.getOnlineBookingPayment());
        priceDetailsResponse.put("hosCharge", bill.getHospitalFee());
        priceDetailsResponse.put("hosChargeWithoutVat", 0);
        priceDetailsResponse.put("docCharge", bill.getStaffFee());
        priceDetailsResponse.put("echCharge", 0);
        priceDetailsResponse.put("smsCharge", 0);
        priceDetailsResponse.put("nbtCharge", 0);
        priceDetailsResponse.put("vatCharge", 0);
        priceDetailsResponse.put("agentCharge", 0);
        priceDetailsResponse.put("docVatPercentage", 0);
        priceDetailsResponse.put("hosVatPercentage", 0);
        priceDetailsResponse.put("vatPercentage", 0);
        priceDetailsResponse.put("doctorVatCharge", 0);
        priceDetailsResponse.put("hospitalVatCharge", 0);
        priceDetailsResponse.put("locationPrice", 0);

        Map<String, Object> paymentDetailsResponse = new HashMap<>();
        paymentDetailsResponse.put("paymentMode", bill.getCreditCompany().getName());
        paymentDetailsResponse.put("paymentChannel", agencyCode);
        paymentDetailsResponse.put("branchCode", null);
        paymentDetailsResponse.put("seqNo", null);

        Map<String, Object> apoinmentDetailsResponse = new HashMap<>();
        apoinmentDetailsResponse.put("refNo", clientsReferanceNo);
        apoinmentDetailsResponse.put("patientNo", Integer.parseInt(bill.getSingleBillSession().getSerialNoStr()));
        apoinmentDetailsResponse.put("allPatientNo", channelService.nextAvailableAppoinmentNumberForSession(session).get("nextNumber"));
        apoinmentDetailsResponse.put("showPno", true);
        apoinmentDetailsResponse.put("showTime", true);
        apoinmentDetailsResponse.put("chRoom", session.getRoomNo());
        apoinmentDetailsResponse.put("timeInterval", null);
        apoinmentDetailsResponse.put("sessionDetails", sessionDetailsResponse);
        apoinmentDetailsResponse.put("patient", patientDetailsResponse);
        apoinmentDetailsResponse.put("price", priceDetailsResponse);
        apoinmentDetailsResponse.put("payment", paymentDetailsResponse);
        apoinmentDetailsResponse.put("status", "unpaid");

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", apoinmentDetailsResponse);

        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editBooking(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }

        String clientsReferanceNo = (String) requestBody.get("refNo");
        Map<String, String> patientDetailsFromRequest = (Map<String, String>) requestBody.get("patient");
        String patientPhoneNo = patientDetailsFromRequest.get("teleNo");
        String patientName = patientDetailsFromRequest.get("patientName");
        String patientNic = patientDetailsFromRequest.get("nid");
        String title = patientDetailsFromRequest.get("title");
        Map<String, String> paymentDetails = (Map<String, String>) requestBody.get("payment");
        String agencyCode = paymentDetails.get("paymentChannel");
        String agencyName = paymentDetails.get("paymentMode");
        String email = patientDetailsFromRequest.get("email");
        String address = patientDetailsFromRequest.get("address");

        if (clientsReferanceNo == null || clientsReferanceNo.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Ref no is missing.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (patientName == null || patientName.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Patient name is missing.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (title == null || title.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Patient title is missing.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (patientNic == null || patientNic.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("Patient name is missing.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Institution creditCompany = null;
        try {
            validatePhoneNumber(patientPhoneNo);
            creditCompany = validateAndFetchAgency(agencyName, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        OnlineBooking bookingDetails = channelService.findOnlineBookingFromRefNo(clientsReferanceNo, false, creditCompany);

        if (bookingDetails == null) {
            OnlineBooking retiredBooking = channelService.findOnlineBookingFromRefNo(clientsReferanceNo, true, creditCompany);

            if (retiredBooking != null) {
                JSONObject response = commonFunctionToErrorResponse("Your appoinment is retired due to time out. Please readd appoinemnt : " + clientsReferanceNo);
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            } else {
                JSONObject response = commonFunctionToErrorResponse("No Appoinment available for the RefNo : " + clientsReferanceNo);
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        Bill temporarySavedBill = channelService.findBillFromOnlineBooking(bookingDetails);

        try {
            validateBillForCompleteBooking(temporarySavedBill);
            validateChannelingSession(temporarySavedBill.getSingleBillSession().getSessionInstance());
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        OnlineBooking editedBooking = channelService.editOnlineBooking(bookingDetails, patientPhoneNo, title, patientName, patientNic, email, address);

        Bill temporaryBill = channelService.findBillFromOnlineBooking(editedBooking);
        SessionInstance session = temporaryBill.getSingleBillSession().getSessionInstance();

        try {
            WebSocketService.broadcastToSessions("Online Booking Edited - " + session.getId());
        } catch (Exception e) {
            LOGGER.severe("Web socket communication error at edit booking" + e.getMessage());
        }

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        Map<String, Object> sessionDetailsResponse = new HashMap<>();
        sessionDetailsResponse.put("hosId", session.getInstitution().getId().toString());
        sessionDetailsResponse.put("amount", temporaryBill.getNetTotal());
        sessionDetailsResponse.put("appTimeInterval", null);
        sessionDetailsResponse.put("hosAmount", temporaryBill.getHospitalFee());
        sessionDetailsResponse.put("docAmount", temporaryBill.getStaffFee());
        sessionDetailsResponse.put("docId", temporaryBill.getStaff().getId().toString());
        sessionDetailsResponse.put("theDate", forDate.format(session.getSessionDate()));
        sessionDetailsResponse.put("theDay", forDay.format(session.getSessionDate()));
        sessionDetailsResponse.put("startTime", forTime.format(session.getSessionTime()));
        sessionDetailsResponse.put("arriveTime", null);
        sessionDetailsResponse.put("hosLocation", session.getInstitution().getAddress());
        sessionDetailsResponse.put("sessionStarted", session.isStarted());

        Map<String, Object> patientDetailsResponse = new HashMap<>();
        patientDetailsResponse.put("member", null);
        patientDetailsResponse.put("needSMS", editedBooking.isNeedSms());
        patientDetailsResponse.put("nsr", editedBooking.isNsr());
        patientDetailsResponse.put("foreign", editedBooking.isForeignStatus());
        patientDetailsResponse.put("teleNo", editedBooking.getPhoneNo());
        patientDetailsResponse.put("title", editedBooking.getTitle());
        patientDetailsResponse.put("patientName", editedBooking.getPatientName());
        patientDetailsResponse.put("nid", editedBooking.getNic());
        patientDetailsResponse.put("memberId", editedBooking.getId());
        patientDetailsResponse.put("patientFullName", editedBooking.getTitle() + " " + editedBooking.getPatientName());
        patientDetailsResponse.put("patientFullNameWithMobile", editedBooking.getTitle() + " " + editedBooking.getPatientName() + " " + editedBooking.getPhoneNo());

        Map<String, Object> priceDetailsResponse = new HashMap<>();
        priceDetailsResponse.put("totalAmount", temporaryBill.getNetTotal());
        priceDetailsResponse.put("nsrFee", null);
        priceDetailsResponse.put("charge", editedBooking.getOnlineBookingPayment());
        priceDetailsResponse.put("hosCharge", temporaryBill.getHospitalFee());
        priceDetailsResponse.put("hosChargeWithoutVat", 0);
        priceDetailsResponse.put("docCharge", temporaryBill.getStaffFee());
        priceDetailsResponse.put("echCharge", 0);
        priceDetailsResponse.put("smsCharge", 0);
        priceDetailsResponse.put("nbtCharge", 0);
        priceDetailsResponse.put("vatCharge", 0);
        priceDetailsResponse.put("agentCharge", 0);
        priceDetailsResponse.put("docVatPercentage", 0);
        priceDetailsResponse.put("hosVatPercentage", 0);
        priceDetailsResponse.put("vatPercentage", 0);
        priceDetailsResponse.put("doctorVatCharge", 0);
        priceDetailsResponse.put("hospitalVatCharge", 0);
        priceDetailsResponse.put("locationPrice", 0);

        Map<String, Object> paymentDetailsResponse = new HashMap<>();
        paymentDetailsResponse.put("paymentMode", editedBooking.getAgency().getName());
        paymentDetailsResponse.put("paymentChannel", editedBooking.getAgency().getCode());
        paymentDetailsResponse.put("branchCode", null);
        paymentDetailsResponse.put("seqNo", null);

        Map<String, Object> apoinmentDetailsResponse = new HashMap<>();
        apoinmentDetailsResponse.put("refNo", clientsReferanceNo);
        apoinmentDetailsResponse.put("patientNo", Integer.parseInt(temporaryBill.getSingleBillSession().getSerialNoStr()));
        apoinmentDetailsResponse.put("allPatientNo", session.getNextAvailableAppointmentNumber() != null ? session.getNextAvailableAppointmentNumber().intValue() - 1 : 0);
        apoinmentDetailsResponse.put("showPno", true);
        apoinmentDetailsResponse.put("showTime", true);
        apoinmentDetailsResponse.put("chRoom", session.getRoomNo());
        apoinmentDetailsResponse.put("timeInterval", null);
        apoinmentDetailsResponse.put("sessionDetails", sessionDetailsResponse);
        apoinmentDetailsResponse.put("patient", patientDetailsResponse);
        apoinmentDetailsResponse.put("price", priceDetailsResponse);
        apoinmentDetailsResponse.put("payment", paymentDetailsResponse);
        apoinmentDetailsResponse.put("status", "unpaid");

        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", apoinmentDetailsResponse);

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

    private void validateBillForCompleteBooking(Bill bill) {
        if (bill.isCancelled()) {
            throw new ValidationException("Appoinment : ", "Appoinment is already cancelled. Cant complete the booking.");
        }
        if (bill.isRefunded()) {
            throw new ValidationException("Appoinment : ", "Appoinment is already refunded. Cant complete the booking.");
        }

        if (bill.isPaid()) {
            throw new ValidationException("Appoinment : ", "Appoinment Booking for the ref no is already completed.");
        }
    }

    private void validateChannelingSession(SessionInstance session) {
        if (session.isCompleted()) {
            throw new ValidationException("Channel : ", "Appoinment session is already finished now.");
        }
        if (session.isCancelled()) {
            throw new ValidationException("Channel : ", "Channel session is Cancelled from the Hospital. Please contact hospital for more info.");
        }
        if (session.isRetired()) {
            throw new ValidationException("Channel : ", "Channel session is not available.");
        }
    }

    @POST
    @Path("/complete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeBooking(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.UNAUTHORIZED).entity(responseError.toString()).build();
        }

        String clientsReferanceNo = (String) requestBody.get("refNo");
        Integer statusId = null;
        try {
            statusId = ((Number) requestBody.get("statusId")).intValue();
        } catch (Exception e) {
            statusId = null;
        }

        if (statusId == null) {
            JSONObject response = commonFunctionToErrorResponse("Request statusId is not available or error occupied. Please check.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (statusId == 0) {
            JSONObject response = commonFunctionToErrorResponse("Booking Completion is failed based on statusID(0).");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (clientsReferanceNo.isEmpty() || clientsReferanceNo == null) {
            JSONObject response = commonFunctionToErrorResponse("Invalid Ref No");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }
        Map<String, String> paymentDetails = (Map<String, String>) requestBody.get("payment");
        String agencyCode = paymentDetails.get("paymentChannel");
        String agencyName = paymentDetails.get("paymentMode");
        String agentChargeString = (String) requestBody.get("price");

        double agencyCharge = 0;

        try {
            agencyCharge = Double.parseDouble(agentChargeString);
        } catch (Exception e) {
            JSONObject response = commonFunctionToErrorResponse("There is a error in the payment amount. Please check price field.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Institution creditCompany = null;
        try {
            creditCompany = validateAndFetchAgency(agencyName, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        OnlineBooking bookingData = channelService.findOnlineBookingFromRefNo(clientsReferanceNo, false, creditCompany);

        if (bookingData == null) {
            OnlineBooking retiredBooking = channelService.findOnlineBookingFromRefNo(clientsReferanceNo, true, creditCompany);

            if (retiredBooking != null) {
                JSONObject response = commonFunctionToErrorResponse("Your appoinment is retired due to time out. Please re-place the appoinemnt : " + clientsReferanceNo);
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            } else {
                JSONObject response = commonFunctionToErrorResponse("No Appoinment available for the RefNo : " + clientsReferanceNo);
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        //Bill temporarySavedBill = bookingData.getBill();
        Bill temporarySavedBill = channelService.findBillFromOnlineBooking(bookingData);

        try {
            validateBillForCompleteBooking(temporarySavedBill);
            validateChannelingSession(temporarySavedBill.getSingleBillSession().getSessionInstance());
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }
        if (temporarySavedBill.getSingleBillSession().getSessionInstance().isDoctorHoliday()) {
            JSONObject response = commonFunctionToErrorResponse("Doctor is on Holiday.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        } else if (!temporarySavedBill.getSingleBillSession().getSessionInstance().isAcceptOnlineBookings()) {
            JSONObject response = commonFunctionToErrorResponse("Session is hold for online bookings");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        } else if (channelService.isFullyBookedSession(temporarySavedBill.getSingleBillSession().getSessionInstance())) {
            JSONObject response = commonFunctionToErrorResponse("Session is Full. Can't add online bookings");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Bill completedBill = channelService.settleOnlineAgentInitialBooking(temporarySavedBill.getSingleBillSession(), clientsReferanceNo, agencyCharge);

        OnlineBooking bookingDetails = completedBill.getReferenceBill().getOnlineBooking();
        SessionInstance session = completedBill.getSingleBillSession().getSessionInstance();

        try {
            WebSocketService.broadcastToSessions("Online Booking Completed - " + session.getId());
        } catch (Exception e) {
            LOGGER.severe("Web socket communication error at complete booking " + e.getMessage());
        }

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        Map<String, Object> appoinment = new HashMap<>();
        appoinment.put("refNo", bookingDetails.getReferenceNo());
        appoinment.put("patientNo", Integer.parseInt(completedBill.getSingleBillSession().getSerialNoStr()));
        appoinment.put("allPatientNo", channelService.nextAvailableAppoinmentNumberForSession(session).get("nextNumber"));
        appoinment.put("showPno", true);
        appoinment.put("showTime", true);
        appoinment.put("givenTime", null);
        appoinment.put("chRoom", session.getRoomNo());
        appoinment.put("timeInterval", null);
        appoinment.put("status", OnlineBookingStatus.ACTIVE);
        appoinment.put("isnew", null);

        Map<String, Object> sessionDetails = new HashMap<>();
        Item i = completedBill.getSingleBillSession().getItem();

        sessionDetails.put("hosId", completedBill.getToInstitution().getId());
        sessionDetails.put("docname", session.getStaff().getPerson().getNameWithTitle());
        sessionDetails.put("amount", bookingDetails.getNetTotalForOnlineBooking());
        sessionDetails.put("hosAmount", completedBill.getHospitalFee());
        sessionDetails.put("docAmount", completedBill.getStaffFee());
        sessionDetails.put("specialization", i.getStaff().getSpeciality().getName());
        sessionDetails.put("theDate", forDate.format(session.getSessionDate()));
        sessionDetails.put("theDay", forDay.format(session.getSessionDate()));
        sessionDetails.put("startTime", forTime.format(session.getStartingTime()));
        sessionDetails.put("hosLocation", session.getInstitution().getAddress());
        sessionDetails.put("hosName", session.getInstitution().getName());
        sessionDetails.put("sessionStarted", session.isStarted());

        Map<String, Object> patientDetails = new HashMap<>();
        patientDetails.put("title", bookingDetails.getTitle());
        patientDetails.put("member", null);
        patientDetails.put("needSMS", bookingDetails.isNeedSms());
        patientDetails.put("nsr", bookingDetails.isNsr());
        patientDetails.put("foreign", bookingDetails.isForeignStatus());
        patientDetails.put("teleNo", bookingDetails.getPhoneNo());
        patientDetails.put("patientName", bookingDetails.getPatientName());
        patientDetails.put("patientFullName", bookingDetails.getTitle() + " " + bookingDetails.getPatientName());
        patientDetails.put("nid", bookingDetails.getNic());

        Map<String, Object> priceDetails = new HashMap<>();
        priceDetails.put("totalAmount", bookingDetails.getNetTotalForOnlineBooking());
        priceDetails.put("docCharge", completedBill.getStaffFee());
        priceDetails.put("hosCharge", completedBill.getHospitalFee());

        Map<String, Object> paymentDetailsForResponse = new HashMap<>();
        paymentDetailsForResponse.put("paymentMode", completedBill.getCreditCompany().getName());
        paymentDetailsForResponse.put("paymentChannel", completedBill.getCreditCompany().getCode());
        paymentDetailsForResponse.put("seqNo", null);

        appoinment.put("sessionDetails", sessionDetails);
        appoinment.put("patient", patientDetails);
        appoinment.put("price", priceDetails);
        appoinment.put("payment", paymentDetailsForResponse);

        Map response = new HashMap();
        response.put("data", appoinment);
        response.put("message", "Booking completed");
        response.put("code", "202");
        response.put("detailMessage", "Your booking is setted");

        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/channelHistoryList")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppointmentList(@Context HttpServletRequest requestContext, Map<String, String> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.ACCEPTED).entity(responseError.toString()).build();
        }

        String fromDate = requestBody.get("fromDate");
        String toDate = requestBody.get("toDate");
        String agencyName = requestBody.get("paymentMode");
        String agencyCode = requestBody.get("paymentChannel");

        Institution agency = null;

        try {
            agency = validateAndFetchAgency(agencyName, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Date startDate = null;
        Date endDate = null;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            startDate = formatter.parse(fromDate);
            endDate = formatter.parse(toDate);
        } catch (Exception e) {
            JSONObject response = commonFunctionToErrorResponse("Check date format again.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        List<OnlineBooking> bookingList = channelService.fetchOnlineBookingWithingDateRange(startDate, endDate, agency);

        if (bookingList == null || bookingList.isEmpty()) {
            JSONObject response = commonFunctionToErrorResponse("NO Appoinements for that Date Range");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (OnlineBooking ob : bookingList) {
            Bill bill = ob.getBill();

            Map<String, Object> mapDetail = new HashMap<>();
            mapDetail.put("DoctorName", bill.getStaff().getPerson().getNameWithTitle());
            mapDetail.put("PatientName", ob.getPatientName());
            mapDetail.put("HosTelephone", bill.getToInstitution().getPhone());
            mapDetail.put("NicNumber", ob.getNic());
            mapDetail.put("HosName", bill.getToInstitution().getName());
            mapDetail.put("RefNo", bill.getAgentRefNo());
            mapDetail.put("HosLocation", bill.getToInstitution().getAddress());
            mapDetail.put("AppointmentNumber", Integer.parseInt(bill.getSingleBillSession().getSerialNoStr()));
            result.add(mapDetail);
        }

        Map response = new HashMap<>();
        response.put("data", result);
        response.put("message", "Accepted");
        response.put("code", 202);
        response.put("detailMessage", "All the appoinment details listed within " + fromDate + " to " + toDate);

        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/channelHistoryByRef")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookingDetails(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.ACCEPTED).entity(responseError.toString()).build();
        }

        String refNo = (String) requestBody.get("refNo");
        String agencyCode = (String) requestBody.get("bookingChannel");

        if (refNo.isEmpty() || refNo == null) {
            JSONObject response = commonFunctionToErrorResponse("Invalid Ref No");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Institution creditCompany = null;

        try {
            creditCompany = validateAndFetchAgency(null, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        OnlineBooking bookingDetails = channelService.findOnlineBookingFromRefNo(refNo, false, creditCompany);

        if (bookingDetails == null) {
            JSONObject response = commonFunctionToErrorResponse("No bill reference with Ref No. : " + refNo);
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Bill bookingBill = channelService.findBillFromOnlineBooking(bookingDetails);

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        String bookingStatus = "Active";
        if (bookingDetails.getOnlineBookingStatus() == OnlineBookingStatus.PATIENT_CANCELED) {
            bookingStatus = "Patient Canceled";
        } else if (bookingDetails.getOnlineBookingStatus() == OnlineBookingStatus.DOCTOR_CANCELED) {
            bookingStatus = "Doctor Canceled";
        } else if (bookingDetails.getOnlineBookingStatus() == OnlineBookingStatus.COMPLETED) {
            bookingStatus = "Completed";
        } else if (bookingDetails.getOnlineBookingStatus() == OnlineBookingStatus.ABSENT) {
            bookingStatus = "Absent";
        }

        Map<String, Object> appoinment = new HashMap<>();
        appoinment.put("refNo", bookingBill.getAgentRefNo());
        appoinment.put("patientNo", bookingBill.getSingleBillSession().getSerialNo());
        appoinment.put("allPatientNo", (Integer) channelService.nextAvailableAppoinmentNumberForSession(bookingBill.getSingleBillSession().getSessionInstance()).get("nextNumber") - 1);
        appoinment.put("showPno", true);
        appoinment.put("showTime", true);
        appoinment.put("chRoom", bookingBill.getSingleBillSession().getSessionInstance().getRoomNo());
        appoinment.put("timeInterval", null);
        appoinment.put("status", bookingStatus);

        SessionInstance session = bookingBill.getSingleBillSession().getSessionInstance();

        String sessionStatus = SessionStatusForOnlineBooking.Available.toString();
        if (session.isDoctorHoliday()) {
            sessionStatus = SessionStatusForOnlineBooking.Holiday.toString();
        } else if (session.isCompleted()) {
            sessionStatus = SessionStatusForOnlineBooking.Ended.toString();
        } else if (session.isCancelled()) {
            sessionStatus = SessionStatusForOnlineBooking.Cancelled.toString();
        } else if (!session.isAcceptOnlineBookings()) {
            sessionStatus = SessionStatusForOnlineBooking.Hold.toString();
        } else if (channelService.isFullyBookedSession(session)) {
            sessionStatus = SessionStatusForOnlineBooking.Full.toString();
        }

        Map<String, Object> sessionDetails = new HashMap<>();
        Item i = bookingBill.getSingleBillSession().getItem();
        sessionDetails.put("hosId", i.getInstitution().getId().toString());
        sessionDetails.put("docname", session.getOriginatingSession().getStaff().getPerson().getNameWithTitle());
        sessionDetails.put("amount", bookingBill.getNetTotal());
        sessionDetails.put("hosAmount", bookingBill.getHospitalFee());
        sessionDetails.put("docAmount", bookingBill.getStaffFee());
        sessionDetails.put("specialization", i.getStaff().getSpeciality().getName());
        sessionDetails.put("theDate", forDate.format(session.getSessionDate()));
        sessionDetails.put("theDay", forDay.format(session.getSessionDate()));
        sessionDetails.put("startTime", forTime.format(session.getStartingTime()));
        sessionDetails.put("hosLocation", session.getInstitution().getAddress());
        sessionDetails.put("hosName", session.getInstitution().getName());
        sessionDetails.put("sessionStarted", session.isStarted());
        sessionDetails.put("status", sessionStatus);

        Map<String, Object> patientDetails = new HashMap<>();
        patientDetails.put("title", bookingDetails.getTitle());
        patientDetails.put("foreign", bookingDetails.isForeignStatus());
        patientDetails.put("teleNo", bookingDetails.getPhoneNo());
        patientDetails.put("patientName", bookingDetails.getPatientName());
        patientDetails.put("patientFullName", bookingDetails.getTitle() + " " + bookingDetails.getPatientName());
        patientDetails.put("nid", bookingDetails.getNic());
        patientDetails.put("memberId", null);
        patientDetails.put("member", null);
        patientDetails.put("needSMS", bookingDetails.isNeedSms());
        patientDetails.put("nsr", bookingDetails.isNsr());

        Map<String, Object> priceDetails = new HashMap<>();
        priceDetails.put("totalAmount", bookingBill.getTotal());
        priceDetails.put("docCharge", bookingBill.getStaffFee());
        priceDetails.put("hosCharge", bookingBill.getHospitalFee());
        priceDetails.put("charge", bookingDetails.getOnlineBookingPayment());
        priceDetails.put("nsrFee", 0);
        priceDetails.put("hosChargeWithoutVat", 0);
        priceDetails.put("echCharge", 0);
        priceDetails.put("smsCharge", 0);
        priceDetails.put("nbtCharge", 0);
        priceDetails.put("vatCharge", 0);
        priceDetails.put("agentCharge", 0);
        priceDetails.put("docVatPercentage", 0);
        priceDetails.put("hosVatPercentage", 0);
        priceDetails.put("vatPercentage", 0);

        Map<String, Object> paymentDetails = new HashMap<>();
        paymentDetails.put("paymentMode", bookingDetails.getAgency().getName());
        paymentDetails.put("paymentChannel", bookingDetails.getAgency().getCode());

        appoinment.put("sessionDetails", sessionDetails);
        appoinment.put("patient", patientDetails);
        appoinment.put("price", priceDetails);
        appoinment.put("payment", paymentDetails);

        Map response = new HashMap();
        response.put("data", appoinment);
        response.put("code", "202");
        response.put("message", "Booking details for ref No: " + refNo);
        response.put("detailMessage", "Success");

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

    @POST
    @Path("/cancellation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelBooking(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            JSONObject responseError = new JSONObject();
            responseError = errorMessageNotValidKey();
            String json = responseError.toString();
            return Response.status(Response.Status.ACCEPTED).entity(responseError.toString()).build();
        }

        String refNo = (String) requestBody.get("refNo");
        Map paymentDetails = (Map) requestBody.get("payment");

        String agencyCode = (String) paymentDetails.get("paymentChannel");
        String agencyName = (String) paymentDetails.get("paymentMode");

        if (refNo.isEmpty() || refNo == null) {
            JSONObject response = commonFunctionToErrorResponse("Invalid Ref No");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        Institution agency = null;

        try {
            agency = validateAndFetchAgency(agencyName, agencyCode);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        OnlineBooking bookingData = channelService.findOnlineBookingFromRefNo(refNo, false, agency);

        if (bookingData == null) {
            JSONObject response = commonFunctionToErrorResponse("No Appoinment available for the RefNo : " + refNo);
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        } else {
            if (bookingData.getOnlineBookingStatus() == OnlineBookingStatus.PENDING) {
                JSONObject response = commonFunctionToErrorResponse("Temporary Bookings no need to cancel. They are cancelled automatically if not completed.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            } else if (bookingData.getOnlineBookingStatus() == OnlineBookingStatus.PATIENT_CANCELED || bookingData.getOnlineBookingStatus() == OnlineBookingStatus.DOCTOR_CANCELED) {
                JSONObject response = commonFunctionToErrorResponse("Appoinment available for : " + refNo + " is already cancelled");
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
            }
        }

        Bill completedSaveBill = channelService.findBillFromOnlineBooking(bookingData).getPaidBill();

        try {
            validateChannelingSession(completedSaveBill.getSingleBillSession().getSessionInstance());
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Enable Online Bookings cancellation after session is started.", true)) {
            JSONObject response = commonFunctionToErrorResponse("Can't cancel the appoinment due to hospital restriction. Please contact hospital.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        BillSession bs = channelService.cancelBookingBill(completedSaveBill, bookingData);

        SessionInstance session = bs.getSessionInstance();

        try {
            WebSocketService.broadcastToSessions("Online Booking Cancelled - " + session.getId());
        } catch (Exception e) {
            LOGGER.severe("Web socket communication error at cancel booking " + e.getMessage());
        }

        String sessionStatus = SessionStatusForOnlineBooking.Available.toString();
        if (session.isDoctorHoliday()) {
            sessionStatus = SessionStatusForOnlineBooking.Holiday.toString();
        } else if (session.isCompleted()) {
            sessionStatus = SessionStatusForOnlineBooking.Ended.toString();
        } else if (session.isCancelled()) {
            sessionStatus = SessionStatusForOnlineBooking.Cancelled.toString();
        } else if (!session.isAcceptOnlineBookings()) {
            sessionStatus = SessionStatusForOnlineBooking.Hold.toString();
        } else if (channelService.isFullyBookedSession(session)) {
            sessionStatus = SessionStatusForOnlineBooking.Full.toString();
        }

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        Map<String, Object> appoinment = new HashMap<>();
        appoinment.put("refNo", completedSaveBill.getAgentRefNo());
        appoinment.put("patientNo", completedSaveBill.getSingleBillSession().getSerialNo());
        appoinment.put("allPatientNo", (Integer) channelService.nextAvailableAppoinmentNumberForSession(session).get("nextNumber") - 1);
        appoinment.put("showPno", true);
        appoinment.put("showTime", true);
        appoinment.put("chRoom", completedSaveBill.getSingleBillSession().getSessionInstance().getRoomNo());
        appoinment.put("timeInterval", null);
        appoinment.put("status", "Patient-canceled");

        Map<String, Object> sessionDetails = new HashMap<>();
        Item i = completedSaveBill.getSingleBillSession().getItem();
        sessionDetails.put("hosId", i.getInstitution().getId().toString());
        sessionDetails.put("docname", i.getStaff().getPerson().getNameWithInitials());
        sessionDetails.put("amount", completedSaveBill.getNetTotal());
        sessionDetails.put("hosAmount", completedSaveBill.getHospitalFee());
        sessionDetails.put("docAmount", completedSaveBill.getStaffFee());
        sessionDetails.put("specialization", i.getStaff().getSpeciality().getName());
        sessionDetails.put("theDate", forDate.format(session.getSessionDate()));
        sessionDetails.put("theDay", forDay.format(session.getSessionDate()));
        sessionDetails.put("startTime", forTime.format(session.getStartingTime()));
        sessionDetails.put("hosLocation", session.getInstitution().getAddress());
        sessionDetails.put("hosName", session.getInstitution().getName());
        sessionDetails.put("sessionStarted", session.isStarted());
        sessionDetails.put("status", sessionStatus);

        Map<String, Object> patientDetails = new HashMap<>();
        patientDetails.put("titile", bookingData.getTitle());
        patientDetails.put("foreign", bookingData.isForeignStatus());
        patientDetails.put("teleNo", bookingData.getPhoneNo());
        patientDetails.put("patientName", bookingData.getPatientName());
        patientDetails.put("patientFullName", bookingData.getTitle() + " " + bookingData.getPatientName());
        patientDetails.put("nid", bookingData.getNic());
        patientDetails.put("memberId", null);
        patientDetails.put("member", null);
        patientDetails.put("needSMS", bookingData.isNeedSms());
        patientDetails.put("nsr", bookingData.isNsr());

        Map<String, Object> priceDetails = new HashMap<>();
        priceDetails.put("totalAmount", completedSaveBill.getTotal());
        priceDetails.put("docCharge", completedSaveBill.getStaffFee());
        priceDetails.put("hosCharge", completedSaveBill.getHospitalFee());
        priceDetails.put("charge", bookingData.getOnlineBookingPayment());
        priceDetails.put("nsrFee", 0);
        priceDetails.put("hosChargeWithoutVat", 0);
        priceDetails.put("echCharge", 0);
        priceDetails.put("smsCharge", 0);
        priceDetails.put("nbtCharge", 0);
        priceDetails.put("vatCharge", 0);
        priceDetails.put("agentCharge", 0);
        priceDetails.put("docVatPercentage", 0);
        priceDetails.put("hosVatPercentage", 0);
        priceDetails.put("vatPercentage", 0);

        Map<String, Object> paymentDetailsResponse = new HashMap<>();
        paymentDetailsResponse.put("paymentMode", bs.getBill().getCreditCompany().getName());
        paymentDetailsResponse.put("paymentChannel", bs.getBill().getCreditCompany().getCode());

        appoinment.put("sessionDetails", sessionDetails);
        appoinment.put("patient", patientDetails);
        appoinment.put("price", priceDetails);
        appoinment.put("payment", paymentDetails);

        Map response = new HashMap();
        response.put("code", "202");
        response.put("data", appoinment);
        response.put("message", "Accepted");
        response.put("detailMessage", "Booking details for ref No: " + refNo);

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

    @POST
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testAPI() {
        System.out.println("API test method called");

        // Create a simple JSON object to return as response
        JSONObject responseObject = new JSONObject();
        responseObject.put("status", "success");
        responseObject.put("message", "API is reachable and responding successfully.");

        // Return a 200 OK response with the JSON object
        return Response.status(Response.Status.OK)
                .entity(responseObject.toString())
                .build();
    }

    private JSONObject searchDoctor(
            Integer hosID,
            Integer docNo,
            String docName,
            Integer specID,
            Integer offset,
            Integer page,
            String sessionDate) {
        // Parse the sessionDate
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = null;

        if (sessionDate != null && !sessionDate.isEmpty()) {
            try {
                date = dateFormat.parse(sessionDate);
            } catch (ParseException e) {
                return commonFunctionToErrorResponse("Invalid Date format.");

            }
        }
        List<Institution> hospialList = null;
        Institution hospital = null;

        if (hosID != null) {
            hospital = institutionFacade.find(hosID.longValue());
            if (hospital != null) {
                hospialList = Arrays.asList(hospital);
            }

        } else if (hosID == null) {
            hospialList = channelService.findHospitals();
        }

        Long specIdLong = null;
        Speciality speciality = null;

        if (specID != null) {
            specIdLong = specID.longValue();
            speciality = specialityFacade.find(specIdLong);
        }

        Long docNoLong = null;

        if (docNo != null) {
            docNoLong = Long.valueOf(docNo);
        }

        List<Doctor> doctorList = channelService.findDoctorsFromName(docName, docNoLong);
        if (doctorList == null || doctorList.isEmpty()) {
            return commonFunctionToErrorResponse("No doctor with given parameters.");
        }

        // Create a list of specialities if needed (assuming single speciality search)
        List<Speciality> specialities = (speciality != null) ? Arrays.asList(speciality) : channelService.findAllSpecilities();

        // Call the method to find session instances
        List<SessionInstance> sessions = channelService.findSessionInstance(hospialList, specialities, doctorList, date);

        SimpleDateFormat forDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat forTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat forDay = new SimpleDateFormat("E");

        JSONArray resultArray = new JSONArray();

        Map<String, List<SessionInstance>> groupedByInstitution = sessions.stream()
                .collect(Collectors.groupingBy(session -> session.getInstitution().getId().toString()));

        for (Map.Entry<String, List<SessionInstance>> entry : groupedByInstitution.entrySet()) {
            JSONObject hospitalObject = new JSONObject();
            JSONArray doctorArray = new JSONArray();
            hospitalObject.put("hosId", entry.getValue().get(0).getInstitution().getId());
            hospitalObject.put("displayName", entry.getValue().get(0).getInstitution().getName());

            for (SessionInstance session : entry.getValue()) {
                List<Institution> hospitalForNextSession = new ArrayList<>();
                hospitalForNextSession.add(session.getInstitution());
                SessionInstance nextSession = channelService.findNextSessionInstance(hospitalForNextSession, specialities, doctorList, session.getSessionDate());
                JSONObject doctor = new JSONObject();
                doctor.put("docNo", session.getOriginatingSession().getStaff().getPerson().getNameWithTitle() != null ? session.getOriginatingSession().getStaff().getId().toString() : "N/A");
                doctor.put("displayName", session.getOriginatingSession().getStaff().getPerson().getNameWithTitle() != null ? session.getOriginatingSession().getStaff().getPerson().getNameWithTitle() : "N/A");
                doctor.put("title", session.getOriginatingSession().getStaff().getPerson().getTitle() != null ? session.getOriginatingSession().getStaff().getPerson().getTitle().toString() : "N/A");
                doctor.put("nextAvailableDate", nextSession == null ? "Not yet shedule next session by the Hospital." : forDate.format(nextSession.getSessionDate()) + " at " + forTime.format(nextSession.getStartingTime()) + " on " + forDay.format(nextSession.getSessionDate()));
                doctorArray.put(doctor);
            }

            hospitalObject.put("doctor", doctorArray);
            resultArray.put(hospitalObject);
        }

        JSONObject results = new JSONObject();
        //results.put("totalCount", sessions.size()); // Total count of sessions
        results.put("data", resultArray);

        return results;
    }

    // Inner class to encapsulate search results
    private class SearchResults {

        private List<DoctorDetail> doctors;
        private Integer totalCount;

        // Assume getters and setters are here
    }

    // Inner class for Doctor details
    private class DoctorDetail {

        private String name;
        private String speciality;
        private String hospital;

        // Assume getters and setters are here
    }

    @GET
    @Path("/json")
    @Produces("application/json")
    public String getJson() {
        //TODO return proper representation object
        return "<html><h:body><h1>Hello, World!!</h:body></h1></html>";
    }

    @GET
    @Path("/json2")
    @Produces("text/html")
    public String getJson2() {
        //TODO return proper representation object
        return "<html><h:body><h1>Hello, World 2!!</h:body></h1></html>";
    }

    @GET
    @Path("/doctors")
    @Produces("application/json")
    public String getDoctors() {
        List<Object[]> consultants = doctorsListAll();
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        if (!consultants.isEmpty()) {
            for (Object[] con : consultants) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("doc_id", con[0]);
                jSONObject.put("doc_name", con[1]);
                jSONObject.put("doc_specility", con[2]);
                jSONObject.put("doc_code", con[3]);
                array.put(jSONObject);
            }
            jSONObjectOut.put("doctors", array);
            jSONObjectOut.put("error", "0");
            jSONObjectOut.put("error_description", "");
        } else {
            jSONObjectOut.put("doctors", array);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "No Data.");
        }

        String json = jSONObjectOut.toString();
        return json;
    }

    @POST
    @Path("/getApiKey")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiKeyWithRenewal(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String bookingChannel = (String) requestBody.get("bookingChannel");

        try {
            validateAndFetchAgency(null, bookingChannel);
        } catch (ValidationException e) {
            JSONObject response = commonFunctionToErrorResponse(e.getField() + e.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(response.toString()).build();
        }

        String userName = (String) requestBody.get("userName");
        String password = (String) requestBody.get("password");

        WebUser user = channelService.checkUserCredentialForApi(userName, password);

        if (user == null) {
            JSONObject response = commonFunctionToErrorResponse("Authentication failed");
            return Response.status(Response.Status.UNAUTHORIZED).entity(response.toString()).build();
        }

        List<ApiKey> apiKeyList = channelService.listApiKeysForUser(user);

        if (apiKeyList != null && !apiKeyList.isEmpty()) {
            Random rand = new Random();
            int size = apiKeyList.size();
            ApiKey apiKey = apiKeyList.get(rand.nextInt(size));
            Map<String, String> response = new HashMap();
            response.put("key", apiKey.getKeyType().toString());
            response.put("value", apiKey.getKeyValue());
            response.put("userName", userName);
            response.put("expiryDate", apiKey.getDateOfExpiary().toString());

            return Response.status(Response.Status.ACCEPTED).entity(response).build();
        }

        ApiKey apiKey = channelService.createNewApiKeyForApiResponse(user);
        Map<String, String> response = new HashMap<>();
        response.put("key", apiKey.getKeyType().toString());
        response.put("value", apiKey.getKeyValue());
        response.put("userName", userName);

        return Response.status(Response.Status.ACCEPTED).entity(response).build();

    }

//    /sessions/DR0001
    @GET
    @Path("/doctors/{doc_code}")
    @Produces("application/json")
    public String getDoctor(@PathParam("doc_code") String doc_code) {
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        try {
//            Long d_id = Long.parseLong(doc_id);
            List<Object[]> consultants = doctorsList(doc_code, null);
            if (!consultants.isEmpty()) {
                for (Object[] con : consultants) {
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("doc_id", con[0]);
                    jSONObject.put("doc_name", con[1].toString().toUpperCase());
                    jSONObject.put("doc_specility", con[2].toString().toUpperCase());
                    jSONObject.put("doc_code", con[3]);
                    array.put(jSONObject);
                }
                jSONObjectOut.put("doctors", array);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("doctors", array);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }

        } catch (Exception e) {
            jSONObjectOut.put("doctors", array);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
        String json = jSONObjectOut.toString();
        return json;
    }

//    @GET
//    @Path("/users/{userid}")
//    @Produces(MediaType.APPLICATION_XML)
//    public User getUser(@PathParam("userid") int userid) {
//        return userDao.getUser(userid);
//    }
//    /sessions/DR0001
    @GET
    @Path("/sessions/{doc_code}")
    @Produces("application/json")
    public String getSessions(@PathParam("doc_code") String doc_code) {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray array1 = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();

        try {
            List<ServiceSession> sessions = sessionsList(doc_code, null, null);
            if (!sessions.isEmpty()) {
                for (ServiceSession s : sessions) {
                    object = new JSONObject();
                    object.put("session_id", s.getId());
                    object.put("session_date", CommonFunctions.formatDate(s.getSessionDate(), "YYYY-MM-dd"));
                    object.put("session_starting_time", CommonFunctions.getTimeFormat24(s.getStartingTime()));
                    object.put("session_ending_time", CommonFunctions.getTimeFormat24(s.getEndingTime()));
                    object.put("session_max_no", s.getMaxNo());
                    object.put("session_is_refundable", s.isRefundable());
                    object.put("session_duration", s.getDuration());
                    object.put("session_room_no", s.getRoomNo());
//                    object.put("session_current_app_no", channelBean.getBillSessionsCount((long) s.getId(), (Date) s.getSessionDate()) + 1);
                    object.put("session_fee", CommonFunctions.getDouble((double) fetchLocalFee((long) s.getId(), PaymentMethod.Agent, false)));
                    object.put("session_fee_vat", CommonFunctions.getDouble((double) fetchLocalFeeVat((long) s.getId(), PaymentMethod.Agent, false)));
                    object.put("session_forign_fee", CommonFunctions.getDouble((double) fetchLocalFee((long) s.getId(), PaymentMethod.Agent, true)));
                    object.put("session_forign_fee_vat", CommonFunctions.getDouble((double) fetchLocalFeeVat((long) s.getId(), PaymentMethod.Agent, true)));
                    object.put("session_is_leaved", s.isDeactivated());
                    array.put(object);
//            s[10]=fetchLocalFee((long)s[0], PaymentMethod.Agent, true);
                }
                jSONObjectOut.put("session", array);
                jSONObjectOut.put("session_dates", sessionsDatesList(doc_code, null, null));
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("session", sessions);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            jSONObjectOut.put("session", object);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
//        try {
//            List<Object[]> sessions = sessionsList(doc_code, null, null);
//            if (!sessions.isEmpty()) {
//                for (Object[] s : sessions) {
//                    object.put("session_id", s[0]);
//                    object.put("session_date", getCommonController().getDateFormat((Date) s[1]));
//                    object.put("session_starting_time", getCommonController().getTimeFormat24((Date) s[2]));
//                    object.put("session_ending_time", getCommonController().getTimeFormat24((Date) s[3]));
//                    object.put("session_max_no", s[4]);
//                    object.put("session_is_refundable", s[5]);
//                    object.put("session_duration", s[6]);
//                    object.put("session_room_no", s[7]);
//                    object.put("session_current_app_no", channelBean.getBillSessionsCount((long) s[0], (Date) s[1]));
//                    object.put("session_fee", getCommonController().getDouble((double) fetchLocalFee((long) s[0], PaymentMethod.Agent, false)));
//                    object.put("session_is_leaved", s[10]);
//                    //// // System.out.println("s.length = " + s.length);
//                    array.put(object);
////            s[10]=fetchLocalFee((long)s[0], PaymentMethod.Agent, true);
//                }
//                jSONObjectOut.put("session", array);
//                jSONObjectOut.put("session_dates", sessionsDatesList(doc_code, null, null));
//                jSONObjectOut.put("error", "0");
//                jSONObjectOut.put("error_description", "");
//            } else {
//                jSONObjectOut.put("session", sessions);
//                jSONObjectOut.put("error", "1");
//                jSONObjectOut.put("error_description", "No Data.");
//            }
//        } catch (Exception e) {
//            jSONObjectOut.put("session", object);
//            jSONObjectOut.put("error", "1");
//            jSONObjectOut.put("error_description", "Invalid Argument.");
//        }
//        try {
//            List<Object[]> sessions = sessionsList(doc_code, null, null);
//            if (!sessions.isEmpty()) {
//                for (Object[] s : sessions) {
//                    object.put("session_id", s[0]);
//                    object.put("session_date", getCommonController().getDateFormat((Date) s[1]));
//                    object.put("session_starting_time", getCommonController().getTimeFormat24((Date) s[2]));
//                    object.put("session_ending_time", getCommonController().getTimeFormat24((Date) s[3]));
//                    object.put("session_max_no", s[4]);
//                    object.put("session_is_refundable", s[5]);
//                    object.put("session_duration", s[6]);
//                    object.put("session_room_no", s[7]);
//                    object.put("session_current_app_no", channelBean.getBillSessionsCount((long) s[0], (Date) s[1]));
//                    object.put("session_fee", getCommonController().getDouble((double) fetchLocalFee((long) s[0], PaymentMethod.Agent, false)));
//                    object.put("session_is_leaved", s[10]);
//                    //// // System.out.println("s.length = " + s.length);
//                    array.put(object);
////            s[10]=fetchLocalFee((long)s[0], PaymentMethod.Agent, true);
//                }
//                jSONObjectOut.put("session", array);
//                jSONObjectOut.put("session_dates", sessionsDatesList(doc_code, null, null));
//                jSONObjectOut.put("error", "0");
//                jSONObjectOut.put("error_description", "");
//            } else {
//                jSONObjectOut.put("session", sessions);
//                jSONObjectOut.put("error", "1");
//                jSONObjectOut.put("error_description", "No Data.");
//            }
//        } catch (Exception e) {
//            jSONObjectOut.put("session", object);
//            jSONObjectOut.put("error", "1");
//            jSONObjectOut.put("error_description", "Invalid Argument.");
//        }

        String json = jSONObjectOut.toString();
//        String json = new Gson().toJson(sessions);
        return json;
    }

//    /makeBooking/Dushan/0788044212/1/******/DR0001/20385287/451
    @GET
    @Path("/makeBooking/{name}/{phone}/{hospital_id}/{session_id}/{doc_code}/{agent_id}/{agent_reference_no}")
    @Produces("application/json")
    public String makeBooking(@PathParam("name") String name, @PathParam("phone") String phone,
            @PathParam("hospital_id") String hospital_id, @PathParam("session_id") String session_id, @PathParam("doc_code") String doc_code,
            @PathParam("agent_id") String agent_id, @PathParam("agent_reference_no") String agent_reference_no) {
        JSONArray bill = new JSONArray();
        String json = new String();
        List<Object[]> list = new ArrayList<>();
        JSONObject jSONObjectOut = new JSONObject();
        Long h_id = Long.parseLong(hospital_id);
        Long ss_id = Long.parseLong(session_id);
        Long a_id = Long.parseLong(agent_id);
//        Long ar_no = Long.parseLong(agent_reference_no);

        try {

            String s = fetchErrors(name, phone, doc_code, ss_id, a_id, agent_reference_no, "0");
//            String s = fetchErrors(name, phone, doc_code, ss_id, a_id, ar_no);
//            //// // System.out.println("s = " + s);
            if (!"".equals(s)) {
                jSONObjectOut.put("make_booking", s);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
                json = jSONObjectOut.toString();
                return json;
            }
            ServiceSession ss = serviceSessionFacade.find(ss_id);
            if (ss != null) {
                //For Settle bill
                ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.setItemFees(fetchFee(ss.getOriginatingSession()));
                //For Settle bill
                ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            } else {
                jSONObjectOut.put("make_booking", "Invalid Session Id");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
                return jSONObjectOut.toString();
            }
//            //// // System.out.println("ss = " + ss);
            Bill b;
            b = saveBilledBill(ss, name, phone, doc_code, a_id, agent_reference_no, false);

//            Bill b = addToReserveAgentBookingThroughApi(ss, decoder.decode(name, "+"), phone, doc_code, a_id, ar_no);
//            //// // System.out.println("b = " + b);
            bill = billDetails(b.getId());
            jSONObjectOut.put("make_booking", bill);
            jSONObjectOut.put("error", "0");
            jSONObjectOut.put("error_description", "");
        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("make_booking", bill);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }

        json = jSONObjectOut.toString();
        return json;
    }

//    /makeBooking/Dushan/0788044212/1/******/DR0001/20385287/451/1
    @GET
    @Path("/makeBooking/{name}/{phone}/{hospital_id}/{session_id}/{doc_code}/{agent_id}/{agent_reference_no}/{foriegn}")
    @Produces("application/json")
    public String makeBooking(@PathParam("name") String name, @PathParam("phone") String phone,
            @PathParam("hospital_id") String hospital_id, @PathParam("session_id") String session_id, @PathParam("doc_code") String doc_code,
            @PathParam("agent_id") String agent_id, @PathParam("agent_reference_no") String agent_reference_no,
            @PathParam("foriegn") String st_foriegn) {
        JSONArray bill = new JSONArray();
        String json = new String();
        List<Object[]> list = new ArrayList<>();
        JSONObject jSONObjectOut = new JSONObject();
        Long h_id = Long.parseLong(hospital_id);
        Long ss_id = Long.parseLong(session_id);
        Long a_id = Long.parseLong(agent_id);
//        Long ar_no = Long.parseLong(agent_reference_no);

        try {

            String s = fetchErrors(name, phone, doc_code, ss_id, a_id, agent_reference_no, st_foriegn);
//            String s = fetchErrors(name, phone, doc_code, ss_id, a_id, ar_no);
//            //// // System.out.println("s = " + s);
            if (!"".equals(s)) {
                jSONObjectOut.put("make_booking", s);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
                json = jSONObjectOut.toString();
                return json;
            }
            ServiceSession ss = serviceSessionFacade.find(ss_id);
            if (ss != null) {
                //For Settle bill
                ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.setItemFees(fetchFee(ss.getOriginatingSession()));
                //For Settle bill
                ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), PaymentMethod.Agent));
                ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            } else {
                jSONObjectOut.put("make_booking", "Invalid Session Id");
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
                return jSONObjectOut.toString();
            }
//            //// // System.out.println("ss = " + ss);
            Bill b;
            if ("0".equals(st_foriegn)) {
                b = saveBilledBill(ss, name, phone, doc_code, a_id, agent_reference_no, false);
            } else {
                b = saveBilledBill(ss, name, phone, doc_code, a_id, agent_reference_no, true);
            }
//            Bill b = addToReserveAgentBookingThroughApi(ss, decoder.decode(name, "+"), phone, doc_code, a_id, ar_no);
//            //// // System.out.println("b = " + b);

            bill = billDetails(b.getId());
            jSONObjectOut.put("make_booking", bill);
            jSONObjectOut.put("error", "0");
            jSONObjectOut.put("error_description", "");
        } catch (Exception e) {
            e.printStackTrace();
            jSONObjectOut.put("make_booking", bill);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }

        json = jSONObjectOut.toString();
        return json;
    }

//    @POST
//    @Path("/makeBooking2/")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public void makeBooking2(JSONObject inputJsonObj) {
//        String input = (String) inputJsonObj.get("input");
//        String output = "The input you sent is :" + input;
//        JSONObject outputJsonObj = new JSONObject();
//        outputJsonObj.put("output", output);
//    }
    @GET
    @Path("/bookings/{bill_id}")
    @Produces("application/json")
    public String getBookings(@PathParam("bill_id") String bill_id) {
//        /bookings/20058204
        //        /bookings/20058204
        JSONObject jSONObjectOut = new JSONObject();
        JSONArray bill = new JSONArray();
        try {
            long b_id = Long.parseLong(bill_id);
            bill = billDetails(b_id);
            if (bill != null) {
                jSONObjectOut.put("bookings", bill);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("bookings", bill);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            jSONObjectOut.put("bookings", bill);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
//        String json = new Gson().toJson(bill);
        String json = jSONObjectOut.toString();
        return json;
    }

//    /bookings/20385287/2017-11-22/2017-11-22
    @GET
    @Path("/bookings/{agent_id}/{from_date}/{to_date}")
    @Produces("application/json")
    public String getAllBookings(@PathParam("agent_id") String agent_id, @PathParam("from_date") String from_date, @PathParam("to_date") String to_date) {
//        /bookings/20058554/2016-08-01/2016-08-15
        //        /bookings/20058554/2016-08-01/2016-08-15
        JSONObject jSONObjectOut = new JSONObject();
        JSONArray bill = new JSONArray();
        try {
            long b_id = Long.parseLong(agent_id);
            Date fromDate = CommonFunctions.getConvertDateTimeFormat24(from_date);
            Date toDate = CommonFunctions.getConvertDateTimeFormat24(to_date);
            bill = billsDetails(b_id, fromDate, toDate, true);
            if (bill != null) {
                jSONObjectOut.put("bookings", bill);
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("bookings", bill);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            jSONObjectOut.put("bookings", bill);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }
//        String json = new Gson().toJson(bill);
        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/specility/")
    @Produces("application/json")
    public String getAllSpecilities() {
        List<Object[]> specilities = specilityList();
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        if (!specilities.isEmpty()) {
            for (Object[] con : specilities) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("Spec_id", con[0]);
                jSONObject.put("Spec_name", con[1]);
                array.put(jSONObject);
            }

        } else {
        }

        String json = array.toString();
        return json;
    }

    @GET
    @Path("/docs/")
    @Produces("application/json")
    public String getAllDoctors() {
        List<Object[]> consultants = doctorsList(null, null);
        JSONArray array = new JSONArray();
        if (!consultants.isEmpty()) {
            for (Object[] con : consultants) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("doc_id", con[0]);
                jSONObject.put("doc_name", con[1]);
                jSONObject.put("doc_specility", con[2]);
                jSONObject.put("doc_code", con[3]);
                array.put(jSONObject);
            }
//            jSONObjectOut.put("specilities", array);
//            jSONObjectOut.put("error", "0");
//            jSONObjectOut.put("error_description", "");

        } else {
//            jSONObjectOut.put("specilities", array);
//            jSONObjectOut.put("error", "1");
//            jSONObjectOut.put("error_description", "No Data.");
        }

        String json = array.toString();
//        String json = jSONObjectOut.toString();
        return json;
    }

//    /doc/1745
    @GET
    @Path("/doc/{spec_id}/")
    @Produces("application/json")
    public String getDoctorsSelectedSpecility(@PathParam("spec_id") String spec_id) {
        long sp_id = Long.parseLong(spec_id);
        List<Object[]> consultants = doctorsList(null, sp_id);
        JSONArray array = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();
        if (!consultants.isEmpty()) {
            for (Object[] con : consultants) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("doc_id", con[0]);
                jSONObject.put("doc_name", con[1]);
                jSONObject.put("doc_specility", con[2]);
                jSONObject.put("doc_code", con[3]);
                array.put(jSONObject);
            }
//            jSONObjectOut.put("specilities", array);
//            jSONObjectOut.put("error", "0");
//            jSONObjectOut.put("error_description", "");

        } else {
//            jSONObjectOut.put("specilities", array);
//            jSONObjectOut.put("error", "1");
//            jSONObjectOut.put("error_description", "No Data.");
        }

        String json = array.toString();
//        String json = jSONObjectOut.toString();
        return json;
    }

    @GET
    @Path("/ses/{doc_code}")
    @Produces("application/json")
    public String getDocSessions(@PathParam("doc_code") String doc_code) {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray array1 = new JSONArray();
        JSONObject jSONObjectOut = new JSONObject();

        try {
            List<ServiceSession> sessions = sessionsList(doc_code, null, null);
            if (!sessions.isEmpty()) {
                for (ServiceSession s : sessions) {
                    object = new JSONObject();
                    object.put("session_id", s.getId());
                    object.put("session_date", CommonFunctions.formatDate(s.getSessionDate(), "YYYY-MM-dd"));
                    object.put("session_starting_time", CommonFunctions.getTimeFormat24(s.getStartingTime()));
                    object.put("session_ending_time", CommonFunctions.getTimeFormat24(s.getEndingTime()));
                    object.put("session_max_no", s.getMaxNo());
                    object.put("session_is_refundable", s.isRefundable());
                    object.put("session_duration", s.getDuration());
                    object.put("session_room_no", s.getRoomNo());
//                    object.put("session_current_app_no", channelBean.getBillSessionsCount((long) s.getId(), (Date) s.getSessionDate()) + 1);
                    object.put("session_fee", CommonFunctions.getDouble((double) fetchLocalFee((long) s.getId(), PaymentMethod.Agent, false)));
                    object.put("session_fee_vat", CommonFunctions.getDouble((double) fetchLocalFeeVat((long) s.getId(), PaymentMethod.Agent, false)));
                    object.put("session_is_leaved", s.isDeactivated());
                    array.put(object);
//            s[10]=fetchLocalFee((long)s[0], PaymentMethod.Agent, true);
                }
                jSONObjectOut.put("session", array);
                jSONObjectOut.put("session_dates", sessionsDatesList(doc_code, null, null));
                jSONObjectOut.put("error", "0");
                jSONObjectOut.put("error_description", "");
            } else {
                jSONObjectOut.put("session", sessions);
                jSONObjectOut.put("error", "1");
                jSONObjectOut.put("error_description", "No Data.");
            }
        } catch (Exception e) {
            jSONObjectOut.put("session", object);
            jSONObjectOut.put("error", "1");
            jSONObjectOut.put("error_description", "Invalid Argument.");
        }

        String json = array.toString();
        return json;
    }

    @GET
    @Path("/apps/{ses_id}/")
    @Produces("application/json")
    public String getBillSessions(@PathParam("ses_id") String ses_id) {
        long sp_id = Long.parseLong(ses_id);
        List<BillSession> billSessions = fillBillSessions(sp_id);
        JSONArray array = new JSONArray();
        if (!billSessions.isEmpty()) {
            for (BillSession bs : billSessions) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("app_no", bs.getSerialNo());
                jSONObject.put("patient_name", bs.getBill().getPatient().getPerson().getNameWithTitle());
                if (bs.getBill().getPaidAmount() == 0) {
                    jSONObject.put("app_type", "Credit - " + bs.getBill().getPaymentMethod());
                } else {
                    jSONObject.put("app_type", "Paid - " + bs.getBill().getPaymentMethod());
                }
                if (bs.getBill().isCancelled()) {
                    jSONObject.put("app_status", "Canceled");
                } else if (bs.getBill().isRefunded()) {
                    jSONObject.put("app_status", "Refunded");
                } else {
                    jSONObject.put("app_status", "");
                }
                if (bs.getBill().getPaymentMethod() == PaymentMethod.Staff) {
                    jSONObject.put("staff_agent_status", bs.getBill().getToStaff().getCode());
                } else if (bs.getBill().getPaymentMethod() == PaymentMethod.Agent) {
                    jSONObject.put("staff_agent_status", bs.getBill().getCreditCompany().getCode());
                } else {
                    jSONObject.put("staff_agent_status", "");
                }

                array.put(jSONObject);
            }
        }
        String json = array.toString();

        return json;
    }

    //----------------------------------------------------
    public List<Object[]> doctorsList(String doc_code, Long spec_id) {

        List<Object[]> consultants = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = " select pi.staff.id,"
                + " pi.staff.person.name, "
                + " pi.staff.speciality.name,"
                + " pi.staff.code from PersonInstitution pi where pi.retired=false "
                + " and pi.type=:typ "
                + " and pi.staff.retired=false "
                + " and pi.institution is not null "
                + " and pi.staff.speciality is not null ";

        if (doc_code != null) {
            sql += " and pi.staff.code=:doc_code ";
            m.put("doc_code", doc_code);
        }
//        if (doc_id != null) {
//            sql += " and pi.staff.id=:doc_id ";
//            m.put("doc_id", doc_id);
//        }
        if (spec_id != null) {
            sql += " and pi.staff.speciality.id=:spec_id ";
            m.put("spec_id", spec_id);
        }

        sql += " order by pi.staff.speciality.name,pi.staff.person.name ";

        m.put("typ", PersonInstitutionType.Channelling);
        consultants = getStaffFacade().findAggregates(sql, m);

//        //// // System.out.println("m = " + m);
//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("consultants.size() = " + consultants.size());
        return consultants;
    }

    public List<Object[]> doctorsListAll() {
        List<Object[]> consultants = new ArrayList<>();
        String jpql;
        Map m = new HashMap();
        jpql = " select staff.id,"
                + " staff.person.name, "
                + " staff.speciality.name,"
                + " staff.code "
                + " from Consultant staff ";
        jpql += " where staff.retired=:ret ";
        jpql += " order by staff.speciality.name, staff.person.name ";
        m.put("ret", false);
        consultants = getStaffFacade().findAggregates(jpql, m);
        return consultants;
    }

    public List<BillSession> fillBillSessions(long ses_id) {

        HashMap m = new HashMap();

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                //                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        m.put("bt", bts);
        m
                .put("class", BilledBill.class
                );
//        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        m.put("ss", ses_id);

        return getBillSessionFacade().findByJpql(sql, m);

    }

    public List<Object[]> sessionsListObject(String doc_code, Date fromDate, Date toDate) {

        List<Object[]> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select s.id, "
                + " s.sessionDate, "
                + " s.startingTime, "
                + " s.endingTime, "
                + " s.maxNo, "
                + " s.refundable, "
                + " s.duration, "
                + " s.roomNo, "
                + " 0, "
                + " 0,"
                + " s.deactivated "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", CommonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m
                .put("class", ServiceSession.class
                );

        sessions = getStaffFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

        return sessions;
    }

    public List<ServiceSession> sessionsList(String doc_code, Date fromDate, Date toDate) {

        List<ServiceSession> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select s "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", CommonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m
                .put("class", ServiceSession.class
                );

        sessions = getServiceSessionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        //// // System.out.println("m = " + m);
//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("sessions.size() = " + sessions.size());
        List<ServiceSession> reList = new ArrayList<>();
        for (ServiceSession session : sessions) {
//            //// // System.out.println("session.getId() = " + session.getId());
//            //// // System.out.println("session.getId() = " + session.getStartingTime());
            Calendar date = Calendar.getInstance();
            date.setTime(session.getSessionDate());
//            //// // System.out.println("date.getTime() = " + date.getTime());
            Calendar time = Calendar.getInstance();
            time.setTime(session.getStartingTime());
//            //// // System.out.println("time.getTime() = " + time.getTime());
            time.set(Calendar.YEAR, date.get(Calendar.YEAR));
            time.set(Calendar.MONTH, date.get(Calendar.MONTH));
            time.set(Calendar.DATE, date.get(Calendar.DATE));
//            //// // System.out.println("time.getTime() = " + time.getTime());
            if (time.getTime().before(new Date())) {
                reList.add(session);
            }
        }
//        //// // System.out.println("reList.size() = " + reList.size());
        sessions.removeAll(reList);
//        //// // System.out.println("sessions.size() = " + sessions.size());

//        List<Object[]> objects = new ArrayList<>();
//        for (ServiceSession s : sessions) {
//            Object[] ob = null;
//            ob[0] =s.getId();
//            ob[1] =s.getSessionDate();
//            ob[2] =s.getStartingTime();
//            ob[3] =s.getEndingTime();
//            ob[4] =s.getMaxNo();
//            ob[5] =s.isRefundable();
//            ob[6] =s.getDuration();
//            ob[7] =s.getRoomNo();
//            ob[8] =0;
//            ob[9] =0;
//            ob[10] =s.isDeactivated();
//            if (ob!=null) {
//                objects.add(ob);
//            }
//        }
//        //// // System.out.println("objects.size() = " + objects.size());
        return sessions;
    }

    public JSONArray sessionsDatesList(String doc_code, Date fromDate, Date toDate) {
        JSONArray array = new JSONArray();
        List<ServiceSession> sessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select distinct(s) "
                + " From ServiceSession s where s.retired=false "
                + " and s.staff.code=:doc_code "
                + " and s.originatingSession is not null "
                + " and type(s)=:class ";
        if (fromDate != null && toDate != null) {
            sql += " and s.sessionDate between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        } else {
            sql += " and s.sessionDate >= :nd ";
            m.put("nd", CommonFunctions.getStartOfDay());
        }

        sql += " order by s.sessionDate,s.startingTime ";

        m.put("doc_code", doc_code);
        m
                .put("class", ServiceSession.class
                );

        sessions = getServiceSessionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        List<ServiceSession> reList = new ArrayList<>();
        for (ServiceSession session : sessions) {
//            //// // System.out.println("session.getId() = " + session.getId());
//            //// // System.out.println("session.getId() = " + session.getStartingTime());
            Calendar date = Calendar.getInstance();
            date.setTime(session.getSessionDate());
//            //// // System.out.println("date.getTime() = " + date.getTime());
            Calendar time = Calendar.getInstance();
            time.setTime(session.getStartingTime());
//            //// // System.out.println("time.getTime() = " + time.getTime());
            time.set(Calendar.YEAR, date.get(Calendar.YEAR));
            time.set(Calendar.MONTH, date.get(Calendar.MONTH));
            time.set(Calendar.DATE, date.get(Calendar.DATE));
//            //// // System.out.println("time.getTime() = " + time.getTime());
            if (time.getTime().before(new Date())) {
                reList.add(session);
            }
        }
//        //// // System.out.println("reList.size() = " + reList.size());
        sessions.removeAll(reList);
//        //// // System.out.println("sessions.size() = " + sessions.size());

//        //// // System.out.println("m = " + m);
//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("sessions.size() = " + sessions.size());
        Date beforeDate = null;
        for (ServiceSession s : sessions) {
//            //// // System.out.println("s = " + s.getSessionAt());
//            //// // System.out.println("beforeDate = " + beforeDate);
            if (beforeDate == null) {
//                System.err.println("add Null");
                Date d = (Date) s.getSessionDate();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                array.put(df.format(d));
                beforeDate = s.getSessionDate();
            } else {
//                //// // System.out.println("beforeDate.getTime() = " + beforeDate.getTime());
//                //// // System.out.println("s.getSessionDate().getTime() = " + s.getSessionDate().getTime());
                if (beforeDate.getTime() != s.getSessionDate().getTime()) {
//                    System.err.println("add");
                    Date d = (Date) s.getSessionDate();
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    array.put(df.format(d));
                }
                beforeDate = s.getSessionDate();
            }
        }
//        for (Object s : sessions) {
//            Date d = (Date) s;
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//            array.put(df.format(d));
//        }

        return array;
    }

    public JSONArray billDetails(long billId) {
        List<BillSession> billObjects;
        JSONArray array = new JSONArray();
        String sql;
        Map m = new HashMap();

        sql = "Select bs From BillSession bs "
                + " where bs.bill.id=:id ";

        m.put("id", billId);
        billObjects = billSessionFacade.findByJpql(sql, m);

//        //// // System.out.println("m = " + m);
//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("billObjects.length = " + billObjects.size());
        Map map = new HashMap();
        if (!billObjects.isEmpty()) {

            try {
                map.put("bill_id", billObjects.get(0).getBill().getId());
                map.put("bill_number", billObjects.get(0).getBill().getInsId());
                map.put("bill_agent", billObjects.get(0).getBill().getCreditCompany().getId());
                map.put("bill_app_no", billObjects.get(0).getBill().getSingleBillSession().getSerialNo());
                map.put("bill_patient_name", billObjects.get(0).getBill().getPatient().getPerson().getName());
                map.put("bill_phone", billObjects.get(0).getBill().getPatient().getPerson().getPhone());
                map.put("bill_doc_name", billObjects.get(0).getBill().getStaff().getPerson().getName());
                map.put("bill_session_date", CommonFunctions.formatDate(billObjects.get(0).getBill().getSingleBillSession().getSessionDate(), "YYYY-MM-dd"));
                map.put("bill_session_start_time", CommonFunctions.getTimeFormat24(billObjects.get(0).getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", CommonFunctions.getDateTimeFormat24(billObjects.get(0).getBill().getCreatedAt()));
                map.put("bill_total", CommonFunctions.getDouble(billObjects.get(0).getBill().getNetTotal()));
                map.put("bill_vat", CommonFunctions.getDouble(billObjects.get(0).getBill().getVat()));
                map.put("bill_vat_plus_total", CommonFunctions.getDouble(billObjects.get(0).getBill().getNetTotal() + billObjects.get(0).getBill().getVat()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        //// // System.out.println("map.length = " + map.size());
        array.put(map);

        return array;
    }

    public JSONArray billsDetails(long agentId, Date fromDate, Date toDate, boolean createDate) {
        List<BillSession> billObjects;
        JSONArray array = new JSONArray();
        String sql;
        Map m = new HashMap();

        sql = "Select bs From BillSession bs "
                + " where bs.bill.creditCompany.id=:id ";
        if (createDate) {
            sql += " and bs.bill.createdAt between :fd and :td "
                    + " order by bs.bill.createdAt ";
        } else {
            sql += " and bs.bill.singleBillSession.sessionDate between :fd and :td "
                    + " order by bs.bill.singleBillSession.sessionDate ";
        }

        m.put("id", agentId);
        m.put("fd", CommonFunctions.getStartOfDay(fromDate));
        m.put("td", CommonFunctions.getEndOfDay(toDate));
        billObjects = billSessionFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        //// // System.out.println("m = " + m);
//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("billObjects.length = " + billObjects.size());
        for (BillSession o : billObjects) {
            try {
                JSONObject map = new JSONObject();
                map.put("bill_id", o.getBill().getId());
                map.put("bill_number", o.getBill().getInsId());
                map.put("bill_agent", o.getBill().getCreditCompany().getId());
                map.put("bill_app_no", o.getBill().getSingleBillSession().getSerialNo());
                map.put("bill_patient_name", o.getBill().getPatient().getPerson().getName());
                map.put("bill_phone", o.getBill().getPatient().getPerson().getPhone());
                map.put("bill_doc_name", o.getBill().getStaff().getPerson().getName());
                map.put("bill_session_date", CommonFunctions.formatDate(o.getBill().getSingleBillSession().getSessionDate(), "YYYY-MM-dd"));
                map.put("bill_session_start_time", CommonFunctions.getTimeFormat24(o.getBill().getSingleBillSession().getServiceSession().getStartingTime()));
                map.put("bill_created_at", CommonFunctions.getDateTimeFormat24(o.getBill().getCreatedAt()));
                map.put("bill_total", CommonFunctions.getDouble(o.getBill().getNetTotal() + o.getBill().getVat()));
                array.put(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return array;
    }

    //----------------------------------------------------
    double fetchLocalFee(long id, PaymentMethod paymentMethod, boolean forign) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        if (forign) {
            jpql = "Select sum(f.ffee)";
        } else {
            jpql = "Select sum(f.fee)";
        }

        jpql += " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", serviceSessionFacade.find(id).getOriginatingSession().getId());
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Double obj = ItemFeeFacade.findDoubleByJpql(jpql, m);
//        //// // System.out.println("obj = " + obj);
        if (obj == null) {
            return 0;
        }

        return obj;
    }

    double fetchLocalFeeVat(long id, PaymentMethod paymentMethod, boolean forign) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        if (forign) {
            jpql = "Select sum(f.ffee)";
        } else {
            jpql = "Select sum(f.fee)";
        }

        jpql += " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses "
                + " and f.feeType in :fts ";
        m.put("fts", feeTypes);

        m.put("ses", serviceSessionFacade.find(id).getOriginatingSession().getId());

        Double obj = ItemFeeFacade.findDoubleByJpql(jpql, m);
//        //// // System.out.println("obj = " + obj);
        if (obj == null) {
            return 0;
        }

        return obj * 0.15;
    }

    String fetchErrors(String name, String phone, String doc, long ses, long agent, String agent_ref, String st_foriegn) {
//    String fetchErrors(String name, String phone, String doc, long ses, long agent, long agent_ref) {
        String s = "";
        if (name == null || "".equals(name)) {
            s = "Please Enter Name";
            return s;
        }
        if (phone == null || "".equals(phone)) {
            s = "Please Enter Phone Number";
            return s;
        }
        if (phone.length() != 10) {
            s = "Please Enter Phone Number Length";
            return s;
        }
        if ("".equals(doc)) {
            s = "Please Enter Docctor";
            return s;
        }
        if ("".equals(ses)) {
            s = "Please Enter Session";
            return s;
        }
        if ("".equals(agent)) {
            s = "Please Enter Agency";
            return s;
        }
        Institution institution = institutionFacade.find(agent);
        if (institution == null) {
            s = "Incorrect Agency Id";
            return s;
        }
        if ("".equals(agent_ref)) {
            s = "Please Enter Agency Reference No";
            return s;
        }
        if (checkAgentRefNo(agent_ref, institution)) {
            s = "This Reference No Already Exists";
            return s;
        }
        if ("".equals(st_foriegn)) {
            s = "Please Enter Foriegner Or Not";
            return s;
        }
        if (!("0".equals(st_foriegn) || "1".equals(st_foriegn))) {
            s = "Please Enter Foriegner Status 0 or 1";
            return s;
        }
//        if (checkAgentRefNo(agent_ref,institution)) {
//            s = "This Reference No Already Exists";
//            return s;
//        }

        return s;
    }

    private Bill saveBilledBill(ServiceSession ss, String name, String phone, String doc, long agent, String agent_ref, boolean foriegn) {
//    private Bill addToReserveAgentBookingThroughApi(ServiceSession ss, String name, String phone, String doc, long agent, long agent_ref) {
        Bill savingBill = createBill(ss, name, phone, agent);
        BillItem savingBillItem = createBillItem(savingBill, agent_ref, ss);
        BillSession savingBillSession = createBillSession(savingBill, savingBillItem, ss);

        List<BillFee> savingBillFees = createBillFee(savingBill, savingBillItem, ss, foriegn);
        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);

        getAmount(ss);

        getBillItemFacade().edit(savingBillItem);

        //Update Bill Session
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);

        //Update Bill
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);

        if (savingBill.getBillType() == BillType.ChannelAgent) {
            updateBallance(savingBill.getCreditCompany(), 0 - (savingBill.getNetTotal() + savingBill.getVat()), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
            savingBill.setBalance(savingBill.getNetTotal());
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    private Bill createBill(ServiceSession ss, String name, String phone, long agent) {
        Bill bill = new BilledBill();
        bill.setStaff(ss.getOriginatingSession().getStaff());
        bill.setAppointmentAt(ss.getSessionDate());
        bill.setTotal(getAmount(ss));
        bill.setNetTotal(getAmount(ss));
        bill.setPaymentMethod(PaymentMethod.Agent);

        Patient p = new Patient();
        p.setPerson(new Person());
        p.getPerson().setName(name);
        p.getPerson().setPhone(phone.substring(0, 3) + "-" + phone.substring(3, 10));
        getPersonFacade().create(p.getPerson());
        bill.setPatient(p);
        getPatientFacade().create(p);

        bill.setBillType(BillType.ChannelAgent);
        Institution institution = institutionFacade.find(agent);
        bill.setCreditCompany(institution);

        String insId = generateBillNumberInsId(bill, ss);

        if (insId.equals("")) {
            return null;
        }
        bill.setInsId(insId);

        String deptId = generateBillNumberDeptId(bill, ss);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(getBillNumberBean().bookingIdGenerator(ss.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getAmount(ss));
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
//        bill.setCreater(null);
        bill.setDepartment(ss.getDepartment());
        bill.setInstitution(ss.getInstitution());

        bill.setToDepartment(ss.getDepartment());
        bill.setToInstitution(ss.getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
//            //// // System.out.println("paidBill 1= " + bill.getPaidBill());
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }

        return bill;
    }

    private BillItem createBillItem(Bill bill, String agent_ref, ServiceSession ss) {
//    private BillItem createBillItem(Bill bill, long agent_ref, ServiceSession ss) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agent_ref);
//        bi.setAgentRefNo(String.valueOf(agent_ref));
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(null);
        bi.setGrossValue(ss.getOriginatingSession().getTotal());
        bi.setItem(ss);
        bi.setNetRate(ss.getOriginatingSession().getTotal());
        bi.setNetValue(ss.getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(ss.getOriginatingSession().getTotal());
        bi.setSessionDate(ss.getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, ServiceSession ss) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(null);
        bs.setDepartment(ss.getOriginatingSession().getDepartment());
        bs.setInstitution(ss.getOriginatingSession().getInstitution());
        bs.setItem(ss);

        bs.setServiceSession(ss);
        bs.setSessionDate(ss.getSessionDate());
        bs.setSessionTime(ss.getSessionTime());
        bs.setStaff(ss.getStaff());

        int count = getServiceSessionBean().getSessionNumber(ss, ss.getSessionDate(), bs);
//        System.err.println("count" + count);

        bs.setSerialNo(count);

        getBillSessionFacade().create(bs);

        return bs;
    }

    private List<BillFee> createBillFee(Bill bill, BillItem billItem, ServiceSession ss, boolean foriegn) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpTotalNet = 0;
        double tmpTotalVat = 0;
        double tmpTotalVatPlusNet = 0;
        double tmpDiscount = 0;
//        //// // System.out.println("ss.getOriginatingSession().getItemFees() = " + ss.getOriginatingSession().getItemFees().size());
        for (ItemFee f : ss.getOriginatingSession().getItemFees()) {
            if (bill.getPaymentMethod() != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (bill.getPaymentMethod() != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
//            bf.setCreater(null);
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(bill.getInstitution());
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
//                //// // System.out.println("bf.getSpeciality() = " + bf.getSpeciality());
                bf.setStaff(f.getStaff());
//                //// // System.out.println("bf.getStaff() = " + bf.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (foriegn) {
                bf.setFeeValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
            }

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
                bf.setFeeGrossValue(bf.getFeeValue());
                bf.setFeeVat(bf.getFeeValue() * 0.15);
                bf.setFeeVatPlusValue(bf.getFeeValue() * 1.15);
                bf.setFeeDiscount(0.0);
            } else {
                bf.setFeeGrossValue(bf.getFeeValue());
                bf.setFeeVat(0.0);
                bf.setFeeVatPlusValue(bf.getFeeValue());
                bf.setFeeDiscount(0.0);
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(bill.getInstitution());
            }

            tmpTotal += bf.getFeeGrossValue();
            tmpTotalVat += bf.getFeeVat();
            tmpTotalVatPlusNet += bf.getFeeVatPlusValue();
            tmpTotalNet += bf.getFeeValue();
            tmpDiscount += bf.getFeeDiscount();

            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }
        bill.setDiscount(tmpDiscount);
        bill.setNetTotal(tmpTotalNet);
        bill.setTotal(tmpTotal);
        bill.setVat(tmpTotalVat);
        bill.setVatPlusNetTotal(tmpTotalVatPlusNet);
//        //// // System.out.println("tmpDiscount = " + tmpDiscount);
//        //// // System.out.println("tmpTotal = " + tmpTotal);
//        //// // System.out.println("bill.getNetTotal() = " + bill.getNetTotal());
//        //// // System.out.println("bill.getTotal() = " + bill.getTotal());
        getBillFacade().edit(bill);

        billItem.setDiscount(tmpDiscount);
        billItem.setGrossValue(tmpTotal);
        billItem.setNetValue(tmpTotalNet);
        billItem.setVat(tmpTotalVat);
        billItem.setVatPlusNetValue(tmpTotalVatPlusNet);
//        //// // System.out.println("billItem.getNetValue() = " + billItem.getNetValue());
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

    public double getAmount(ServiceSession ss) {
        double amount = 0.0;
        amount = ss.getOriginatingSession().getTotalFee();
//        System.err.println("ss.getOriginatingSession().getTotalFee() = " + ss.getOriginatingSession().getTotalFee());

        return amount;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    private String generateBillNumberInsId(Bill bill, ServiceSession ss) {
        String suffix = ss.getInstitution().getCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String insId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "COS";
                } else {
                    suffix += "CS";
                }
                insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), billType, billClassType, suffix);
            } else {
                suffix += "C";
                insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CC";
            billClassType = BillClassType.CancelledBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CF";
            billClassType = BillClassType.RefundBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(ss.getInstitution(), bts, billClassType, suffix);
        }

//        //// // System.out.println("billClassType = " + billClassType);
//        //// // System.out.println("insId = " + insId);
        return insId;
    }

    private String generateBillNumberDeptId(Bill bill, ServiceSession ss) {
        String suffix = ss.getDepartment().getCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "COS";
                } else {
                    suffix += "CS";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "C";
                deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CC";
            billClassType = BillClassType.CancelledBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CF";
            billClassType = BillClassType.RefundBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(ss.getInstitution(), ss.getDepartment(), bts, billClassType, suffix);
        }

//        //// // System.out.println("billClassType = " + billClassType);
//        //// // System.out.println("deptId = " + deptId);
        return deptId;
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession, String refNo) {
//        //// // System.out.println("updating agency balance");
//        //// // System.out.println("ins.getName() = " + ins.getName());
//        //// // System.out.println("ins.getBallance() before " + ins.getBallance());
//        //// // System.out.println("transactionValue = " + transactionValue);
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
//        agentHistory.setCreater(null);
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(billSession);
        agentHistory.setBalanceBeforeTransaction(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNumber(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public List<Object[]> specilityList() {
        List<Object[]> specilities;
        String jpql;
        Map params = new HashMap();
        jpql = " select c.id,c.name "
                + " from DoctorSpeciality c "
                + " where c.retired=:ret "
                + " order by c.name";
        params.put("ret", false);
        specilities = getStaffFacade().findAggregates(jpql, params);
        return specilities;
    }

    public List<Object[]> hospitalList() {
        List<Object[]> hospitals;
        String jpql;
        Map params = new HashMap();
        jpql = " select c.id, c.name, c.code, c.address  "
                + " from Institution c "
                + " where c.retired=:ret "
                + " order by c.name";
        params.put("ret", false);
        hospitals = getInstitutionFacade().findAggregates(jpql, params);
        return hospitals;
    }

    private boolean checkAgentRefNo(long agent_ref, Institution institution) {
        if (getAgentReferenceBookController().agentReferenceNumberIsAlredyUsed(Long.toString(agent_ref), institution, BillType.ChannelAgent, PaymentMethod.Agent)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAgentRefNo(String agent_ref, Institution institution) {
        if (getAgentReferenceBookController().agentReferenceNumberIsAlredyUsed(agent_ref, institution, BillType.ChannelAgent, PaymentMethod.Agent)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * PUT method for updating or creating an instance of ChannelApi
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public UriInfo getContext() {
        return context;
    }

    public void setContext(UriInfo context) {
        this.context = context;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public AgentReferenceBookController getAgentReferenceBookController() {
        return AgentReferenceBookController;
    }

    public void setAgentReferenceBookController(AgentReferenceBookController AgentReferenceBookController) {
        this.AgentReferenceBookController = AgentReferenceBookController;
    }

}
