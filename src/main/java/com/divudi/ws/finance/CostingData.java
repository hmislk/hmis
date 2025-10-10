package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.BillDetailsDTO;
import com.divudi.core.data.dto.BillFinanceDetailsDTO;
import com.divudi.core.data.dto.BillItemDetailsDTO;
import com.divudi.core.data.dto.BillItemFinanceDetailsDTO;
import com.divudi.core.data.dto.PharmaceuticalBillItemDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
    public String getLastBill() {
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
            e.printStackTrace();
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get bill details by bill number (deptId)
     * Endpoint: /costing_data/by_bill_number/{bill_number}
     */
    @GET
    @Path("/by_bill_number/{bill_number}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getBillByNumber(@PathParam("bill_number") String billNumber) {
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
            e.printStackTrace();
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
    public String getBillById(@PathParam("bill_id") String billIdStr) {
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
            e.printStackTrace();
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

        // Convert Bill Finance Details
        if (bill.getBillFinanceDetails() != null) {
            dto.setBillFinanceDetails(convertBillFinanceDetailsToDTO(bill.getBillFinanceDetails()));
        }

        // Convert Bill Items
        List<BillItem> billItems = bill.getBillItems();
        if (billItems == null || billItems.isEmpty()) {
            billItems = findBillItemsFromBill(bill);
        }

        if (billItems != null && !billItems.isEmpty()) {
            List<BillItemDetailsDTO> billItemDTOs = new ArrayList<>();
            for (BillItem billItem : billItems) {
                billItemDTOs.add(convertBillItemToDTO(billItem));
            }
            dto.setBillItems(billItemDTOs);
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
        if (k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
    }

    /**
     * Create error response JSON
     */
    private String errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return gson.toJson(response);
    }

    /**
     * Create success response JSON
     */
    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return gson.toJson(response);
    }
}
