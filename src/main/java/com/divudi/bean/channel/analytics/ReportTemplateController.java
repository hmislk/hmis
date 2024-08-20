/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel.analytics;

import com.divudi.bean.common.*;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.analytics.ReportTemplateColumn;
import com.divudi.data.analytics.ReportTemplateFilter;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.entity.Department;
import com.divudi.entity.ReportTemplate;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.ReportTemplateFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * in Health Informatics
 */
@Named
@SessionScoped
public class ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ReportTemplateFacade ejbFacade;
    private ReportTemplate current;
    private List<ReportTemplate> items = null;

    private Date date;
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private WebUser user;
    private Staff staff;
    private Long startId;
    private Long endId;

    private Institution creditCompany;
    private Institution fromInstitution;
    private Department fromDepartment;
    private Institution toInstitution;
    private Department toDepartment;

    private List<ReportTemplateRow> ReportTemplateRows;
    private ReportTemplateRowBundle reportTemplateRowBundle;

    public void save(ReportTemplate reportTemplate) {
        if (reportTemplate == null) {
            return;
        }
        if (reportTemplate.getId() != null) {
            getFacade().edit(reportTemplate);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            reportTemplate.setCreatedAt(new Date());
            reportTemplate.setCreater(getSessionController().getLoggedUser());
            getFacade().create(reportTemplate);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public ReportTemplate findReportTemplateByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select a "
                + " from ReportTemplate a "
                + " where a.retired=:ret "
                + " and a.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<ReportTemplate> completeReportTemplate(String qry) {
        List<ReportTemplate> list;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select c from ReportTemplate c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        params.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new ReportTemplate();
    }

    public void recreateModel() {
        items = null;
    }

    public ReportTemplateRowBundle generateReport(
            ReportTemplateType type,
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle;

        switch (type) {
            case BILL_NET_TOTAL:
                bundle = handleBillTypeAtomicTotalUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS:
                bundle = handleBillTypeAndPaymentMethodSummaryPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_USING_BILLS:
                bundle = handleBillTypeAndPaymentMethodSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_FEES:
                bundle = handleBillFeeGroupedByBillTypeAtomic(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_FEE_GROUPED_BY_TO_DEPARTMENT_AND_CATEGORY:
                bundle = handleBillFeeGroupedByToDepartmentAndCategory(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_FEE_LIST:
                bundle = handleBillFeeList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_ITEM_LIST:
                bundle = handleBillItemList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_LIST:
                bundle = handleBillList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS:
                bundle = handleBillTypeAtomicSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_PAYMENTS:
                bundle = handleBillTypeAtomicSummaryUsingPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ENCOUNTER_LIST:
                bundle = handleEncounterList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PATIENT_LIST:
                bundle = handlePatientList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_METHOD_SUMMARY_USING_BILLS:
                bundle = handlePaymentMethodSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_METHOD_SUMMARY_USING_PAYMENTS:
                bundle = handlePaymentMethodSummaryUsingPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_TYPE_SUMMARY_PAYMENTS:
                bundle = handlePaymentTypeSummaryPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_TYPE_SUMMARY_USING_BILLS:
                bundle = handlePaymentTypeSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_FEE:
                bundle = handleItemCategorySummaryByBillFee(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_SUMMARY_BY_BILL:
                bundle = handleItemSummaryByBill(btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM:
                bundle = handleItemDepartmentummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM:
            case ITEM_CATEGORY_SUMMARY_BY_BILL:
                bundle = handleItemCategorySummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_FEE:
                bundle = handleToDepartmentSummaryByBillFee(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_ITEM:
                bundle = handleToDepartmentSummaryByBillItem(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;

            case TO_DEPARTMENT_SUMMARY_BY_BILL:
                bundle = handleToDepartmentSummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case SESSION_INSTANCE_LIST:
                bundle = handleSessionInstanceList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            default:
                JsfUtil.addErrorMessage("Unknown Report Type");
                return null;
        }
        return bundle;
    }

    public String processReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        if (current.getReportTemplateType() == null) {
            JsfUtil.addErrorMessage("No report Type");
            return "";
        }
        reportTemplateRowBundle = generateReport(current.getReportTemplateType(), current.getBillTypeAtomics(), date, fromDate, toDate, institution, department, fromInstitution, fromDepartment, toInstitution, toDepartment, user, creditCompany, startId, endId);
        reportTemplateRowBundle.setReportTemplate(current);
        return "";
    }

    private ReportTemplateRowBundle handleBillFeeGroupedByBillTypeAtomic(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillFeeGroupedByToDepartmentAndCategory(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAtomicSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, count(bill), sum(bill.netTotal)) "
                + " from Bill bill "
                + " where bill.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAtomicTotalUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        // Initialize the total to 0
        double totalNetAmount = 0.0;

        jpql = "select sum(bill.netTotal) "
                + " from Bill bill "
                + " where bill.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        Double sumResult = ejbFacade.findSingleResultByJpql(jpql, parameters, TemporalType.DATE);

        if (sumResult != null) {
            totalNetAmount = sumResult;
        }

        bundle.setTotal(totalNetAmount);

        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAndPaymentMethodSummaryPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillTypeAndPaymentMethodSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillFeeList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillItemList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillTypeAtomicSummaryUsingPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleEncounterList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePatientList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentMethodSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentMethodSummaryUsingPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentTypeSummaryPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentTypeSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBillFee(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBillItem(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bi.item.category.name, sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null "
                + " and bi.item.category is not null ";

        jpql += " group by bi.item.category ";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bi.item.category, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null "
                + " and bi.item.category is not null ";

        jpql += " group by bi.item.category ";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemDepartmentummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        System.out.println("handleItemDepartmentummaryByBill");

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bi.item.department, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

//        jpql += " and bi.item is not null "
//                + " and bi.item.department is not null ";
        jpql += " group by bi.item.department ";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        System.out.println("rs = " + rs);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        bundle.setTotal(total);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemSummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bi.item, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.billDate=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.billDate < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.billDate > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null ";

        jpql += " group by bi.item ";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleSessionInstanceList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow(ss) "
                + " from SessionInstance ss "
                + " where ss.retired<>:br ";
        parameters.put("br", true);

        if (paramDate != null) {
            jpql += " and ss.sessionDate=:bd";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and ss.sessionDate < :td";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and ss.sessionDate > :fd";
            parameters.put("fd", paramFromDate);
        }

        if (paramInstitution != null) {
            jpql += " and ss.institution=:ins";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and ss.department=:dep";
            parameters.put("dep", paramDepartment);
        }

        if (paramUser != null) {
            jpql += " and ss.creater=:wu";
            parameters.put("wu", paramUser);
        }

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
            return null;
        } else {
            System.out.println("Results found: " + rs.size());
        }

        
        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBillFee(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBillItem(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    public void saveSelected() {
        if (getCurrent().getName().isEmpty() || getCurrent().getName() == null) {
            JsfUtil.addErrorMessage("Please enter Value");
            return;
        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public ReportTemplateFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReportTemplateFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ReportTemplateController() {
    }

    public ReportTemplate getCurrent() {
        if (current == null) {
            current = new ReportTemplate();
        }
        return current;
    }

    public void setCurrent(ReportTemplate current) {
        this.current = current;
    }

    private List<ReportTemplateColumn> getReportTemplateColumns(String input) {
        List<ReportTemplateColumn> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateColumn column : ReportTemplateColumn.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<ReportTemplateFilter> getReportTemplateFilters(String input) {
        List<ReportTemplateFilter> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateFilter column : ReportTemplateFilter.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<BillTypeAtomic> getBillTypeAtomics(String input) {
        List<BillTypeAtomic> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (BillTypeAtomic column : BillTypeAtomic.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ReportTemplateFacade getFacade() {
        return ejbFacade;
    }

    public String navigateToReportTemplateList() {
        items = getAllItems();
        return "/dataAdmin/report_template_list";
    }

    public String navigateToAddNewReportTemplate() {
        current = new ReportTemplate();
        return "/dataAdmin/report_template";
    }

    public void deleteReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        save(current);
        items = getAllItems();
        JsfUtil.addSuccessMessage("Removed");
    }

    public List<ReportTemplateType> getReportTemplateTypes() {
        return Arrays.asList(ReportTemplateType.values());
    }

    public String navigateToEditReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report_template?faces-redirect=true";
    }

    public String navigateToGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report?faces-redirect=true";
    }

    public String navigateToEditGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report";
    }

    public List<ReportTemplate> getAllItems() {
        List<ReportTemplate> allItems;
        String j;
        j = "select a "
                + " from ReportTemplate a "
                + " where a.retired=false "
                + " order by a.name";
        allItems = getFacade().findByJpql(j);
        return allItems;
    }

    public List<ReportTemplate> getItems() {
        return items;
    }

    public Date getDate() {
        if(date==null){
            date=CommonFunctions.getStartOfDay();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getFromDate() {
        if(fromDate==null){
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if(toDate==null){
            toDate = CommonFunctions.getEndOfDay();
        }
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<ReportTemplateRow> getReportTemplateRows() {
        return ReportTemplateRows;
    }

    public void setReportTemplateRows(List<ReportTemplateRow> ReportTemplateRows) {
        this.ReportTemplateRows = ReportTemplateRows;
    }

    public ReportTemplateRowBundle getReportTemplateRowBundle() {
        return reportTemplateRowBundle;
    }

    public void setReportTemplateRowBundle(ReportTemplateRowBundle reportTemplateRowBundle) {
        this.reportTemplateRowBundle = reportTemplateRowBundle;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Long getStartId() {
        return startId;
    }

    public void setStartId(Long startId) {
        this.startId = startId;
    }

    public Long getEndId() {
        return endId;
    }

    public void setEndId(Long endId) {
        this.endId = endId;
    }

    public static class ReportTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReportTemplateController controller = (ReportTemplateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reportTemplateController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof ReportTemplate) {
                ReportTemplate o = (ReportTemplate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ReportTemplate.class.getName());
            }
        }
    }

}
