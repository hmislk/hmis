/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.Staff;
import java.util.List;

/**
 *
 * @author safrin
 */
public class StaffSession {
    private Staff staff;
    private List<ServiceSession> serviceSession;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<ServiceSession> getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(List<ServiceSession> serviceSession) {
        this.serviceSession = serviceSession;
    }


}
