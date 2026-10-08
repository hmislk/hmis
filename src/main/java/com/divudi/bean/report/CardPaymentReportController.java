package com.divudi.bean.report;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.CardPaymentReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Card Payments report: lists Payment rows of any bill type, filtered by
 * payment method (default Credit Card), with bank, card reference number and
 * comments. Cancellation / refund payments are stored as negative payments and
 * are included, so the total nets correctly.
 */
@Named
@SessionScoped
public class CardPaymentReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PaymentFacade paymentFacade;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private PaymentMethod paymentMethod = PaymentMethod.Card;
    private Institution bank;
    private BillTypeAtomic billTypeAtomic;
    private List<CardPaymentReportDTO> rows;
    private double total;

    public String navigateToCardPaymentReport() {
        fromDate = CommonFunctions.getStartOfDay();
        toDate = CommonFunctions.getEndOfDay();
        institution = null;
        site = null;
        department = null;
        paymentMethod = PaymentMethod.Card;
        bank = null;
        billTypeAtomic = null;
        rows = null;
        total = 0.0;
        return "/reports/financialReports/card_payments?faces-redirect=true";
    }

    public void process() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select the from and to dates");
            return;
        }
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.CardPaymentReportDTO("
                + "p.id, b.id, b.deptId, b.billTypeAtomic, p.createdAt, p.paymentMethod, "
                + "per.name, bk.name, p.creditCardRefNo, p.referenceNo, p.comments, p.paidValue, "
                + "cashierPerson.name, d.name) "
                + "from Payment p "
                + "left join p.bill b "
                + "left join b.patient pt "
                + "left join pt.person per "
                + "left join p.bank bk "
                + "left join p.creater cashier "
                + "left join cashier.webUserPerson cashierPerson "
                + "left join p.department d "
                + "where p.retired=:ret "
                + "and p.createdAt between :fromDate and :toDate ");
        params.put("ret", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        if (paymentMethod != null) {
            jpql.append("and p.paymentMethod=:pm ");
            params.put("pm", paymentMethod);
        }
        if (institution != null) {
            jpql.append("and d.institution=:ins ");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append("and d.site=:site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append("and d=:dept ");
            params.put("dept", department);
        }
        if (bank != null) {
            jpql.append("and bk=:bank ");
            params.put("bank", bank);
        }
        if (billTypeAtomic != null) {
            jpql.append("and b.billTypeAtomic=:bta ");
            params.put("bta", billTypeAtomic);
        }
        jpql.append("order by p.createdAt, p.id");
        rows = (List<CardPaymentReportDTO>) paymentFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (rows == null) {
            rows = new ArrayList<>();
        }
        total = 0.0;
        for (CardPaymentReportDTO r : rows) {
            total += r.getPaidValue();
        }
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public List<CardPaymentReportDTO> getRows() {
        return rows;
    }

    public double getTotal() {
        return total;
    }
}
