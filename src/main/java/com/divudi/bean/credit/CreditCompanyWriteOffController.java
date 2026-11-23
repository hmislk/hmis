package com.divudi.bean.credit;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.CreditCompanyWriteOffStatus;
import com.divudi.core.entity.CreditCompanyWriteOff;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.CreditCompanyWriteOffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.CreditCompanyClaimException;
import com.divudi.service.CreditCompanyWriteOffService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Credit Write-offs page: lists write-off requests for OPD credit bills and lets
 * a user with the approve privilege approve, reject or cancel them (#25847 part 3).
 * Every action re-checks the privilege on the server; the service refuses an
 * approval by the user who made the request.
 */
@Named
@SessionScoped
public class CreditCompanyWriteOffController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private CreditCompanyWriteOffService writeOffService;
    @EJB
    private CreditCompanyWriteOffFacade writeOffFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private Institution filterCompany;
    private CreditCompanyWriteOffStatus filterStatus;
    private Date fromDate;
    private Date toDate;
    private List<CreditCompanyWriteOff> rows = new ArrayList<>();
    /** Comment for an approval or rejection, or reason for a cancellation. */
    private String decisionText;

    public boolean hasAnyWriteOffPrivilege() {
        return webUserController.hasPrivilege("OpdCreditWriteOffRequest")
                || webUserController.hasPrivilege("OpdCreditWriteOffApprove");
    }

    public boolean isCanApprove() {
        return webUserController.hasPrivilege("OpdCreditWriteOffApprove");
    }

    private boolean deny(String privilege) {
        if (webUserController.hasPrivilege(privilege)) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this action.");
        return true;
    }

    public String navigateToWriteOffs() {
        if (!hasAnyWriteOffPrivilege()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return null;
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        filterCompany = null;
        filterStatus = CreditCompanyWriteOffStatus.REQUESTED;
        decisionText = null;
        rows = new ArrayList<>();
        search();
        return "/credit/credit_company_write_offs?faces-redirect=true";
    }

    public void search() {
        if (!hasAnyWriteOffPrivilege()) {
            rows = new ArrayList<>();
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return;
        }
        StringBuilder jpql = new StringBuilder("select w from CreditCompanyWriteOff w"
                + " where w.retired=false and w.requestedAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (filterCompany != null) {
            jpql.append(" and w.bill.creditCompany.id=:cc");
            m.put("cc", filterCompany.getId());
        }
        if (filterStatus != null) {
            jpql.append(" and w.status=:st");
            m.put("st", filterStatus);
        }
        jpql.append(" order by w.requestedAt desc, w.id desc");
        rows = writeOffFacade.findByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    public void approve(Long id) {
        if (deny("OpdCreditWriteOffApprove")) {
            return;
        }
        try {
            writeOffService.approve(id, decisionText, sessionController.getLoggedUser(), true);
            JsfUtil.addSuccessMessage("Write-off approved.");
            decisionText = null;
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        search();
    }

    public void reject(Long id) {
        if (deny("OpdCreditWriteOffApprove")) {
            return;
        }
        try {
            writeOffService.reject(id, decisionText, sessionController.getLoggedUser(), true);
            JsfUtil.addSuccessMessage("Write-off rejected.");
            decisionText = null;
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        search();
    }

    public void cancel(Long id) {
        if (deny("OpdCreditWriteOffApprove")) {
            return;
        }
        try {
            writeOffService.cancel(id, decisionText, sessionController.getLoggedUser(), true);
            JsfUtil.addSuccessMessage("Write-off cancelled. The due on the bill is restored.");
            decisionText = null;
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        search();
    }

    public CreditCompanyWriteOffStatus[] getStatuses() {
        return CreditCompanyWriteOffStatus.values();
    }

    public Institution getFilterCompany() {
        return filterCompany;
    }

    public void setFilterCompany(Institution filterCompany) {
        this.filterCompany = filterCompany;
    }

    public CreditCompanyWriteOffStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(CreditCompanyWriteOffStatus filterStatus) {
        this.filterStatus = filterStatus;
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

    public List<CreditCompanyWriteOff> getRows() {
        return rows;
    }

    public String getDecisionText() {
        return decisionText;
    }

    public void setDecisionText(String decisionText) {
        this.decisionText = decisionText;
    }
}
