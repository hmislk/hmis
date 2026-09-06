package com.divudi.core.data.dataStructure;

import java.io.Serializable;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public class AnalyzerInvestigationCountRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long investigationId;
    private String code;
    private String testName;
    private String departmentName;
    private String analyzerName;
    private Long count;

    public AnalyzerInvestigationCountRow() {
    }

    // Used by JPQL constructor query in
    // com.divudi.bean.report.AnalyzerInvestigationCountController#createAnalyzerInvestigationCounts()
    public AnalyzerInvestigationCountRow(Long investigationId, String code, String testName, String departmentName, String analyzerName, Long count) {
        this.investigationId = investigationId;
        this.code = code;
        this.testName = testName;
        this.departmentName = departmentName;
        this.analyzerName = analyzerName;
        this.count = count;
    }

    public Long getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(Long investigationId) {
        this.investigationId = investigationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAnalyzerName() {
        return analyzerName;
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

}
