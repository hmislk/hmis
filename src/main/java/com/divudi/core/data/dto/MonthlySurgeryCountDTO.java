package com.divudi.core.data.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Month;


/**
 *
 * @author dhanesh
 */
public class MonthlySurgeryCountDTO {

    private Integer month;
    private Map<String, Long> serviceCountMap = new LinkedHashMap<>();
    private Long total;
    private boolean grandTotal;

    public MonthlySurgeryCountDTO() {
    }

    public void addServiceCount(String serviceName, Long count) {
        if (count == null) {
            count = 0L;
        }

        Long existing = serviceCountMap.get(serviceName);
        if (existing == null) {
            serviceCountMap.put(serviceName, count);
        } else {
            serviceCountMap.put(serviceName, existing + count);
        }

        if (total == null) {
            total = 0L;
        }
        total += count;
    }

    public void alignWithHeaders(List<String> headers) {

        Map<String, Long> orderedMap = new LinkedHashMap<>();

        for (String header : headers) {
            Long value = serviceCountMap.get(header);
            orderedMap.put(header, value != null ? value : 0L);
        }

        serviceCountMap = orderedMap;

        if (total == null) {
            total = 0L;
        }
    }

    public void addAll(MonthlySurgeryCountDTO other) {
        for (Map.Entry<String, Long> entry : other.getServiceCountMap().entrySet()) {
            addServiceCount(entry.getKey(), entry.getValue());
        }
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getMonthName() {
        if (month == null) {
            return "";
        }
        return Month.of(month).name(); 
    }

    public Map<String, Long> getServiceCountMap() {
        return serviceCountMap;
    }

    public void setServiceCountMap(Map<String, Long> serviceCountMap) {
        this.serviceCountMap = serviceCountMap;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public boolean isGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(boolean grandTotal) {
        this.grandTotal = grandTotal;
    }

}
