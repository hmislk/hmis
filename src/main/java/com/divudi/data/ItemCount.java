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

    public ItemCount() {
    }

    public ItemCount(String category, String testName, Long testCount) {
        this.category = category;
        this.testName = testName;
        this.testCount = testCount;
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


}
