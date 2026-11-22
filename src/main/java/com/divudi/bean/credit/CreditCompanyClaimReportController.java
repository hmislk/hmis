package com.divudi.bean.credit;

import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.dto.CreditCompanyClaimAgeingClaimDto;
import com.divudi.core.data.dto.CreditCompanyClaimAgeingRowDto;
import com.divudi.core.data.dto.CreditCompanyClaimDueDto;
import com.divudi.core.data.dto.CreditCompanyClaimStatusReportRowDto;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.CreditCompanyClaimReportMath;
import com.divudi.service.CreditCompanyClaimService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Credit company claim reports (#25847): Claims by Status and Claim Ageing.
 * Both are guarded by the OpdCreditClaimReports privilege, checked again on the
 * server for every search.
 */
@Named
@SessionScoped
public class CreditCompanyClaimReportController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = "OpdCreditClaimReports";
    /** Claim ids per bill-due query, to stay clear of IN-list limits. */
    private static final int ID_BATCH = 500;

    @EJB
    private CreditCompanyClaimFacade claimFacade;
    @Inject
    private WebUserController webUserController;

    // Claims by Status
    private Date fromDate;
    private Date toDate;
    private Institution filterCompany;
    private CreditCompanyClaimStatus filterStatus;
    private List<CreditCompanyClaimStatusReportRowDto> statusRows = new ArrayList<>();
    private double totalClaimed;
    private double totalOutstanding;
    private double totalReceivedOrClosed;

    // Claim Ageing
    private List<CreditCompanyClaimAgeingRowDto> ageingRows = new ArrayList<>();
    private Date ageingAsOf;
    private double ageTotal0To30;
    private double ageTotal31To60;
    private double ageTotal61To90;
    private double ageTotalOver90;
    private double ageTotal;

    public boolean isReportsAllowed() {
        return webUserController.hasPrivilege(PRIVILEGE);
    }

    private boolean denied() {
        if (isReportsAllowed()) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this report.");
        return true;
    }

    public String navigateToClaimsByStatus() {
        if (denied()) {
            return null;
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        filterCompany = null;
        filterStatus = null;
        statusRows = new ArrayList<>();
        totalClaimed = 0;
        totalOutstanding = 0;
        totalReceivedOrClosed = 0;
        return "/credit/credit_company_claims_by_status?faces-redirect=true";
    }

    public String navigateToClaimAgeing() {
        if (denied()) {
            return null;
        }
        filterCompany = null;
        clearAgeing();
        return "/credit/credit_company_claim_ageing?faces-redirect=true";
    }

    private void clearAgeing() {
        ageingRows = new ArrayList<>();
        ageingAsOf = null;
        ageTotal0To30 = 0;
        ageTotal31To60 = 0;
        ageTotal61To90 = 0;
        ageTotalOver90 = 0;
        ageTotal = 0;
    }

    public void searchClaimsByStatus() {
        statusRows = new ArrayList<>();
        totalClaimed = 0;
        totalOutstanding = 0;
        totalReceivedOrClosed = 0;
        if (denied()) {
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select the from and to dates.");
            return;
        }
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.CreditCompanyClaimStatusReportRowDto("
                + "c.id, c.claimNo, cc.name, d.name, c.createdAt, c.submittedAt, c.status, c.resubmitCount, c.claimedTotal)"
                + " from CreditCompanyClaim c left join c.creditCompany cc left join c.department d"
                + " where c.retired=false and c.createdAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (filterCompany != null) {
            jpql.append(" and cc.id=:cc");
            m.put("cc", filterCompany.getId());
        }
        if (filterStatus != null) {
            jpql.append(" and c.status=:st");
            m.put("st", filterStatus);
        }
        jpql.append(" order by c.createdAt desc, c.id desc");
        List<CreditCompanyClaimStatusReportRowDto> found
                = (List<CreditCompanyClaimStatusReportRowDto>) claimFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        if (found == null) {
            found = new ArrayList<>();
        }
        Map<Long, Double> due;
        try {
            due = fetchDueByClaimId(idsOf(found));
        } catch (RuntimeException e) {
            JsfUtil.addErrorMessage("The outstanding amounts could not be read. Try again.");
            return;
        }
        double claimed = 0;
        double outstanding = 0;
        for (CreditCompanyClaimStatusReportRowDto r : found) {
            if (r.isCancelled()) {
                r.setOutstanding(0.0);
                continue;
            }
            Double d = due.get(r.getId());
            r.setOutstanding(d == null ? 0.0 : CreditCompanyClaimReportMath.money(d));
            claimed += r.getClaimedTotal();
            outstanding += r.getOutstanding();
        }
        statusRows = found;
        totalClaimed = CreditCompanyClaimReportMath.money(claimed);
        totalOutstanding = CreditCompanyClaimReportMath.money(outstanding);
        totalReceivedOrClosed = CreditCompanyClaimReportMath.receivedOrClosed(totalClaimed, totalOutstanding);
    }

    /** Statuses whose claims are submitted and still waiting for the company. */
    public static List<CreditCompanyClaimStatus> ageingStatuses() {
        return Arrays.asList(CreditCompanyClaimStatus.SUBMITTED, CreditCompanyClaimStatus.RESUBMITTED,
                CreditCompanyClaimStatus.PARTIALLY_SETTLED);
    }

    public void searchClaimAgeing() {
        clearAgeing();
        if (denied()) {
            return;
        }
        Date asOf = new Date();
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.CreditCompanyClaimAgeingClaimDto("
                + "c.id, cc.id, cc.name, c.submittedAt, c.status)"
                + " from CreditCompanyClaim c left join c.creditCompany cc"
                + " where c.retired=false and c.status in :open and c.submittedAt is not null");
        Map<String, Object> m = new HashMap<>();
        m.put("open", ageingStatuses());
        if (filterCompany != null) {
            jpql.append(" and cc.id=:cc");
            m.put("cc", filterCompany.getId());
        }
        List<CreditCompanyClaimAgeingClaimDto> claims
                = (List<CreditCompanyClaimAgeingClaimDto>) claimFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        if (claims == null) {
            claims = new ArrayList<>();
        }
        Map<Long, Double> due;
        try {
            List<Long> ids = new ArrayList<>();
            for (CreditCompanyClaimAgeingClaimDto c : claims) {
                ids.add(c.getId());
            }
            due = fetchDueByClaimId(ids);
        } catch (RuntimeException e) {
            JsfUtil.addErrorMessage("The outstanding amounts could not be read. Try again.");
            return;
        }
        Map<Long, CreditCompanyClaimAgeingRowDto> perCompany = new LinkedHashMap<>();
        for (CreditCompanyClaimAgeingClaimDto c : claims) {
            Double d = due.get(c.getId());
            double outstanding = d == null ? 0.0 : CreditCompanyClaimReportMath.money(d);
            if (outstanding <= CreditCompanyClaimService.MIN_DUE) {
                continue;
            }
            Long key = c.getCompanyId() == null ? Long.valueOf(0L) : c.getCompanyId();
            CreditCompanyClaimAgeingRowDto row = perCompany.get(key);
            if (row == null) {
                row = new CreditCompanyClaimAgeingRowDto(c.getCompanyId(),
                        c.getCompanyName() == null ? "(no company)" : c.getCompanyName());
                perCompany.put(key, row);
            }
            row.add(CreditCompanyClaimReportMath.bucketOf(c.getSubmittedAt(), asOf), outstanding);
        }
        List<CreditCompanyClaimAgeingRowDto> out = new ArrayList<>(perCompany.values());
        out.sort((a, b) -> a.getCompanyName().compareToIgnoreCase(b.getCompanyName()));
        for (CreditCompanyClaimAgeingRowDto r : out) {
            ageTotal0To30 += r.getDays0To30();
            ageTotal31To60 += r.getDays31To60();
            ageTotal61To90 += r.getDays61To90();
            ageTotalOver90 += r.getOver90();
        }
        ageTotal0To30 = CreditCompanyClaimReportMath.money(ageTotal0To30);
        ageTotal31To60 = CreditCompanyClaimReportMath.money(ageTotal31To60);
        ageTotal61To90 = CreditCompanyClaimReportMath.money(ageTotal61To90);
        ageTotalOver90 = CreditCompanyClaimReportMath.money(ageTotalOver90);
        ageTotal = CreditCompanyClaimReportMath.money(ageTotal0To30 + ageTotal31To60 + ageTotal61To90 + ageTotalOver90);
        ageingRows = out;
        ageingAsOf = asOf;
    }

    private static List<Long> idsOf(List<CreditCompanyClaimStatusReportRowDto> rows) {
        List<Long> ids = new ArrayList<>();
        for (CreditCompanyClaimStatusReportRowDto r : rows) {
            ids.add(r.getId());
        }
        return ids;
    }

    /**
     * Current due of each claim's bills (net total less refunds less
     * settlements, the Service due expression), summed per claim; one query per
     * batch of claim ids, never one per claim. A query failure propagates.
     */
    private Map<Long, Double> fetchDueByClaimId(Collection<Long> claimIds) {
        Map<Long, Double> out = new HashMap<>();
        List<Long> all = new ArrayList<>(claimIds);
        for (int from = 0; from < all.size(); from += ID_BATCH) {
            List<Long> batch = all.subList(from, Math.min(all.size(), from + ID_BATCH));
            Map<String, Object> m = new HashMap<>();
            m.put("ids", new ArrayList<>(batch));
            List<?> found = claimFacade.findLightsByJpqlStrict(
                    "select new com.divudi.core.data.dto.CreditCompanyClaimDueDto("
                    // Grouped on purpose: EclipseLink reads "a - b - c" as "a - (b - c)".
                    + "ci.claim.id, sum(b.netTotal - (b.refundAmount + b.paidAmount)))"
                    + " from CreditCompanyClaimItem ci join ci.bill b"
                    + " where ci.retired=false and ci.claim.id in :ids group by ci.claim.id", m);
            for (Object o : found) {
                CreditCompanyClaimDueDto dto = (CreditCompanyClaimDueDto) o;
                out.put(dto.getClaimId(), dto.getDue());
            }
        }
        return out;
    }

    public CreditCompanyClaimStatus[] getStatuses() {
        return CreditCompanyClaimStatus.values();
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

    public Institution getFilterCompany() {
        return filterCompany;
    }

    public void setFilterCompany(Institution filterCompany) {
        this.filterCompany = filterCompany;
    }

    public CreditCompanyClaimStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(CreditCompanyClaimStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public List<CreditCompanyClaimStatusReportRowDto> getStatusRows() {
        return statusRows;
    }

    public double getTotalClaimed() {
        return totalClaimed;
    }

    public double getTotalOutstanding() {
        return totalOutstanding;
    }

    public double getTotalReceivedOrClosed() {
        return totalReceivedOrClosed;
    }

    public List<CreditCompanyClaimAgeingRowDto> getAgeingRows() {
        return ageingRows;
    }

    public Date getAgeingAsOf() {
        return ageingAsOf;
    }

    public double getAgeTotal0To30() {
        return ageTotal0To30;
    }

    public double getAgeTotal31To60() {
        return ageTotal31To60;
    }

    public double getAgeTotal61To90() {
        return ageTotal61To90;
    }

    public double getAgeTotalOver90() {
        return ageTotalOver90;
    }

    public int getAgeTotalClaims() {
        int total = 0;
        if (ageingRows != null) {
            for (CreditCompanyClaimAgeingRowDto row : ageingRows) {
                total += row.getClaimCount();
            }
        }
        return total;
    }

    public double getAgeTotal() {
        return ageTotal;
    }
}
