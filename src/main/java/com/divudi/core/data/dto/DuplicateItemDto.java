package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Lightweight projection of a pharmaceutical item used by the "Possible
 * Duplicate Items" report (Pharmaceutical Management &gt; Check Entered Data).
 *
 * Carries just enough to let an administrator recognise a duplicate and locate
 * it in the AMP/AMPP/VMP/VMPP screens, plus the total stock across all
 * departments so they can tell which of the duplicates actually holds stock.
 */
public class DuplicateItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String barcode;
    private String type;
    private Double totalStock;
    /**
     * Item id under which stock is actually held. Equals {@link #id} for AMP/VMP
     * items, but for an AMPP/VMPP it is the backing AMP/VMP id, because stock is
     * keyed by the AMP (see {@code StockController.listStocksOfSelectedItem}).
     * Used only to sum the correct stock; the displayed identity stays {@link #id}.
     */
    private Long stockItemId;

    public DuplicateItemDto() {
    }

    public DuplicateItemDto(Long id, String name, String code, String barcode, String type) {
        this(id, name, code, barcode, type, id);
    }

    public DuplicateItemDto(Long id, String name, String code, String barcode, String type, Long stockItemId) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.type = type;
        this.stockItemId = stockItemId != null ? stockItemId : id;
        this.totalStock = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Double totalStock) {
        this.totalStock = totalStock;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(Long stockItemId) {
        this.stockItemId = stockItemId;
    }
}
