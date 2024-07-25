package com.divudi.data;

import com.divudi.entity.Department;
import com.divudi.entity.Staff;

public class TokenCount {
    private Department department;
    private Long tokenCount;
    private Staff staff;

    public TokenCount() {
    }

    public TokenCount(Department department, Staff staff, Long tokenCount) {
        this.department = department;
        this.tokenCount = tokenCount;
        this.staff = staff;
    }

    
    
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Long getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Long tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }
    
    
    
    
}
