package com.divudi.service.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.facade.StockFacade;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class StockSearchService {

    @EJB
    private StockFacade stockFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    public List<Stock> findAvailableStocks(String qry, Department department) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }
        qry = qry.replaceAll("[\n\r]", "").trim();
        Map<String, Object> params = new HashMap<>();
        params.put("department", department);
        params.put("stockMin", 0.0);
        params.put("query", "%" + qry + "%");
        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);
        StringBuilder sql = new StringBuilder("SELECT i FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");
        sql.append("i.itemBatch.item.name LIKE :query ");
        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }
        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :query ");
        }
        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }
        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");
        return stockFacade.findByJpql(sql.toString(), params, 20);
    }

    public List<StockDTO> findStockDtos(String qry, Department department) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }
        qry = qry.replaceAll("[\n\r]", "").trim();
        Map<String, Object> params = new HashMap<>();
        params.put("department", department);
        params.put("stockMin", 0.0);
        params.put("query", "%" + qry + "%");
        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);
        StringBuilder sql = new StringBuilder("SELECT new com.divudi.core.data.dto.StockDTO(")
                .append("s.id, ")
                .append("s.itemBatch.item.name, ")
                .append("s.itemBatch.item.code, ")
                .append("s.itemBatch.item.vmp.name, ")
                .append("s.itemBatch.retailsaleRate, ")
                .append("s.stock, ")
                .append("s.itemBatch.dateOfExpire, ")
                .append("s.itemBatch.item.allowFractions) ")
                .append("FROM Stock s ")
                .append("WHERE s.stock > :stockMin ")
                .append("AND s.department = :department ")
                .append("AND (");
        sql.append("s.itemBatch.item.name LIKE :query ");
        if (searchByItemCode) {
            sql.append("OR s.itemBatch.item.code LIKE :query ");
        }
        if (searchByBarcode) {
            sql.append("OR s.itemBatch.item.barcode = :query ");
        }
        if (searchByGeneric) {
            sql.append("OR s.itemBatch.item.vmp.vtm.name LIKE :query ");
        }
        sql.append(") ORDER BY s.itemBatch.item.name, s.itemBatch.dateOfExpire");
        return (List<StockDTO>) stockFacade.findLightsByJpql(sql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, 20);
    }

    public List<StockDTO> findRetailRateStockDtos(Amp amp, Department department) {
        if (amp == null || department == null) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("d", department);
        params.put("amp", amp);
        String sql = "SELECT new com.divudi.core.data.dto.StockDTO(" +
                "s.id, " +
                "s.id, " +
                "s.itemBatch.id, " +
                "s.itemBatch.item.name, " +
                "s.itemBatch.item.code, " +
                "s.itemBatch.retailsaleRate, " +
                "s.stock, " +
                "s.itemBatch.dateOfExpire, " +
                "s.itemBatch.batchNo, " +
                "s.itemBatch.purcahseRate, " +
                "s.itemBatch.wholesaleRate, " +
                "s.itemBatch.retailsaleRate, " +
                "s.itemBatch.item.allowFractions) " +
                "FROM Stock s " +
                "WHERE s.department = :d " +
                "AND s.itemBatch.item = :amp " +
                "ORDER BY s.stock DESC";
        return (List<StockDTO>) stockFacade.findLightsByJpql(sql, params);
    }

    public List<StockDTO> findCostRateStockDtos(Amp amp, Department department) {
        if (amp == null || department == null) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("d", department);
        params.put("amp", amp);
        String sql = "SELECT new com.divudi.core.data.dto.StockDTO(" +
                "s.id, " +
                "s.id, " +
                "s.itemBatch.id, " +
                "s.itemBatch.item.name, " +
                "s.itemBatch.item.code, " +
                "s.itemBatch.costRate, " +
                "s.stock, " +
                "s.itemBatch.dateOfExpire, " +
                "s.itemBatch.batchNo, " +
                "s.itemBatch.purcahseRate, " +
                "s.itemBatch.wholesaleRate, " +
                "s.itemBatch.costRate, " +
                "s.itemBatch.item.allowFractions) " +
                "FROM Stock s " +
                "WHERE s.department = :d " +
                "AND s.itemBatch.item = :amp " +
                "ORDER BY s.stock DESC";
        return (List<StockDTO>) stockFacade.findLightsByJpql(sql, params);
    }
}
