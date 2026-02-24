
package com.divudi.core.data.dto;

/**
 *
 * @author dhanesh
 */
public class MonthServiceCountDTO {
    
    private Integer month; 
    private String serviceName;
    private Long serviceCount;

    public MonthServiceCountDTO(Integer month, String serviceName, Long serviceCount) {
        this.month = month;
        this.serviceName = serviceName;
        this.serviceCount = serviceCount;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(Long serviceCount) {
        this.serviceCount = serviceCount;
    }
    
    
}
