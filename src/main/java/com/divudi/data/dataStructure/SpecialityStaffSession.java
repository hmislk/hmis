/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Speciality;
import java.util.List;

/**
 *
 * @author safrin
 */
public class SpecialityStaffSession {

    private Speciality speciality;
    private List<StaffSession> staffSessions;

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public List<StaffSession> getStaffSessions() {
        return staffSessions;
    }

    public void setStaffSessions(List<StaffSession> staffSessions) {
        this.staffSessions = staffSessions;
    }
}
