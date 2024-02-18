/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.hr;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.MessageType;

import com.divudi.entity.Area;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.hr.Designation;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffCategory;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.java.CommonFunctions;
import java.util.Date;
import javax.inject.Inject;
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
public class ReportKeyWord {


    CommonFunctions commonFunctions;

    DayType[] dayTypes;
    Staff staff;
    Times times;
    Staff replacingStaff;
    Department department;
    Department departmentFrom;
    StaffCategory staffCategory;
    Designation designation;
    Roster roster;
    PaysheetComponent paysheetComponent;
    SalaryCycle salaryCycle;
    Shift shift;
    Speciality speciality;
    Patient patient;
    Institution institution;
    Institution bank;
    Institution institutionBank;
    PaymentMethod paymentMethod;
    Item item;
    Category category;
    StaffShift staffShift;
    LeaveType leaveType;
    Double from;
    Double to;
    Sex sex;
    EmployeeStatus employeeStatus;
    boolean additionalDetails=false;
    WebUser webUser;
    private String string = "0";
    private String string1 = "0";
    boolean bool1;
    boolean bool2;
    String address;
    AdmissionType admissionType;
    Area area;
    MessageType smsType;
    BillType billType;
    @Transient
    String transAddress1;
    @Transient
    String transAddress2;
    @Transient
    String transAddress3;
    @Transient
    String transAddress4;
    
    int numOfRows=100;

    Date fromDate;
    Date toDate;
    
    MembershipScheme membershipScheme;
    PatientEncounter patientEncounter;

    public PaysheetComponent getPaysheetComponent() {
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public SalaryCycle getSalaryCycle() {
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {
        this.salaryCycle = salaryCycle;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Double getFrom() {
        if (from == null) {
            from = 0.0;
        }
        return from;
    }

    public void setFrom(Double from) {
        this.from = from;
    }

    public Double getTo() {
        if (to == null) {
            to = 0.0;
        }
        return to;
    }

    public void setTo(Double to) {
        this.to = to;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public StaffCategory getStaffCategory() {
        return staffCategory;
    }

    public void setStaffCategory(StaffCategory staffCategory) {
        this.staffCategory = staffCategory;
    }

    public Designation getDesignation() {
        return designation;
    }

    public void setDesignation(Designation designation) {
        this.designation = designation;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Staff getReplacingStaff() {
        return replacingStaff;
    }

    public void setReplacingStaff(Staff replacingStaff) {
        this.replacingStaff = replacingStaff;
    }

    public EmployeeStatus getEmployeeStatus() {
        return employeeStatus;
    }

    public void setEmployeeStatus(EmployeeStatus employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    public Times getTimes() {
        return times;
    }

    public void setTimes(Times times) {
        this.times = times;
    }

    public DayType[] getDayTypes() {
        return dayTypes;
    }

    public void setDayTypes(DayType[] dayTypes) {
        this.dayTypes = dayTypes;
    }

    public Institution getInstitutionBank() {
        return institutionBank;
    }

    public void setInstitutionBank(Institution institutionBank) {
        this.institutionBank = institutionBank;
    }

    public boolean isAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(boolean additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public boolean isBool1() {
        return bool1;
    }

    public void setBool1(boolean bool1) {
        this.bool1 = bool1;
    }

    public String getTransAddress1() {
        if (transAddress1 == null) {
            split();
        }
        return transAddress1;
    }

    public void setTransAddress1(String transAddress1) {
        this.transAddress1 = transAddress1;
    }

    public String getTransAddress2() {
        return transAddress2;
    }

    public void setTransAddress2(String transAddress2) {
        this.transAddress2 = transAddress2;
    }

    public String getTransAddress3() {
        return transAddress3;
    }

    public void setTransAddress3(String transAddress3) {
        this.transAddress3 = transAddress3;
    }

    public String getTransAddress4() {
        return transAddress4;
    }

    public void setTransAddress4(String transAddress4) {
        this.transAddress4 = transAddress4;
    }

    public void split() {
        if (address == null) {
            return;
        }

        String arr[] = address.split(",");
        ////// // System.out.println(arr);
        if (arr == null) {
            return;
        }
        try {
            transAddress1 = arr[0];
            transAddress2 = arr[1];
            transAddress3 = arr[2];
            transAddress4 = arr[3];
        } catch (Exception e) {
            ////// // System.out.println(e.getMessage());
        }

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void resetKeyWord() {
        dayTypes = null;
        staff = null;
        times = null;
        replacingStaff = null;
        department = null;
        staffCategory = null;
        designation = null;
        roster = null;
        paysheetComponent = null;
        salaryCycle = null;
        shift = null;
        speciality = null;
        patient = null;
        institution = null;
        bank = null;
        institutionBank = null;
        paymentMethod = null;
        item = null;
        staffShift = null;
        leaveType = null;
        from = null;
        to = null;
        sex = null;
        employeeStatus = null;
        additionalDetails = false;
        webUser = null;
        string = "0";
        string1 = "0";
        bool1 = false;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public boolean isBool2() {
        return bool2;
    }

    public void setBool2(boolean bool2) {
        this.bool2 = bool2;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public MessageType getSmsType() {
        return smsType;
    }

    public void setSmsType(MessageType smsType) {
        this.smsType = smsType;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfMonth();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public int getNumOfRows() {
        return numOfRows;
    }

    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Department getDepartmentFrom() {
        return departmentFrom;
    }

    public void setDepartmentFrom(Department departmentFrom) {
        this.departmentFrom = departmentFrom;
    }

}
