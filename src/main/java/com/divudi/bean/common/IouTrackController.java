package com.divudi.bean.common;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Track IOUs: lists IOU payments with whether each is still open, settled or
 * converted to cash.
 */
@Named
@SessionScoped
public class IouTrackController implements Serializable {

    public static final String STATUS_ALL = "ALL";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SETTLED = "SETTLED";
    public static final String STATUS_CONVERTED = "CONVERTED";

    @EJB
    private PaymentFacade paymentFacade;

    private Date fromDate;
    private Date toDate;
    private String status;
    private Staff debtorStaff;
    private WebUser holder;
    private List<Payment> ious;

    public String navigateToTrackIous() {
        fromDate = CommonFunctions.getStartOfDay(CommonFunctions.getAddedDate(new Date(), -30));
        toDate = CommonFunctions.getEndOfDay(new Date());
        status = STATUS_OPEN;
        debtorStaff = null;
        holder = null;
        fillIous();
        return "/cashier/iou_track?faces-redirect=true";
    }

    public void fillIous() {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " left join p.cancelledBill cb"
                + " where p.retired=:ret"
                + " and p.paymentMethod=:pm"
                + " and p.paidValue > 0"
                // Conversion and its cancellation post IOU offsets, not IOUs given to anyone
                + " and (p.bill.billTypeAtomic is null or p.bill.billTypeAtomic not in :offsetTypes)"
                + " and p.createdAt between :fd and :td");
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("pm", PaymentMethod.IOU);
        params.put("offsetTypes", Arrays.asList(BillTypeAtomic.IOU_TO_CASH_CONVERSION,
                BillTypeAtomic.IOU_TO_CASH_CONVERSION_CANCELLED));
        params.put("fd", fromDate);
        params.put("td", toDate);
        if (STATUS_OPEN.equals(status)) {
            jpql.append(" and p.cancelled=:can and p.cancelledBill is null");
            params.put("can", false);
        } else if (STATUS_SETTLED.equals(status)) {
            jpql.append(" and p.cancelled=:can and cb.billTypeAtomic=:settle");
            params.put("can", true);
            params.put("settle", BillTypeAtomic.IOU_SETTLE);
        } else if (STATUS_CONVERTED.equals(status)) {
            jpql.append(" and p.cancelled=:can and cb.billTypeAtomic=:conv");
            params.put("can", false);
            params.put("conv", BillTypeAtomic.IOU_TO_CASH_CONVERSION);
        }
        if (debtorStaff != null) {
            jpql.append(" and p.bill.staff=:staff");
            params.put("staff", debtorStaff);
        }
        if (holder != null) {
            jpql.append(" and (p.currentHolder=:holder or (p.currentHolder is null and p.creater=:holder))");
            params.put("holder", holder);
        }
        jpql.append(" order by p.createdAt desc");
        ious = paymentFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public String iouStatus(Payment p) {
        if (p == null) {
            return "";
        }
        BillTypeAtomic closedBy = p.getCancelledBill() == null ? null : p.getCancelledBill().getBillTypeAtomic();
        if (p.isCancelled()) {
            return closedBy == BillTypeAtomic.IOU_SETTLE ? "Settled" : "Cancelled";
        }
        if (closedBy == BillTypeAtomic.IOU_TO_CASH_CONVERSION) {
            return "Converted to Cash";
        }
        if (closedBy != null) {
            return "Closed";
        }
        return "Open";
    }

    public double getTotalValue() {
        double total = 0.0;
        if (ious != null) {
            for (Payment p : ious) {
                total += p.getPaidValue();
            }
        }
        return total;
    }

    public double getOpenValue() {
        double total = 0.0;
        if (ious != null) {
            for (Payment p : ious) {
                if (!p.isCancelled() && p.getCancelledBill() == null) {
                    total += p.getPaidValue();
                }
            }
        }
        return total;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Staff getDebtorStaff() {
        return debtorStaff;
    }

    public void setDebtorStaff(Staff debtorStaff) {
        this.debtorStaff = debtorStaff;
    }

    public WebUser getHolder() {
        return holder;
    }

    public void setHolder(WebUser holder) {
        this.holder = holder;
    }

    public List<Payment> getIous() {
        return ious;
    }

    public void setIous(List<Payment> ious) {
        this.ious = ious;
    }

}
