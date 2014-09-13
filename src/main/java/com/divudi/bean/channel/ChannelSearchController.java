/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.entity.BillSession;
import com.divudi.facade.BillSessionFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ChannelSearchController implements Serializable {

    private Date date;
    private List<BillSession> billSessions;
    private List<BillSession> filteredbillSessions;  
    ///////////
    @EJB
    private BillSessionFacade billSessionFacade;
    ///////////
    @Inject
    private BookingController bookingController;
    @Inject
    private ChannelBillController channelBillController;

    /**
     * Creates a new instance of ChannelSearchController
     */
    public ChannelSearchController() {
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        billSessions = null;
        filteredbillSessions = null;
    }

    public List<BillSession> getBillSessions() {

        if (billSessions == null) {
            if (getDate() != null) {
                String sql = "Select bs From BillSession bs "
                        + " where bs.retired=false "
                        + " and bs.sessionDate= :ssDate "
                        + " order by  bs.serviceSession.staff.speciality.name,"
                        + " bs.serviceSession.staff.person.name,"
                        + " bs.serviceSession.id,"
                        + " bs.serialNo";
                HashMap hh = new HashMap();
                hh.put("ssDate", getDate());
                billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

            }
        }


        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public List<BillSession> getFilteredbillSessions() {
        return filteredbillSessions;
    }

    public void setFilteredbillSessions(List<BillSession> filteredbillSessions) {
        this.filteredbillSessions = filteredbillSessions;
    }

   
    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public BookingController getBookingController() {
        return bookingController;
    }

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public void setChannelBillController(ChannelBillController channelBillController) {
        this.channelBillController = channelBillController;
    }
}
