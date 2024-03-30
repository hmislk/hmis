package com.divudi.data;


/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class ItemCount {

    private String category;
    private Integer serialNo;
    private String testName;
    private Long testCount;
    private String doctor;
    private Long doctorId;
    private String service;
    private Long serviceId;
    private Long serviceCount;

    public ItemCount() {
    }

    public ItemCount(String doctor, Long doctorId, String service, Long serviceId, Long serviceCount) {
        this.doctor = doctor;
        this.doctorId = doctorId;
        this.service = service;
        this.serviceId = serviceId;
        this.serviceCount = serviceCount;
    }
    
    

    public ItemCount(String category, String testName, Long testCount) {
        this.category = category;
        this.testName = testName;
        this.testCount = testCount;
    }

    public ItemCount(String doctor, String service) {
        this.doctor = doctor;
        this.service = service;
        this.testCount=0L;
    }
    

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Long getTestCount() {
        return testCount;
    }

    public void setTestCount(Long testCount) {
        this.testCount = testCount;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(Long serviceCount) {
        this.serviceCount = serviceCount;
    }
    
    

    
}
