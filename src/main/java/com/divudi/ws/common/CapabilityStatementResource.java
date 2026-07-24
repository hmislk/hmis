/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * HMIS API capability statement endpoint.
 *
 * Returns a machine-readable summary of available API resources,
 * their endpoints, operations, and authentication requirements.
 */
@Path("capabilities")
@RequestScoped
public class CapabilityStatementResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapabilities() {
        JsonObject response = Json.createObjectBuilder()
                .add("status", "success")
                .add("data", Json.createObjectBuilder()
                        .add("name", "HMIS REST API")
                        .add("version", "1.0")
                        .add("description", "Hospital Management Information System API")
                        .add("authentication", "API Key required. Most endpoints use 'Finance' header; Channel/Booking and Consultant endpoints use 'Token' header; FHIR Patient endpoints use 'FHIR' header; Config endpoints use 'Config' header; LIMS and middleware endpoints use their own credential schemes (URL params, JSON body, or HTTP Basic Auth). See per-resource authentication field for specifics.")
                        .add("contact", "HMIS Support Team")
                        .add("termsOfUse", "Use according to institutional HMIS API access policies")
                        .add("resources", buildResources())
                        .build())
                .build();

        return Response.ok(response.toString(), MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    private javax.json.JsonArray buildResources() {
        return Json.createArrayBuilder()
                .add(resource("Capabilities", "/api/capabilities",
                        "API discovery endpoint that describes available resources",
                        "None",
                        "GET"))
                .add(resource("Channel", "/api/channel",
                        "Channeling and appointment operations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Consultant", "/api/channel/consultant",
                        "List consultants via GET (supports query, page, size, specialityId). "
                        + "Create a new consultant via POST with duplicate detection (returns already_exists/409 when matched by name+title). "
                        + "Update an existing consultant by ID via PUT /api/channel/consultant/{id}. "
                        + "Required field for POST: name. Optional: title, sex, mobile, phone, fax, address, code, serialNo, "
                        + "specialityId, institutionId, registration, qualification, description.",
                        "API Key (Token header)",
                        "GET", "POST", "PUT"))
                .add(resource("Doctor Speciality", "/api/channel/speciality",
                        "CRUD for DoctorSpeciality records. "
                        + "GET lists active specialities (supports ?query=&page=&size=). "
                        + "POST creates a speciality (required: name; optional: code, description); returns 200 with status=already_exists if a duplicate name exists. "
                        + "PUT /{id} updates name, code, or description (only supplied fields changed). "
                        + "DELETE /{id} soft-retires the speciality.",
                        "API Key (Token header)",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Clinical Metadata", "/api/clinical/metadata",
                        "CRUD for EMR clinical metadata types: symptom, sign, diagnosis, procedure, plan, vocabulary, "
                        + "race, religion, blood_group, civil_status, employment, relationship. "
                        + "Required param: type. GET supports query/page/size. "
                        + "POST returns success, already_exists (with id), or error.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Clinical Favourite Medicines", "/api/clinical/favourite_medicines",
                        "Clinical favourite medicine templates and favourite-diagnosis medicine suggestions "
                        + "(type=FavouriteMedicine default, or type=FavouriteDiagnosis). "
                        + "Includes /entities/diagnoses for searching diagnoses to use as forItemName.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Membership", "/api/apiMembership",
                        "Membership-related operations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Config", "/api/config",
                        "Application configuration options",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("FHIR", "/api/fhir",
                        "FHIR and interoperability resources",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Finance", "/api/finance",
                        "Finance operations and billing endpoints. "
                        + "GET /bill/search?billNumber= looks up bills by bill number (insId or deptId).",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Balance History", "/api/balance_history",
                        "Financial balance history",
                        "API Key",
                        "GET"))
                .add(resource("Bill Data Correction", "/api/bill_data_correction",
                        "Bill data correction operations",
                        "API Key",
                        "POST"))
                .add(resource("Costing Data", "/api/costing_data",
                        "Cost accounting data",
                        "API Key",
                        "GET"))
                .add(resource("QuickBooks", "/api/qb",
                        "QuickBooks integration",
                        "API Key",
                        "GET"))
                .add(resource("Departments", "/api/departments",
                        "Department management. Create/update bodies and GET responses include the "
                        + "bed-board SVG fields svgParentView and svgChildView (issue #21592). "
                        + "Dedicated sub-resource: GET/PUT /api/departments/{id}/svg "
                        + "(body { svgParentView, svgChildView }) reads/sets just the drawings. "
                        + "SVG is stored verbatim; it is sanitised at render time on the bed board.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Institutions", "/api/institutions",
                        "Institution management. Create/update bodies and GET responses include the "
                        + "bed-board SVG fields svgParentView and svgChildView (issue #21592). "
                        + "Dedicated sub-resource: GET/PUT /api/institutions/{id}/svg "
                        + "(body { svgParentView, svgChildView }) reads/sets just the drawings. "
                        + "SVG is stored verbatim; it is sanitised at render time on the bed board.",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH", "DELETE"))
                .add(resource("Sites", "/api/sites",
                        "Site management. Create/update bodies and GET responses include the "
                        + "bed-board SVG fields svgParentView and svgChildView (issue #21592). "
                        + "Dedicated sub-resource: GET/PUT /api/sites/{id}/svg "
                        + "(body { svgParentView, svgChildView }) reads/sets just the drawings. "
                        + "SVG is stored verbatim; it is sanitised at render time on the bed board.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward", "/api/apiInward",
                        "Inward patient workflows",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Admission Number Counters", "/api/admission-numbers",
                        "View or reset the BHT/OPD-card admission-number sequence counter for an admission type.",
                        "API Key (Finance header)", "GET", "PUT"))
                .add(resource("Inward Discount Matrix", "/api/inward-discount-matrix",
                        "Manage inward discount matrix entries for services/investigations and pharmacy. "
                        + "Supports scope=service|pharmacy to restrict category types. "
                        + "Optional creditCompanyId filters or sets a credit-company-specific override row. "
                        + "Lookup sub-paths for resolving names to IDs: "
                        + "/admission-types/search, /payment-schemes/search, "
                        + "/pharmaceutical-item-categories/search, /payment-methods, /credit-companies/search. "
                        + "POST returns HTTP 409 with existing id when a duplicate combination exists.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward Price Adjustment", "/api/inward-price-adjustment",
                        "Manage inward price adjustment (margin) matrix entries for services, investigations, and pharmacy. "
                        + "Supports scope=service|pharmacy to restrict category types. "
                        + "Optional creditCompanyId sets a credit-company-specific margin override. "
                        + "Price range lookup: fromPrice/toPrice define the gross value range to which the margin applies. "
                        + "Lookup sub-paths: /categories/search?scope=service|pharmacy, /departments/search, "
                        + "/payment-methods, /credit-companies/search. "
                        + "Diagnostic sub-path: /diagnose?itemId=&departmentId=&paymentMethod=&patientEncounterId=&price= "
                        + "explains whether inward service-charge margin will be applied for an item, with a per-condition breakdown. "
                        + "POST returns HTTP 409 with existing id when a duplicate combination exists. "
                        + "NOTE: For inward price adjustments, prefer the newer /api/price-matrix/inward endpoint.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Price Matrix Inward", "/api/price-matrix/inward",
                        "Manage InwardPriceAdjustment (margin/service charge) matrix entries. "
                        + "Flat DTO format with departmentId/departmentName etc. "
                        + "Supports discountPercent, admissionTypeId, and creditCompanyId. "
                        + "All create/update/retire actions are audit-logged (PRICE_MATRIX_CREATED/UPDATED/RETIRED). "
                        + "Query params: categoryId, departmentId, paymentMethod, limit. "
                        + "POST returns HTTP 409 with existing id when a duplicate combination exists.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward Room Categories", "/api/inward/room-categories",
                        "Manage inward room categories (backs /inward/inward_room_category.xhtml). "
                        + "POST returns HTTP 409 with existing id when a duplicate name already exists.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward Rooms", "/api/inward/rooms",
                        "Manage inward rooms (backs /inward/inward_room.xhtml). "
                        + "Supports optional filter roomCategoryId. "
                        + "POST returns HTTP 409 with existing id when a duplicate name already exists. "
                        + "Create/update bodies and GET responses include the bed-board child-tile field "
                        + "svgChildView (a Room is a leaf, so it has no svgParentView) (issue #21592). "
                        + "Dedicated sub-resource: GET/PUT /api/inward/rooms/{id}/svg "
                        + "(body { svgChildView }) reads/sets just the drawing. "
                        + "SVG is stored verbatim; it is sanitised at render time on the bed board.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward Document Templates", "/api/inward/document-templates",
                        "Manage document templates (HTML templates with placeholders). "
                        + "Supports all DocumentTemplateType values: Prescription, MedicalCertificate, FitnessCertificate, Referral, InpatientDiagnosisCard, InpatientLetter. "
                        + "Optional query params: type (filter by type), query (name search), size. "
                        + "GET /{id} includes full contents field. "
                        + "POST/PUT fields: name, type, contents (HTML with placeholders), defaultTemplate, autoGenerate.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward Room Facility Charges", "/api/inward/room-facility-charges",
                        "Manage inward room facility charges / room fees (backs /inward/inward_room_facility.xhtml). "
                        + "Supports optional filters roomId and roomCategoryId. "
                        + "departmentId is required on POST and may not be set to null on PUT. "
                        + "Charge fields: roomCharge, maintananceCharge, linenCharge, nursingCharge, "
                        + "moCharge, moChargeForAfterDuration, adminstrationCharge, medicalCareCharge. "
                        + "TimedItemFee fields: timedItemFeeDurationHours, timedItemFeeOverShootHours, "
                        + "timedItemFeeDurationDaysForMoCharge.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Item Requests", "/api/itemrequests",
                        "External systems submit item/service requests (meals like Breakfast/Lunch/Dinner as "
                        + "InwardService items, and stock items like Water Bottle/Tea/Milk/Sugar) against a patient's "
                        + "active BHT. Requests are saved Pending (no charge, no stock movement) and routed to a "
                        + "target department's in-app approval queue. A department user approves (charges the BHT and "
                        + "deducts stock atomically, failing the whole approval if any line has insufficient stock) or "
                        + "rejects (records a reason) the request via the JSF approval page — this API does not expose "
                        + "approve/reject. External systems poll GET /{id} for status: PENDING, APPROVED, REJECTED, CANCELLED.",
                        "API Key",
                        "GET", "POST", "PUT"))
                .add(resource("LIMS", "/api/lims",
                        "Laboratory Information Management System integrations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("LIMS Middleware", "/api/limsmw",
                        "LIMS middleware integration",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Machines (Analyzers)", "/api/machines",
                        "CRUD for Machine (analyzer) entities. "
                        + "GET lists active machines (supports ?query=&size=). "
                        + "GET /{id} returns a single machine. "
                        + "POST creates a machine (required: name; optional: code, description); returns 409 when duplicate name exists. "
                        + "PUT /{id} updates name, code, or description. "
                        + "DELETE /{id} soft-retires the machine.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Analyzer Tests", "/api/machines/{machineId}/tests",
                        "CRUD for analyzer test Items (itemType=AnalyzerTest) linked to a Machine. "
                        + "GET lists tests for the machine (supports ?query=&size=). "
                        + "GET /{id} returns a single test. "
                        + "POST creates a test (required: name, code); returns 409 when duplicate code exists for the machine. "
                        + "PUT /{id} updates name or code. "
                        + "DELETE /{id} soft-retires the test. "
                        + "The code field matches analyzer output codes used by the LIMS middleware.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Patient Samples", "/api/patient-samples",
                        "Search and retrieval of PatientSample records. "
                        + "GET /search supports ?sampleId=&patientName=&fromDate=&toDate=&billNumber=&size= (dates: yyyy-MM-dd). "
                        + "GET /{id} returns full sample detail including automation workflow fields "
                        + "(sentToAnalyzer, receivedFromAnalyzer, sampleCollected, sampleReceivedAtLab, etc.) "
                        + "and linked patientInvestigation, machine, test, and investigationComponent.",
                        "API Key",
                        "GET"))
                .add(resource("Middleware", "/api/middleware",
                        "General middleware endpoints",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmaceutical Config", "/api/pharmaceutical_config",
                        "Pharmaceutical configuration management",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("Pharmaceutical Items", "/api/pharmaceutical_items",
                        "Pharmaceutical item master data. AMP create/update accepts "
                        + "strengthOfAnIssueUnit (Double) and strengthUnitId (Long, MeasurementUnit) "
                        + "for strength-ratio based dispensing substitution.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Pharmacy Items", "/api/pharmacy/items",
                        "Create, search, update, and retire dispensable pharmacy PharmaceuticalItem records used by pharmacy billing and dispensing. "
                        + "POST/PUT fields include name, code, categoryId, dosageFormId, ampId, institutionId, departmentId, retailRate, allowFractions, discountAllowed. "
                        + "GET /search supports query, institutionId, departmentId, size.",
                        "API Key (Finance header)",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Pharmacy Adjustments", "/api/pharmacy_adjustments",
                        "Pharmacy stock and adjustment operations. "
                        + "POST /backfill_finance_details is an admin-only, idempotent backfill: recomputes "
                        + "BillFinanceDetails + bill totals for adjustment bills created before this fix existed, "
                        + "using each bill's own stored before/after audit values. Bills that already have "
                        + "BillFinanceDetails are skipped, not overwritten. Body: departmentId, fromDate, toDate "
                        + "(yyyy-MM-dd), apply (false = dry run).",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmacy Discounts", "/api/pharmacy/discounts",
                        "Manage PaymentSchemeDiscount rows that control per-category discount percentages "
                        + "applied during pharmacy billing for a given payment scheme. "
                        + "POST /api/pharmacy/discounts/bulk creates or updates a discount % across "
                        + "all pharmacy item categories at once (idempotent). "
                        + "GET lists rows (filter: paymentSchemeId, paymentSchemeName, billType, limit). "
                        + "POST creates a single row. PUT /{id} updates discountPercent. DELETE /{id} soft-retires.",
                        "API Key (Finance header)",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Payment Schemes", "/api/payment-scheme",
                        "List and update PaymentScheme records. "
                        + "GET lists all active schemes (optional ?query=name-filter&limit=). "
                        + "Response includes all billing-scope flags: validForInpatientBills, validForPharmacy, "
                        + "validForBilledBills, validForChanneling, and eligibility flags "
                        + "(staffMemberRequired, membershipRequired, staffRequired, staffOrFamilyRequired, "
                        + "memberRequired, memberOrFamilyRequired, seniorCitizenRequired, pregnantMotherRequired). "
                        + "PUT /{id} performs a partial update — only fields present in the request body are changed.",
                        "API Key (Finance header)",
                        "GET", "PUT"))
                .add(resource("Pharmacy Search", "/api/pharmacy_adjustments/search",
                        "Pharmacy stock search",
                        "API Key",
                        "GET"))
                .add(resource("Pharmacy Batches", "/api/pharmacy_batches",
                        "Pharmacy batch management",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmacy F15 Report", "/api/pharmacy_f15_report",
                        "Pharmacy F15 reporting",
                        "API Key",
                        "GET"))
                .add(resource("Stock History", "/api/stock_history",
                        "Stock movement history",
                        "API Key",
                        "GET"))
                .add(resource("Login History", "/api/logins",
                        "Login history filtered by department, user, and date range (days, fromDate, toDate). "
                        + "Also exposes /api/logins/last-per-user for the most recent login per unique user in a department.",
                        "API Key",
                        "GET"))
                .add(resource("Staff", "/api/staff",
                        "Staff CRUD. GET lists active staff (supports ?query=&departmentId=&size=). "
                        + "GET /{id} returns a single staff record including personId, code, designation, and department. "
                        + "POST creates a new staff member (required: name; optional: code, designation (string label), departmentId, institutionId). "
                        + "Creates a linked Person automatically. "
                        + "PUT /{id} updates name, code, designation, departmentId, or institutionId (partial update — only supplied fields change). "
                        + "DELETE /{id}?retireComments=reason soft-retires a staff record. "
                        + "Link an existing Staff to a WebUser via PUT /api/users/{id}/staff with body {staffId}. "
                        + "POST /api/users supports optional staffId field to pre-link staff at user creation. "
                        + "POST /api/users/{id}/privileges/all with optional body {departmentIds:[...]} assigns every privilege across specified (or all loggable) departments; "
                        + "returns {privilegesAdded, privilegesSkipped, departments:[{departmentId, added, skipped}]}.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Users", "/api/users",
                        "User CRUD, password reset/change, loggable department assignment, "
                        + "and per-user privilege assignment with department scope. "
                        + "Create/update accepts optional loginPage and optional staffId. "
                        + "POST /{id}/privileges requires departmentId. "
                        + "POST /{id}/departments/{departmentId}/privileges/category assigns all privileges from named categories. "
                        + "POST /{id}/privileges/all with optional body {departmentIds:[...]} assigns every privilege across specified or all loggable departments. "
                        + "PUT /{id}/staff with body {staffId} links a Staff record to the user. "
                        + "Supports filtering by departmentId and query string. "
                        + "DELETE /{id}/departments/{assignmentId} revokes one loggable department. "
                        + "DELETE /{id}/departments/{departmentId}/privileges bulk-revokes all privileges for a department. "
                        + "POST /{id}/departments/{departmentId}/privileges/all assigns every privilege for a department. "
                        + "Role-template operations (roles are admin-time templates; runtime reads user-level records only): "
                        + "POST /{id}/role/reset resets a user's records for the given aspects/departments to a role template "
                        + "(roleId optional — defaults to the user's own role; body: {roleId?, departmentIds[], aspects[]?, updateUserRole?, preview?}). "
                        + "POST /{id}/role/expand and POST /{id}/role/narrow add/strip a role template's records "
                        + "(body: {roleId, departmentIds[], aspects[]?, preview?}; roleId required). "
                        + "aspects values: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE (default [\"PRIVILEGES\"]). "
                        + "preview=true returns counts without writing. "
                        + "POST /bulk/role-operations applies RESET/EXPAND/NARROW to many users at once (explicit userIds or a role/department filter); "
                        + "for safety it requires preview=true first, then confirm=true to actually apply. "
                        + "GET /roles lists active roles with template summary counts (privileges/icons/subscriptions) and template login page. "
                        + "PUT /{id}/login-page (body: {departmentId, loginPage}) and DELETE /{id}/login-page/{departmentId} manage the "
                        + "per-user-per-department default login page override. "
                        + "POST /{id}/force-password-reset flags needToResetPassword=true without requiring a new password value "
                        + "(distinct from /reset-password, which sets an actual password). "
                        + "GET /password-status (optional ?from=&to=, yyyy-MM-dd) reports lastPasswordResetAt and needToResetPassword "
                        + "per active user; omitted range returns all users including those who have never reset.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("User Bulk Privileges", "/api/users/bulk-privileges",
                        "Assign a set of privileges to multiple users in one call. "
                        + "If departmentId is omitted, privileges are assigned across each user's own loggable departments. "
                        + "Returns a per-user summary of privilegesAdded and privilegesSkipped.",
                        "API Key",
                        "POST"))
                .add(resource("Available Privileges", "/api/users/privileges/available",
                        "Returns the complete list of valid privilege enum names from Privileges.java. "
                        + "Use this to discover assignable privilege names before calling assign endpoints.",
                        "API Key",
                        "GET"))
                .add(resource("User Roles", "/api/user-roles",
                        "User role CRUD and role-level privilege assignment with optional department scope.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Subscriptions", "/api/subscriptions",
                        "Manage notification trigger subscriptions (who receives which notification, in which department). "
                        + "GET /subscriptions lists subscriptions (filters: triggerType, userId, departmentId, applicationWide=true). "
                        + "GET /subscriptions/trigger-types lists all available TriggerType values (name, label, medium, parent). "
                        + "POST /subscriptions creates a subscription (body: userId, triggerType, and EITHER departmentId OR applicationWide:true); "
                        + "returns already_exists when an identical non-retired subscription exists. "
                        + "DELETE /subscriptions/{id} soft-retires a subscription. "
                        + "An application-wide subscription (null department) matches every department across the whole application.",
                        "API Key",
                        "GET", "POST", "DELETE"))
                                .add(resource("Investigations", "/api/investigations",
                        "Investigation master management including search, create, update, and activate/deactivate for item import workflows. "
                        + "Category/sample/container(tube)/analyzer(machine) can each be set via an ID referencing an existing row "
                        + "(categoryId, sampleId, containerId, analyzerId — errors if not found) or a name "
                        + "(categoryName, sampleName, containerName, analyzerName — found-or-created by name if no matching row exists).",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("Investigation Format", "/api/investigations/{investigationId}/format",
                        "Manage investigation report format: items (Label, Value, Calculation, Flag, DynamicLabel types), "
                        + "item values (dropdown options for List-type items), calculations (formulas referencing other items), "
                        + "flags (reference range flags by age/sex), and dynamic labels (conditional labels by age/sex). "
                        + "Sub-resources: /items, /items/{itemId}/values, /calculations, /flags, /dynamic-labels.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Investigation Components", "/api/investigations/{investigationId}/components",
                        "Manage InvestigationComponent groupings used to organize report items within an investigation's format "
                        + "(componentName only). GET lists components for the investigation. POST creates one. "
                        + "PUT /{componentId} renames one. DELETE /{componentId} permanently removes one — rejected with an error "
                        + "if any report item (InvestigationItem) still references it.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Investigation Fees", "/api/investigations/{investigationId}/fees",
                        "Manage investigation pricing (ItemFee), mirroring the Services /fees sub-resource. "
                        + "GET lists non-retired fees for the investigation. POST adds a fee "
                        + "(body: name, feeType, fee, ffee, discountAllowed, institutionId, departmentId, specialityId, staffId). "
                        + "PUT /{feeId} updates a fee (only non-null fields are applied). "
                        + "DELETE /{feeId} soft-deletes (retires) a fee. All mutations recalculate the investigation's "
                        + "total/totalForForeigner and are rejected against a retired investigation.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Investigation Validators", "/api/investigations/{investigationId}/validators",
                        "Manage InvestigationValidator result-range checks (name, maximumValue, minimumValue) for an "
                        + "investigation. GET lists non-retired validators. POST creates one. "
                        + "PUT /{validatorId} updates one (only non-null/non-blank fields are applied). "
                        + "DELETE /{validatorId} soft-deletes (retires) one. All mutations are rejected against a "
                        + "retired investigation. Note: the legacy InvestigationValidaterComponent relation is dead "
                        + "code in the app today and is intentionally not exposed by this API.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Services", "/api/services",
                        "OPD and Inward service management including fees and categories. "
                        + "Fee sub-paths: /{id}/fees (GET fees, POST add), /{id}/fees/{feeId} (PUT update, DELETE remove). "
                        + "/fees/bulk-margin (POST bulk-update marginAllowed/discountAllowed on fees in a category). "
                        + "/fees/margin-disabled?categoryId=X (GET diagnostic list of fees with marginAllowed=false/null).",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH", "DELETE"))
                .add(resource("Timed Items", "/api/timed-items",
                        "Manage timed item master data (room rent, oxygen, ICU time, etc.) and their tiered fee slots (TimedItemFee). "
                        + "TimedItem entities are consumed by the inward timed service page (/inward/inward_timed_service_consume.xhtml). "
                        + "Fees are ordered by sortOrder and support durationHours/overShootHours/repeating for tiered block billing. "
                        + "Sub-resource: /timed-items/{id}/fees for per-item fee management. "
                        + "Sub-resource: /timed-items/categories for TimedItemCategory CRUD (GET list, GET /{id}, POST, PUT /{id}, DELETE /{id}). "
                        + "PATCH /activate and /deactivate control availability without retiring.",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH", "DELETE"))
                .add(resource("Collecting Centre Fees", "/api/pricing/collecting_centre_fees",
                        "Manage item fees for collecting centres. "
                        + "GET ?institutionId=X lists all active fees for that centre. "
                        + "POST creates a new fee (body: collectingCentreId, itemId, name, feeType, fee, ffee, departmentId). "
                        + "PUT /{feeId} updates a fee. "
                        + "DELETE /{feeId} soft-retires a single fee. "
                        + "DELETE ?institutionId=X soft-retires ALL active fees for that centre. "
                        + "POST /recalculate?institutionId=X recalculates item totals for all items with CC fees.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("SAP Integration - Billing", "/api/sap/billing",
                        "Bidirectional SAP S/4HANA Cloud FI integration. "
                        + "POST /push/{billId} pushes an HMIS bill to SAP as a journal entry (debit AR, credit revenue). "
                        + "GET /status/{billId} returns the push status and SAP document number. "
                        + "POST /confirm receives a payment confirmation webhook from SAP. "
                        + "GET /confirm/status/{billId} returns the confirmation status. "
                        + "Auth: Finance header.",
                        "API Key (Finance header)",
                        "GET", "POST"))
                .add(resource("Dynamic Forms", "/api/forms",
                        "Design and manage dynamic clinical form templates and capture filled values. "
                        + "Templates: GET /forms/templates lists all; GET /forms/templates/{id} returns one with field count; POST creates; PUT updates; DELETE retires. "
                        + "Fields: GET /forms/templates/{id}/fields; POST adds a field (componentPresentationType, componentDataType, editHtml, viewHtml, choices). "
                        + "PUT /forms/fields/{id} updates; DELETE /forms/fields/{id} retires. "
                        + "Choices: GET /forms/fields/{id}/choices; POST adds; PUT /forms/choices/{id} updates; DELETE /forms/choices/{id} retires. "
                        + "Filled data: GET /forms/entries/{admissionId} lists all PatientFormEntry records for an admission; "
                        + "GET /forms/entries/{entryId}/values lists all CaptureComponent values for a filled entry.",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("SAP Integration - Inventory", "/api/sap/inventory",
                        "SAP S/4HANA Cloud MM inventory sync. "
                        + "GET /sync?fromDate=yyyy-MM-dd&toDate=yyyy-MM-dd fetches SAP goods-receipt material documents "
                        + "and matches them to HMIS pharmacy items by code or barcode (configurable). "
                        + "fromDate defaults to last-sync watermark; toDate defaults to today. "
                        + "Read-only audit sync — does not create GRN bills. "
                        + "Auth: Finance header.",
                        "API Key (Finance header)",
                        "GET"))
                .add(resource("FHIR Patient", "/api/fhir/Patient",
                        "FHIR R5 Patient search, read, create, update",
                        "API Key (use FHIR header, not Finance)",
                        "GET", "POST", "PUT"))
                .build();
    }

    private JsonObject resource(String name, String path, String description, String authentication,
            String... operations) {
        JsonArrayBuilder operationArray = Json.createArrayBuilder();
        for (String operation : operations) {
            operationArray.add(operation);
        }
        return Json.createObjectBuilder()
                .add("name", name)
                .add("path", path)
                .add("description", description)
                .add("operations", operationArray)
                .add("authentication", authentication)
                .build();
    }
}
