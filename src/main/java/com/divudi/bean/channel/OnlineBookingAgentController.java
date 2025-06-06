package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.OnlineBooking;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.OnlineBookingFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.ChannelService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Chinthaka Prasad
 */
@Named
@SessionScoped
public class OnlineBookingAgentController implements Serializable {

    private Institution current;
    private List<Institution> allAgents;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;

    private Institution institutionForBookings;
    private Institution agentForBookings;
    private boolean paidStatus;
    private List<OnlineBooking> paidToHospitalList;

    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private SessionController sessionController;

    @EJB
    private ChannelService channelService;
    private List<OnlineBooking> onlineBookingList;

    @EJB
    private OnlineBookingFacade onlineBookingFacade;
    @EJB
    private BillFacade billFacade;

    private PaymentMethod paidToHospitalPaymentMethod;
    private Bill paidToHospitalBill;
    private PaymentMethodData paymentMethodData;
    private double paidToHospitalTotal;

    public double getPaidToHospitalTotal() {
        return paidToHospitalTotal;
    }

    public void setPaidToHospitalTotal(double paidToHospitalTotal) {
        this.paidToHospitalTotal = paidToHospitalTotal;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Bill getPaidToHospitalBill() {
        if (paidToHospitalBill == null) {
            paidToHospitalBill = new BilledBill();
            paidToHospitalBill.setBillType(BillType.ChannelOnlineBookingAgentPaidToHospital);
        }
        return paidToHospitalBill;
    }

    public void setPaidToHospitalBill(Bill paidToHospitalBill) {
        this.paidToHospitalBill = paidToHospitalBill;
    }

    public PaymentMethod getPaidToHospitalPaymentMethod() {
        return paidToHospitalPaymentMethod;
    }

    public void setPaidToHospitalPaymentMethod(PaymentMethod paidToHospitalPaymentMethod) {
        this.paidToHospitalPaymentMethod = paidToHospitalPaymentMethod;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public OnlineBookingFacade getOnlineBookingFacade() {
        return onlineBookingFacade;
    }

    public void setOnlineBookingFacade(OnlineBookingFacade onlineBookingFacade) {
        this.onlineBookingFacade = onlineBookingFacade;
    }

    public void setTotalForPaymentMethodData(){
        if(paidToHospitalPaymentMethod != null && paidToHospitalBill.getNetTotal() >0){
            switch (paidToHospitalPaymentMethod) {
                case Cash: 
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }
    
    public Bill createHospitalPaymentBill(List<OnlineBooking> bookings) {
        Bill paidBill = getPaidToHospitalBill();
        paidBill.setCreatedAt(new Date());
        paidBill.setCreater(getSessionController().getLoggedUser());
        paidBill.setToInstitution(institutionForBookings);
        paidBill.setCreditCompany(agentForBookings);
        paidBill.setTotal(createTotalAmountToPay());
        paidBill.setBalance(0d);
        getBillFacade().create(paidBill);
        return paidBill;

    }

    public void createPaymentForHospital() {
        if (paidToHospitalList == null || paidToHospitalList.isEmpty()) {
            JsfUtil.addErrorMessage("No Bookings are selected to proceed");
        }

        for (OnlineBooking ob : paidToHospitalList) {
            if (!ob.isPaidToHospital()) {
                ob.setPaidToHospital(true);
                ob.setPaidToHospitalDate(new Date());
                ob.setPaidToHospitalProcessedBy(getSessionController().getLoggedUser());
                getOnlineBookingFacade().edit(ob);
            }
        }
    }

    public void onRowSelect(SelectEvent<OnlineBooking> event) {
        System.out.println("line 151");
        OnlineBooking selected = event.getObject();

        if (selected.isPaidToHospital()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Selection is not allowed", "This Booking is already paid."));
            paidToHospitalList.remove(selected); // manually remove it
        } else {
            System.out.println("line 158");
            paidToHospitalList.add(selected);
        }

    }
    
    public void onRowUnselect(SelectEvent<OnlineBooking> event){
        OnlineBooking selected = event.getObject();
        paidToHospitalList.remove(selected);
    }

    
    
    public double createTotalAmountToPay() {
        if (paidToHospitalList == null || paidToHospitalList.isEmpty()) {
            return 0;
        }

        double totalForPay = 0;

        for (OnlineBooking ob : paidToHospitalList) {
            if (!ob.isPaidToHospital()) {
                totalForPay += ob.getAppoinmentTotalAmount();
            }
        }
        paidToHospitalTotal = totalForPay;
        return totalForPay;
    }

    public List<OnlineBooking> getPaidToHospitalList() {
        return paidToHospitalList;
    }

    public void setPaidToHospitalList(List<OnlineBooking> paidToHospitalList) {
        this.paidToHospitalList = paidToHospitalList;
    }

    public boolean isPaidStatus() {
        return paidStatus;
    }

    public void setPaidStatus(boolean paidStatus) {
        this.paidStatus = paidStatus;
    }

    public List<OnlineBooking> getOnlineBookingList() {
        System.out.println("line 202");
        if(onlineBookingList == null){
            onlineBookingList = new ArrayList<>();
        }
        return onlineBookingList;
    }

    public void setOnlineBookingList(List<OnlineBooking> onlineBookingList) {
        this.onlineBookingList = onlineBookingList;
    }

    public List<Institution> getAllAgents() {
        return allAgents;
    }

    public void setAllAgents(List<Institution> allAgents) {
        this.allAgents = allAgents;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.OnlineBookingAgent);
    }

    public void fetchOnlineBookingsForManagement() {
        if (institutionForBookings == null) {
            JsfUtil.addErrorMessage("Please Select Hospital.");
            return;
        }
        
        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("Please Select the Agent.");
            return;
        }
        List<OnlineBooking> bookingList = channelService.fetchOnlineBookings(fromDate, toDate, agentForBookings, institutionForBookings, false, OnlineBookingStatus.COMPLETED);

        if (bookingList != null) {
            onlineBookingList = bookingList;
        }
        
        paidToHospitalList = null;
    }

    public void delete() {

        if (getCurrent() != null && getCurrent().getId() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getInstitutionFacade().edit(getCurrent());
            current = null;
            fillAllAgents();
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            current = null;
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
    }

    @PostConstruct
    public void init() {
        fillAllAgents();
    }

    public boolean checkDuplicateCodeAvailability(Institution institution) {
        fillAllAgents();
        for (Institution ins : allAgents) {
            if (institution.getId() == null) {
                if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                    return true;
                }
            } else if (institution.getId() != null) {
                if (ins.getId() != institution.getId()) {
                    if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void saveSelected(boolean isNew) {
        if (getCurrent().getName() == null || getCurrent().getName().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Name is missing.");
            return;
        }

        if (getCurrent().getCode() == null || getCurrent().getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Code is missing.");
            return;
        }

        if (checkDuplicateCodeAvailability(current)) {
            JsfUtil.addErrorMessage("Agent Code is already taken. Use different one.");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0 && !isNew) {
            getInstitutionFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else if (isNew && getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getInstitutionFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        } else if (isNew && getCurrent().getId() != null) {
            JsfUtil.addErrorMessage("Please use update Button to edit Agent.");
        } else if (!isNew && getCurrent().getId() == null) {
            JsfUtil.addErrorMessage("Please Save the Agent.");
        }
        fillAllAgents();
    }

    public void fillAllAgents() {
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("type", InstitutionType.OnlineBookingAgent);
        allAgents = getInstitutionFacade().findByJpql(j, m);
    }

    public List<Institution> completeAgent(String params) {
        if (params == null) {
            return null;
        }
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " and LOWER(i.name) like  LOWER(CONCAT('%', :params, '%')) or LOWER(i.code) like  LOWER(CONCAT('%', :params, '%'))"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("params", params);
        m.put("type", InstitutionType.OnlineBookingAgent);
        return getInstitutionFacade().findByJpql(j, m);
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.OnlineBookingAgent);
        }
        return current;
    }

    public void setCurrent(Institution current) {
        this.current = current;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitutionForBookings() {
        return institutionForBookings;
    }

    public void setInstitutionForBookings(Institution institutionForBookings) {
        this.institutionForBookings = institutionForBookings;
    }

    public Institution getAgentForBookings() {
        return agentForBookings;
    }

    public void setAgentForBookings(Institution agentForBookings) {
        this.agentForBookings = agentForBookings;
    }

    public ChannelService getChannelService() {
        return channelService;
    }

    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }

}
