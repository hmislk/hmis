package com.divudi.entity;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.analytics.ReportTemplateColumn;
import com.divudi.data.analytics.ReportTemplateFilter;
import com.divudi.data.analytics.ReportTemplateType;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Entity
public class ReportTemplate implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated
    private ReportTemplateType reportTemplateType;

    private String name;
    private String code;
    @Lob
    private String description;
    private int orderNo;
    private String keywords;
    @ManyToOne
    private Category category;
    @Lob
    private String filters;
    @Lob
    private String billTypes;
    @Lob
    private String columns;
    @Lob
    private String totals;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    @Transient
    private List<BillTypeAtomic> billTypeAtomics;

    @Transient
    private List<ReportTemplateColumn> reportColumns;

    @Transient
    private List<ReportTemplateFilter> reportFilters;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ReportTemplate)) {
            return false;
        }
        ReportTemplate other = (ReportTemplate) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ReportTemplate[ id=" + id + " ]";
    }

    public ReportTemplateType getReportTemplateType() {
        return reportTemplateType;
    }

    public void setReportTemplateType(ReportTemplateType reportTemplateType) {
        this.reportTemplateType = reportTemplateType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.reportFilters = null;
        this.filters = filters;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.reportColumns = null;
        this.columns = columns;
    }

    public String getTotals() {
        return totals;
    }

    public void setTotals(String totals) {
        this.totals = totals;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public String getBillTypes() {
        return billTypes;
    }

    public void setBillTypes(String billTypes) {
        this.billTypeAtomics = null;
        this.billTypes = billTypes;
    }

    public boolean matchesBillTypeAtomic(String input) {
        List<BillTypeAtomic> bts = getBillTypeAtomics();
        return bts.stream()
                .anyMatch(e -> e.name().equalsIgnoreCase(input.trim()));
    }

    public boolean matchesReportTemplateColumn(String input) {
        List<ReportTemplateColumn> columns = getReportColumns();
        return columns.stream()
                .anyMatch(e -> e.name().equalsIgnoreCase(input.trim()));
    }

    public boolean matchesReportTemplateFilter(String input) {
        List<ReportTemplateFilter> filters = getReportFilters();
        return filters.stream()
                .anyMatch(e -> e.name().equalsIgnoreCase(input.trim()));
    }

    public List<BillTypeAtomic> getBillTypeAtomics() {
        if (billTypeAtomics == null) {
            billTypeAtomics = convertToBillTypeAtomicList(this.billTypes);
        }
        return billTypeAtomics;
    }

    public List<ReportTemplateColumn> getReportColumns() {
        if (reportColumns == null) {
            reportColumns = convertToReportTemplateColumnList(this.columns);
        }
        return reportColumns;
    }

    public List<BillTypeAtomic> convertToBillTypeAtomicList(String input) {
        return Arrays.stream(input.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return BillTypeAtomic.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ReportTemplateColumn> convertToReportTemplateColumnList(String input) {
        return Arrays.stream(input.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> Arrays.stream(ReportTemplateColumn.values())
                .filter(e -> e.getLabel().equalsIgnoreCase(s))
                .findFirst()
                .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ReportTemplateFilter> convertToReportTemplateFilterList(String input) {
        return Arrays.stream(input.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> Arrays.stream(ReportTemplateFilter.values())
                .filter(e -> e.getLabel().equalsIgnoreCase(s))
                .findFirst()
                .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ReportTemplateFilter> getReportFilters() {
        if (reportFilters == null) {
            reportFilters = convertToReportTemplateFilterList(this.filters);
        }
        return reportFilters;
    }

}
