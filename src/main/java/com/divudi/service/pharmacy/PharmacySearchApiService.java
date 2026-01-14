/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.data.dto.search.DepartmentDTO;
import com.divudi.core.data.dto.search.ItemDTO;
import com.divudi.core.data.dto.search.StockSearchRequestDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Pharmacy Search API operations
 * Provides search functionality for stocks, departments, and items
 * Leverages existing patterns from StockSearchService and controller autocomplete methods
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class PharmacySearchApiService implements Serializable {

    @EJB
    private StockFacade stockFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private ItemFacade itemFacade;

    /**
     * Search stocks with comprehensive filtering
     * Leverages StockSearchService patterns with enhanced filtering
     */
    public List<StockDTO> searchStocks(StockSearchRequestDTO request) throws Exception {
        validateStockSearchRequest(request);

        // Find department by name
        Department department = findDepartmentByName(request.getDepartment());
        if (department == null) {
            throw new Exception("Department not found: " + request.getDepartment());
        }

        // Build dynamic JPQL query
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.StockDTO(")
            .append("s.id, s.id, s.itemBatch.id, s.itemBatch.item.name, s.itemBatch.item.code, ")
            .append("s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire, ")
            .append("s.itemBatch.batchNo, s.itemBatch.purcahseRate, s.itemBatch.wholesaleRate, ")
            .append("s.itemBatch.costRate, s.itemBatch.item.allowFractions) ")
            .append("FROM Stock s ")
            .append("WHERE s.department = :department ")
            .append("AND s.retired = false ");

        Map<String, Object> params = new HashMap<>();
        params.put("department", department);

        // Add item search criteria (name, code, barcode)
        jpql.append("AND (upper(s.itemBatch.item.name) LIKE :query ")
            .append("OR upper(s.itemBatch.item.code) LIKE :query ");

        // Include barcode search if query is longer (following StockSearchService pattern)
        if (request.getQuery().length() > 6) {
            jpql.append("OR s.itemBatch.item.barcode = :exactQuery ");
            params.put("exactQuery", request.getQuery().trim());
        }
        jpql.append(") ");

        params.put("query", "%" + request.getQuery().toUpperCase() + "%");

        // Add quantity filters
        if (!request.getIncludeZeroStock()) {
            jpql.append("AND s.stock > 0 ");
        }

        if (request.getMinQuantity() != null) {
            jpql.append("AND s.stock >= :minQuantity ");
            params.put("minQuantity", request.getMinQuantity());
        }

        if (request.getMaxQuantity() != null) {
            jpql.append("AND s.stock <= :maxQuantity ");
            params.put("maxQuantity", request.getMaxQuantity());
        }

        // Add retail rate filters
        if (request.getMinRetailRate() != null) {
            jpql.append("AND s.itemBatch.retailsaleRate >= :minRetailRate ");
            params.put("minRetailRate", request.getMinRetailRate());
        }

        if (request.getMaxRetailRate() != null) {
            jpql.append("AND s.itemBatch.retailsaleRate <= :maxRetailRate ");
            params.put("maxRetailRate", request.getMaxRetailRate());
        }

        // Add cost rate filters
        if (request.getMinCostRate() != null) {
            jpql.append("AND s.itemBatch.costRate >= :minCostRate ");
            params.put("minCostRate", request.getMinCostRate());
        }

        if (request.getMaxCostRate() != null) {
            jpql.append("AND s.itemBatch.costRate <= :maxCostRate ");
            params.put("maxCostRate", request.getMaxCostRate());
        }

        // Add purchase rate filters
        if (request.getMinPurchaseRate() != null) {
            jpql.append("AND s.itemBatch.purcahseRate >= :minPurchaseRate ");
            params.put("minPurchaseRate", request.getMinPurchaseRate());
        }

        if (request.getMaxPurchaseRate() != null) {
            jpql.append("AND s.itemBatch.purcahseRate <= :maxPurchaseRate ");
            params.put("maxPurchaseRate", request.getMaxPurchaseRate());
        }

        // Add expiry date filters
        if (request.getExpiryAfter() != null) {
            jpql.append("AND s.itemBatch.dateOfExpire >= :expiryAfter ");
            params.put("expiryAfter", request.getExpiryAfter());
        }

        if (request.getExpiryBefore() != null) {
            jpql.append("AND s.itemBatch.dateOfExpire <= :expiryBefore ");
            params.put("expiryBefore", request.getExpiryBefore());
        }

        // Add batch number filter
        if (request.getBatchNo() != null && !request.getBatchNo().trim().isEmpty()) {
            jpql.append("AND upper(s.itemBatch.batchNo) LIKE :batchNo ");
            params.put("batchNo", "%" + request.getBatchNo().toUpperCase() + "%");
        }

        // Add ordering
        jpql.append("ORDER BY s.itemBatch.item.name, s.stock DESC, s.itemBatch.dateOfExpire");

        // Execute query with limit - cast to correct type
        @SuppressWarnings("unchecked")
        List<StockDTO> results = (List<StockDTO>) stockFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP, request.getLimit());
        return results;
    }

    /**
     * Search departments by name
     * Follows DepartmentController.completeDepartments() pattern
     */
    public List<DepartmentDTO> searchDepartments(String query, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Department search query is required");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        String jpql = "SELECT d FROM Department d " +
                      "WHERE d.retired = false " +
                      "AND upper(d.name) LIKE :query " +
                      "ORDER BY d.name";

        int resultLimit = (limit != null && limit > 0 && limit <= 50) ? limit : 20;
        List<Department> departments = departmentFacade.findByJpql(jpql, params, resultLimit);

        // Convert entities to DTOs
        List<DepartmentDTO> results = new ArrayList<>();
        for (Department dept : departments) {
            results.add(new DepartmentDTO(dept.getId(), dept.getName(), dept.getCode()));
        }
        return results;
    }

    /**
     * Search items by name, code, or barcode
     * Follows AmpController.completeAmp() pattern
     */
    public List<ItemDTO> searchItems(String query, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Item search query is required");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT i FROM Amp i ")
            .append("WHERE i.retired = false ")
            .append("AND (upper(i.name) LIKE :query ")
            .append("OR upper(i.code) LIKE :query ");

        // Include barcode search for longer queries
        if (query.length() > 6) {
            jpql.append("OR i.barcode = :exactQuery ");
            params.put("exactQuery", query.trim());
        }

        jpql.append(") ORDER BY i.name");

        int resultLimit = (limit != null && limit > 0 && limit <= 50) ? limit : 30;
        List items = itemFacade.findByJpql(jpql.toString(), params, resultLimit);

        // Convert entities to DTOs
        List<ItemDTO> results = new ArrayList<>();
        for (Object itemObj : items) {
            if (itemObj instanceof com.divudi.core.entity.pharmacy.Amp) {
                com.divudi.core.entity.pharmacy.Amp amp = (com.divudi.core.entity.pharmacy.Amp) itemObj;
                String genericName = "";
                if (amp.getVmp() != null && amp.getVmp().getName() != null) {
                    genericName = amp.getVmp().getName();
                }
                results.add(new ItemDTO(amp.getId(), amp.getName(), amp.getCode(), amp.getBarcode(), genericName));
            }
        }
        return results;
    }

    // Private helper methods

    /**
     * Find department by exact name match
     */
    private Department findDepartmentByName(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("name", departmentName.trim());

        String jpql = "SELECT d FROM Department d " +
                      "WHERE d.retired = false " +
                      "AND d.name = :name";

        List<Department> departments = departmentFacade.findByJpql(jpql, params, 1);
        return departments.isEmpty() ? null : departments.get(0);
    }

    /**
     * Validate stock search request
     */
    private void validateStockSearchRequest(StockSearchRequestDTO request) throws Exception {
        if (request == null) {
            throw new Exception("Search request is required");
        }

        if (!request.isValid()) {
            throw new Exception("Query and department are required for stock search");
        }

        // Validate numeric ranges
        if (request.getMinQuantity() != null && request.getMinQuantity() < 0) {
            throw new Exception("Minimum quantity cannot be negative");
        }

        if (request.getMaxQuantity() != null && request.getMaxQuantity() < 0) {
            throw new Exception("Maximum quantity cannot be negative");
        }

        if (request.getMinQuantity() != null && request.getMaxQuantity() != null &&
            request.getMinQuantity() > request.getMaxQuantity()) {
            throw new Exception("Minimum quantity cannot be greater than maximum quantity");
        }

        // Validate rate ranges
        if (request.getMinRetailRate() != null && request.getMinRetailRate() < 0) {
            throw new Exception("Minimum retail rate cannot be negative");
        }

        if (request.getMaxRetailRate() != null && request.getMaxRetailRate() < 0) {
            throw new Exception("Maximum retail rate cannot be negative");
        }

        if (request.getMinCostRate() != null && request.getMinCostRate() < 0) {
            throw new Exception("Minimum cost rate cannot be negative");
        }

        if (request.getMaxCostRate() != null && request.getMaxCostRate() < 0) {
            throw new Exception("Maximum cost rate cannot be negative");
        }

        // Validate date ranges
        if (request.getExpiryAfter() != null && request.getExpiryBefore() != null &&
            request.getExpiryAfter().after(request.getExpiryBefore())) {
            throw new Exception("Expiry 'after' date cannot be later than 'before' date");
        }
    }

    /**
     * Parse date string in yyyy-MM-dd format
     */
    public Date parseDate(String dateString) throws Exception {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false); // Strict date parsing
            return dateFormat.parse(dateString.trim());
        } catch (ParseException e) {
            throw new Exception("Invalid date format. Expected yyyy-MM-dd, got: " + dateString);
        }
    }
}