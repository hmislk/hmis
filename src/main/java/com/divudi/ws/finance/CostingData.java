package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.BillDetailsDTO;
import com.divudi.core.data.dto.BillFinanceDetailsDTO;
import com.divudi.core.data.dto.BillItemDetailsDTO;
import com.divudi.core.data.dto.BillItemFinanceDetailsDTO;
import com.divudi.core.data.dto.PaymentDTO;
import com.divudi.core.data.dto.PharmaceuticalBillItemDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.persistence.TemporalType;

@Path("costing_data")
@RequestScoped
public class CostingData {

    @Context
    private HttpServletRequest requestContext;

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    @EJB
    private PaymentFacade paymentFacade;

    @Inject
    ApiKeyController apiKeyController;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public CostingData() {
    }

    /**
     * Helper method to convert BigDecimal to Double
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Get the last bill details without any parameters
     * Endpoint: /costing_data/last_bill
     */
    @GET
    @Path("/last_bill")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastBill() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            // Get the last bill
            String jpql = "SELECT b FROM Bill b ORDER BY b.id DESC";
            Map<String, Object> params = new HashMap<>();
            List<Bill> bills = billFacade.findByJpql(jpql, params, 1);

            if (bills == null || bills.isEmpty()) {
                return errorResponse("No bills found", 404);
            }

            Bill bill = bills.get(0);
            BillDetailsDTO billDTO = convertBillToDTO(bill);

            return successResponse(billDTO);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get bill details by bill number using query parameter (Recommended for bill numbers with special characters)
     * Endpoint: /costing_data/bill?number={bill_number}
     * Example: /costing_data/bill?number=MP/OP/25/000074
     */
    @GET
    @Path("/bill")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBillByNumberQuery(@QueryParam("number") String billNumber) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            if (billNumber == null || billNumber.trim().isEmpty()) {
                return errorResponse("Bill number is required", 400);
            }

            // Search bills by deptId (exact match)
            String jpql = "SELECT b FROM Bill b WHERE b.deptId = :deptId ORDER BY b.id DESC";
            Map<String, Object> params = new HashMap<>();
            params.put("deptId", billNumber.trim());
            List<Bill> bills = billFacade.findByJpql(jpql, params);

            if (bills == null || bills.isEmpty()) {
                return errorResponse("No bills found with bill number: " + billNumber, 404);
            }

            // Convert all matching bills to DTOs
            List<BillDetailsDTO> billDTOs = new ArrayList<>();
            for (Bill bill : bills) {
                billDTOs.add(convertBillToDTO(bill));
            }

            return successResponse(billDTOs);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get bill details by bill number (deptId) using path parameter
     * Endpoint: /costing_data/by_bill_number/{bill_number}
     * NOTE: This endpoint does not work with bill numbers containing forward slashes (/)
     * Use /costing_data/bill?number={bill_number} instead for such cases
     * @deprecated Use getBillByNumberQuery with query parameter for better compatibility
     */
    @GET
    @Path("/by_bill_number/{bill_number}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBillByNumber(@PathParam("bill_number") String billNumber) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            if (billNumber == null || billNumber.trim().isEmpty()) {
                return errorResponse("Bill number is required", 400);
            }

            // Search bills by deptId (exact match)
            String jpql = "SELECT b FROM Bill b WHERE b.deptId = :deptId ORDER BY b.id DESC";
            Map<String, Object> params = new HashMap<>();
            params.put("deptId", billNumber.trim());
            List<Bill> bills = billFacade.findByJpql(jpql, params);

            if (bills == null || bills.isEmpty()) {
                return errorResponse("No bills found with bill number: " + billNumber, 404);
            }

            // Convert all matching bills to DTOs
            List<BillDetailsDTO> billDTOs = new ArrayList<>();
            for (Bill bill : bills) {
                billDTOs.add(convertBillToDTO(bill));
            }

            return successResponse(billDTOs);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get bill details by bill ID (exact match)
     * Endpoint: /costing_data/by_bill_id/{bill_id}
     */
    @GET
    @Path("/by_bill_id/{bill_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBillById(@PathParam("bill_id") String billIdStr) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            if (billIdStr == null || billIdStr.trim().isEmpty()) {
                return errorResponse("Bill ID is required", 400);
            }

            Long billId;
            try {
                billId = Long.parseLong(billIdStr.trim());
            } catch (NumberFormatException e) {
                return errorResponse("Invalid bill ID format", 400);
            }

            // Find bill by ID
            Bill bill = billFacade.find(billId);

            if (bill == null) {
                return errorResponse("No bill found with ID: " + billId, 404);
            }

            BillDetailsDTO billDTO = convertBillToDTO(bill);

            return successResponse(billDTO);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Convert Bill entity to BillDetailsDTO
     */
    private BillDetailsDTO convertBillToDTO(Bill bill) {
        BillDetailsDTO dto = new BillDetailsDTO();

        // Set bill basic details
        dto.setId(bill.getId());
        dto.setDtype(bill.getClass().getSimpleName());
        dto.setCreatedAt(bill.getCreatedAt());
        dto.setFromDepartmentId(bill.getFromDepartment() != null ? bill.getFromDepartment().getId() : null);
        dto.setToDepartmentId(bill.getToDepartment() != null ? bill.getToDepartment().getId() : null);
        dto.setDepartmentId(bill.getDepartment() != null ? bill.getDepartment().getId() : null);
        dto.setFromInstitutionId(bill.getFromInstitution() != null ? bill.getFromInstitution().getId() : null);
        dto.setToInstitutionId(bill.getToInstitution() != null ? bill.getToInstitution().getId() : null);
        dto.setInstitutionId(bill.getInstitution() != null ? bill.getInstitution().getId() : null);
        dto.setBillTypeAtomic(bill.getBillTypeAtomic() != null ? bill.getBillTypeAtomic().toString() : null);
        dto.setBillType(bill.getBillType() != null ? bill.getBillType().toString() : null);
        dto.setDiscount(bill.getDiscount());
        dto.setTax(bill.getTax());
        dto.setExpenseTotal(bill.getExpenseTotal());
        dto.setNetTotal(bill.getNetTotal());
        dto.setTotal(bill.getTotal());
        dto.setBillFinanceDetailsId(bill.getBillFinanceDetails() != null ? bill.getBillFinanceDetails().getId() : null);

        // Set additional fields from completion queries
        dto.setBalance(bill.getBalance());
        dto.setVat(bill.getVat());
        dto.setPaidAmount(bill.getPaidAmount());
        dto.setPaymentMethod(bill.getPaymentMethod() != null ? bill.getPaymentMethod().toString() : null);
        dto.setCancelledBillId(bill.getCancelledBill() != null ? bill.getCancelledBill().getId() : null);
        dto.setRefundedBillId(bill.getRefundedBill() != null ? bill.getRefundedBill().getId() : null);
        dto.setRetired(bill.isRetired());
        dto.setPatientName(bill.getPatient() != null && bill.getPatient().getPerson() != null ? bill.getPatient().getPerson().getName() : null);
        dto.setCreditCompanyName(bill.getCreditCompany() != null ? bill.getCreditCompany().getName() : null);
        dto.setDeptId(bill.getDeptId());

        // Convert Bill Finance Details
        if (bill.getBillFinanceDetails() != null) {
            dto.setBillFinanceDetails(convertBillFinanceDetailsToDTO(bill.getBillFinanceDetails()));
        }

        // Convert Bill Items - always use explicit query to avoid LazyInitializationException
        // Bill entities returned by BillFacade are detached, so accessing lazy collections will fail
        List<BillItem> billItems = findBillItemsFromBill(bill);

        if (billItems != null && !billItems.isEmpty()) {
            List<BillItemDetailsDTO> billItemDTOs = new ArrayList<>();
            for (BillItem billItem : billItems) {
                billItemDTOs.add(convertBillItemToDTO(billItem));
            }
            dto.setBillItems(billItemDTOs);
        }

        // Convert Payments - always use explicit query to avoid LazyInitializationException
        List<Payment> payments = findPaymentsFromBill(bill);

        if (payments != null && !payments.isEmpty()) {
            List<PaymentDTO> paymentDTOs = new ArrayList<>();
            for (Payment payment : payments) {
                paymentDTOs.add(convertPaymentToDTO(payment));
            }
            dto.setPayments(paymentDTOs);
        }

        return dto;
    }

    /**
     * Convert BillFinanceDetails entity to BillFinanceDetailsDTO
     */
    private BillFinanceDetailsDTO convertBillFinanceDetailsToDTO(BillFinanceDetails bfd) {
        BillFinanceDetailsDTO dto = new BillFinanceDetailsDTO();
        dto.setId(bfd.getId());
        dto.setNetTotal(toDouble(bfd.getNetTotal()));
        dto.setGrossTotal(toDouble(bfd.getGrossTotal()));
        dto.setTotalCostValue(toDouble(bfd.getTotalCostValue()));
        dto.setTotalPurchaseValue(toDouble(bfd.getTotalPurchaseValue()));
        dto.setTotalRetailSaleValue(toDouble(bfd.getTotalRetailSaleValue()));
        return dto;
    }

    /**
     * Convert BillItem entity to BillItemDetailsDTO
     */
    private BillItemDetailsDTO convertBillItemToDTO(BillItem billItem) {
        BillItemDetailsDTO dto = new BillItemDetailsDTO();
        dto.setId(billItem.getId());
        dto.setCreatedAt(billItem.getCreatedAt());
        dto.setBillId(billItem.getBill() != null ? billItem.getBill().getId() : null);
        dto.setItemId(billItem.getItem() != null ? billItem.getItem().getId() : null);
        dto.setBillItemFinanceDetailsId(billItem.getBillItemFinanceDetails() != null ? billItem.getBillItemFinanceDetails().getId() : null);
        dto.setQty(billItem.getQty());
        dto.setRate(billItem.getRate());
        dto.setNetRate(billItem.getNetRate());
        dto.setGrossValue(billItem.getGrossValue());
        dto.setNetValue(billItem.getNetValue());
        dto.setRetired(billItem.isRetired());

        // Convert Bill Item Finance Details
        if (billItem.getBillItemFinanceDetails() != null) {
            dto.setBillItemFinanceDetails(convertBillItemFinanceDetailsToDTO(billItem.getBillItemFinanceDetails()));
        }

        // Convert Pharmaceutical Bill Item
        PharmaceuticalBillItem pbi = findPharmaceuticalBillItem(billItem);
        if (pbi != null) {
            dto.setPharmaceuticalBillItem(convertPharmaceuticalBillItemToDTO(pbi));
        }

        return dto;
    }

    /**
     * Convert BillItemFinanceDetails entity to BillItemFinanceDetailsDTO
     */
    private BillItemFinanceDetailsDTO convertBillItemFinanceDetailsToDTO(BillItemFinanceDetails bifd) {
        BillItemFinanceDetailsDTO dto = new BillItemFinanceDetailsDTO();
        dto.setId(bifd.getId());
        dto.setCreatedAt(bifd.getCreatedAt());
        dto.setQuantity(toDouble(bifd.getQuantity()));
        dto.setQuantityByUnits(toDouble(bifd.getQuantityByUnits()));
        dto.setLineNetRate(toDouble(bifd.getLineNetRate()));
        dto.setGrossRate(toDouble(bifd.getGrossRate()));
        dto.setLineGrossRate(toDouble(bifd.getLineGrossRate()));
        dto.setCostRate(toDouble(bifd.getCostRate()));
        dto.setPurchaseRate(toDouble(bifd.getPurchaseRate()));
        dto.setRetailSaleRate(toDouble(bifd.getRetailSaleRate()));
        dto.setLineCostRate(toDouble(bifd.getLineCostRate()));
        dto.setBillCostRate(toDouble(bifd.getBillCostRate()));
        dto.setTotalCostRate(toDouble(bifd.getTotalCostRate()));
        dto.setLineGrossTotal(toDouble(bifd.getLineGrossTotal()));
        dto.setGrossTotal(toDouble(bifd.getGrossTotal()));
        dto.setLineCost(toDouble(bifd.getLineCost()));
        dto.setBillCost(toDouble(bifd.getBillCost()));
        dto.setTotalCost(toDouble(bifd.getTotalCost()));
        dto.setValueAtCostRate(toDouble(bifd.getValueAtCostRate()));
        dto.setValueAtPurchaseRate(toDouble(bifd.getValueAtPurchaseRate()));
        dto.setValueAtRetailRate(toDouble(bifd.getValueAtRetailRate()));
        return dto;
    }

    /**
     * Convert PharmaceuticalBillItem entity to PharmaceuticalBillItemDTO
     */
    private PharmaceuticalBillItemDTO convertPharmaceuticalBillItemToDTO(PharmaceuticalBillItem pbi) {
        PharmaceuticalBillItemDTO dto = new PharmaceuticalBillItemDTO();
        dto.setCreatedAt(pbi.getCreatedAt());
        dto.setCreaterId(pbi.getCreater() != null ? pbi.getCreater().getId() : null);
        dto.setId(pbi.getId());
        dto.setBillItemId(pbi.getBillItem() != null ? pbi.getBillItem().getId() : null);
        dto.setQty(pbi.getQty());
        dto.setFreeQty(pbi.getFreeQty());
        dto.setRetailRate(pbi.getRetailRate());
        dto.setPurchaseRate(pbi.getPurchaseRate());
        dto.setCostRate(pbi.getCostRate());
        dto.setPurchaseValue(pbi.getPurchaseValue());
        dto.setRetailValue(pbi.getRetailValue());
        dto.setCostValue(pbi.getCostValue());
        return dto;
    }

    /**
     * Find BillItems from Bill
     */
    private List<BillItem> findBillItemsFromBill(Bill bill) {
        if (bill == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.retired = false AND bi.bill = :bill ORDER BY bi.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bill", bill);
        return billItemFacade.findByJpql(jpql, params);
    }

    /**
     * Find PharmaceuticalBillItem for a BillItem
     */
    private PharmaceuticalBillItem findPharmaceuticalBillItem(BillItem billItem) {
        if (billItem == null) {
            return null;
        }
        String jpql = "SELECT pbi FROM PharmaceuticalBillItem pbi WHERE pbi.billItem = :billItem";
        Map<String, Object> params = new HashMap<>();
        params.put("billItem", billItem);
        return pharmaceuticalBillItemFacade.findFirstByJpql(jpql, params);
    }

    /**
     * Find Payments from Bill
     */
    private List<Payment> findPaymentsFromBill(Bill bill) {
        if (bill == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Payment p WHERE p.bill.id = :billId AND p.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", bill.getId());
        return paymentFacade.findByJpql(jpql, params);
    }

    /**
     * Convert Payment entity to PaymentDTO
     */
    private PaymentDTO convertPaymentToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setBillId(payment.getBill() != null ? payment.getBill().getId() : null);
        dto.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        dto.setPaidValue(payment.getPaidValue());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setPaymentDate(payment.getPaymentDate());

        // Bank details
        if (payment.getBank() != null) {
            dto.setBankId(payment.getBank().getId());
            dto.setBankName(payment.getBank().getName());
        }

        // Credit company
        if (payment.getCreditCompany() != null) {
            dto.setCreditCompanyId(payment.getCreditCompany().getId());
            dto.setCreditCompanyName(payment.getCreditCompany().getName());
        }

        // Staff
        if (payment.getToStaff() != null) {
            dto.setToStaffId(payment.getToStaff().getId());
            dto.setToStaffName(payment.getToStaff().getName());
        }

        // Other fields
        dto.setReferenceNo(payment.getReferenceNo());
        dto.setPolicyNo(payment.getPolicyNo());
        dto.setChequeRefNo(payment.getChequeRefNo());
        dto.setChequeDate(payment.getChequeDate());
        dto.setComments(payment.getComments());
        dto.setRetired(payment.isRetired());

        return dto;
    }

    /**
     * List bills by bill type for a date range and department.
     * Used by AI agents to drill into specific transaction types when investigating
     * F15 report discrepancies.
     *
     * Endpoint: GET /costing_data/bills_by_type
     * Params:
     *   fromDate     - start datetime, format: yyyy-MM-dd HH:mm:ss  (e.g. 2026-02-18 00:00:00)
     *   toDate       - end datetime,   format: yyyy-MM-dd HH:mm:ss  (e.g. 2026-02-18 23:59:59)
     *   departmentId - department ID (e.g. 485 for Main Pharmacy)
     *   billTypeAtomic - bill type name (e.g. PHARMACY_RETAIL_SALE, PHARMACY_GRN)
     *   limit        - max results (default 200, max 1000)
     *
     * Filters apply on Bill.createdAt to align with PharmacyService F15 logic.
     *
     * Returns lightweight bill summary list (not full bill details).
     * Use /costing_data/by_bill_id/{id} to retrieve full details including bill items.
     */
    @GET
    @Path("/bills_by_type")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBillsByType(
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("departmentId") Long departmentId,
            @QueryParam("billTypeAtomic") String billTypeAtomic,
            @QueryParam("limit") Integer limit) {

        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            return errorResponse("Not a valid key", 401);
        }

        if (fromDateStr == null || fromDateStr.trim().isEmpty()) {
            return errorResponse("Parameter 'fromDate' is required (format: yyyy-MM-dd HH:mm:ss)", 400);
        }
        if (toDateStr == null || toDateStr.trim().isEmpty()) {
            return errorResponse("Parameter 'toDate' is required (format: yyyy-MM-dd HH:mm:ss)", 400);
        }
        if (departmentId == null) {
            return errorResponse("Parameter 'departmentId' is required", 400);
        }
        if (billTypeAtomic == null || billTypeAtomic.trim().isEmpty()) {
            return errorResponse("Parameter 'billTypeAtomic' is required (e.g. PHARMACY_RETAIL_SALE)", 400);
        }

        int maxResults = (limit != null && limit > 0 && limit <= 1000) ? limit : 200;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate;
        Date toDate;
        try {
            fromDate = sdf.parse(fromDateStr.trim());
            toDate = sdf.parse(toDateStr.trim());
        } catch (ParseException e) {
            return errorResponse("Invalid date format. Use yyyy-MM-dd HH:mm:ss (e.g. 2026-02-18 00:00:00)", 400);
        }

        try {
            String jpql = "SELECT b FROM Bill b "
                    + "WHERE b.retired = false "
                    + "AND b.department.id = :departmentId "
                    + "AND b.billTypeAtomic = :billTypeAtomic "
                    + "AND b.createdAt >= :fromDate "
                    + "AND b.createdAt <= :toDate "
                    + "ORDER BY b.id ASC";

            Map<String, Object> params = new HashMap<>();
            params.put("departmentId", departmentId);
            params.put("billTypeAtomic", com.divudi.core.data.BillTypeAtomic.valueOf(billTypeAtomic.trim()));
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP, maxResults);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Bill b : bills) {
                Map<String, Object> row = new HashMap<>();
                row.put("billId", b.getId());
                row.put("billNumber", b.getDeptId());
                row.put("billType", b.getBillType() != null ? b.getBillType().toString() : null);
                row.put("billTypeAtomic", b.getBillTypeAtomic() != null ? b.getBillTypeAtomic().toString() : null);
                row.put("createdAt", b.getCreatedAt());
                row.put("billTime", b.getBillTime());
                row.put("netTotal", b.getNetTotal());
                row.put("grossTotal", b.getTotal());
                row.put("retired", b.isRetired());
                row.put("completed", b.isCompleted());

                if (b.getBillFinanceDetails() != null) {
                    BillFinanceDetails bfd = b.getBillFinanceDetails();
                    row.put("stockValueAtRetailRate", toDouble(bfd.getTotalRetailSaleValue()));
                    row.put("stockValueAtCostRate", toDouble(bfd.getTotalCostValue()));
                    row.put("stockValueAtPurchaseRate", toDouble(bfd.getTotalPurchaseValue()));
                }

                result.add(row);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("code", 200);
            response.put("count", result.size());
            response.put("departmentId", departmentId);
            response.put("billTypeAtomic", billTypeAtomic);
            response.put("fromDate", fromDateStr);
            response.put("toDate", toDateStr);
            response.put("data", result);
            return Response.status(200).entity(gson.toJson(response)).build();

        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(com.divudi.core.data.BillTypeAtomic.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            return errorResponse("Unknown billTypeAtomic value: " + billTypeAtomic
                    + ". Valid values (com.divudi.core.data.BillTypeAtomic): " + validValues, 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
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
