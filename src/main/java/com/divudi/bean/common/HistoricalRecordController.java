/*
 * Created by ChatGPT on request by Dr. M. H. B. Ariyaratne
 *
 */
package com.divudi.bean.common;

import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.data.HistoricalRecordType;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.service.HistoricalRecordService;
import com.divudi.core.data.StockValueRow;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;
import com.divudi.service.StockService;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class HistoricalRecordController implements Serializable, ControllerWithReportFilters {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;

    @EJB
    private HistoricalRecordFacade ejbFacade;
    @EJB
    HistoricalRecordService historicalRecordService;
    @EJB
    StockService stockService;

    private HistoricalRecord current;
    private HistoricalRecord selected;
    private List<HistoricalRecord> items = null;
    private List<HistoricalRecordType> historicalRecordTypes;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private HistoricalRecordType historicalRecordType;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;
    private List<ReportViewType> reportViewTypes;

    public String navigateToHistoricalRecordList() {
        recreateModel();
        return "/dataAdmin/historical_record_list?faces-redirect=true";
    }

    public String navigateToHistoricalRecordGenerate() {
        recreateModel();
        return "/dataAdmin/historical_record_generate?faces-redirect=true";
    }

    public List<HistoricalRecordType> getHistoricalRecordTypes() {
        if (historicalRecordTypes == null) {
            historicalRecordTypes = historicalRecordService.fetchHistoricalRecordTypes();
        }
        return historicalRecordTypes;
    }

    public HistoricalRecordController() {
    }

    public HistoricalRecord findRecord(HistoricalRecordType historicalRecordType, Date recordDate) {
        if (historicalRecordType == null || recordDate == null) {
            return null;
        }
        return findRecord(historicalRecordType, null, null, null, recordDate);
    }

    public HistoricalRecord findRecord(HistoricalRecordType historicalRecordType, Institution institution, Date recordDate) {
        if (historicalRecordType == null || recordDate == null) {
            return null;
        }
        return findRecord(historicalRecordType, institution, null, null, recordDate);
    }

    public HistoricalRecord findRecord(HistoricalRecordType historicalRecordType, Institution institution, Department department, Date recordDate) {
        if (historicalRecordType == null || recordDate == null) {
            return null;
        }
        return findRecord(historicalRecordType, institution, null, department, recordDate);
    }

    public HistoricalRecord findRecord(HistoricalRecordType historicalRecordType, Institution institution, Institution site, Department department, Date recordDate) {
        return historicalRecordService.findRecord(historicalRecordType, institution, site, department, recordDate);
    }

    public void processHistoricalRecordList() {
        items = historicalRecordService.findRecords(
                historicalRecordType,
                institution,
                site,
                department,
                fromDate,
                toDate
        );
    }

    public String processCreateHistoricalRecord() {
        System.out.println("processCreateHistoricalRecord");
        System.out.println("historicalRecordType = " + historicalRecordType);
        if (historicalRecordType == null) {
            JsfUtil.addErrorMessage("Please select a variable");
            return null;
        }

        selected = handleGeneration(historicalRecordType);
        System.out.println("selected = " + selected);
        return navigateToHistoricalRecordList();
    }

    private HistoricalRecord handleGeneration(HistoricalRecordType type) {
        System.out.println("handleGeneration");
        System.out.println("type = " + type);
        if (type == null) {
            return null;
        }
        switch (type) {
            case PHARMACY_STOCK_VALUE_PURCHASE_RATE:
                return generatePharmacyStockValuePurchaseRate();
            case PHARMACY_STOCK_VALUE_RETAIL_RATE:
                return generatePharmacyStockValueRetailRate();
            case PHARMACY_STOCK_VALUE_COST_RATE:
                return generatePharmacyStockValueCostRate();
            case COLLECTION_CENTRE_BALANCE:
                return generateCollectionCentreBalance();
            case CREDIT_COMPANY_BALANCE:
                return generateCreditCompanyBalance();
            case DRAWER_BALANCE:
                return generateDrawerBalance();
            default:
                return null;
        }
    }

    private HistoricalRecord buildRecord(HistoricalRecordType type) {
        HistoricalRecord hr = new HistoricalRecord();
        hr.setHistoricalRecordType(type);
        hr.setInstitution(institution);
        hr.setSite(site);
        hr.setDepartment(department);
        hr.setRecordDate(new Date());
        hr.setRecordDateTime(new Date());
        hr.setRecordValue(0.0);
        return hr;
    }

    private HistoricalRecord generatePharmacyStockValuePurchaseRate() {
        System.out.println("generatePharmacyStockValuePurchaseRate");
        HistoricalRecord rec = buildRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_PURCHASE_RATE);
        System.out.println("rec = " + rec);
        StockValueRow result = stockService.calculateStockValues(institution, site, department);
        System.out.println("result = " + result);
        if (result != null) {
            rec.setRecordValue(result.getPurchaseValue());
        }
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    private HistoricalRecord generatePharmacyStockValueRetailRate() {
        HistoricalRecord rec = buildRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_RETAIL_RATE);
        StockValueRow result = stockService.calculateStockValues(institution, site, department);
        if (result != null) {
            rec.setRecordValue(result.getRetailValue());
        }
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    private HistoricalRecord generatePharmacyStockValueCostRate() {
        HistoricalRecord rec = buildRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_COST_RATE);
        StockValueRow result = stockService.calculateStockValues(institution, site, department);
        if (result != null) {
            rec.setRecordValue(result.getCostValue());
        }
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    private HistoricalRecord generateCollectionCentreBalance() {
        HistoricalRecord rec = buildRecord(HistoricalRecordType.COLLECTION_CENTRE_BALANCE);
        // TODO: implement generation logic
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    private HistoricalRecord generateCreditCompanyBalance() {
        HistoricalRecord rec = buildRecord(HistoricalRecordType.CREDIT_COMPANY_BALANCE);
        // TODO: implement generation logic
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    private HistoricalRecord generateDrawerBalance() {
        HistoricalRecord rec = buildRecord(HistoricalRecordType.DRAWER_BALANCE);
        // TODO: implement generation logic
        historicalRecordService.createHistoricalRecord(rec);
        return rec;
    }

    public HistoricalRecord getCurrent() {
        if (current == null) {
            current = new HistoricalRecord();
        }
        return current;
    }

    public void setCurrent(HistoricalRecord current) {
        this.current = current;
    }

    public HistoricalRecord getSelected() {
        return selected;
    }

    public void setSelected(HistoricalRecord selected) {
        this.selected = selected;
    }

    private HistoricalRecordFacade getFacade() {
        return ejbFacade;
    }

    public List<HistoricalRecord> getItems() {
        return items;
    }

    public void recreateModel() {
        items = null;
    }

    public HistoricalRecordFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(HistoricalRecordFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public HistoricalRecordType getHistoricalRecordType() {
        return historicalRecordType;
    }

    public void setHistoricalRecordType(HistoricalRecordType historicalRecordType) {
        this.historicalRecordType = historicalRecordType;
    }

    @Override
    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    @Override
    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Override
    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    @Override
    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    @Override
    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    @Override
    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public List<ReportViewType> getReportViewTypes() {
        return reportViewTypes;
    }

    public void setReportViewTypes(List<ReportViewType> reportViewTypes) {
        this.reportViewTypes = reportViewTypes;
    }



    @FacesConverter(forClass = HistoricalRecord.class)
    public static class HistoricalRecordConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            HistoricalRecordController controller = (HistoricalRecordController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "historicalRecordController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException e) {
//                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid ID format: " + value, e); No Logger is Implemented yet. Will be done later
                return null;
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            if (value == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof HistoricalRecord) {
                HistoricalRecord o = (HistoricalRecord) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + HistoricalRecord.class.getName());
            }
        }
    }

}
