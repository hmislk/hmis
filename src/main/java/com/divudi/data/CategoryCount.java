package com.divudi.data;

import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class CategoryCount {

    private String category;
    private List<ItemCount> items;
    private Long total;
    private Integer serialNo;

    // Constructor matching the provided arguments
    public CategoryCount(String category, List<ItemCount> items, Long total) {
        this.category = category;
        this.items = items;
        this.total = total;
    }

    public CategoryCount() {
    }

    

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<ItemCount> getItems() {
        return items;
    }

    public void setItems(List<ItemCount> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

}
