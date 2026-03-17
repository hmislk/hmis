package com.divudi.core.data;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
public class DepartmentsGroupedIntoInstitutions {

    private Institution institution;
    private List<Department> departments;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

}
