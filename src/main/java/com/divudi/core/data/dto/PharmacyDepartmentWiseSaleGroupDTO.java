package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups the {@link PharmacyDepartmentWiseSaleDTO} bill type rows belonging to
 * one department, and carries that department's subtotal.
 *
 * Assembled in the controller rather than by the query, because JPQL cannot
 * return a nested structure.
 *
 * @author Claude Code
 */
public class PharmacyDepartmentWiseSaleGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department department;
    private List<PharmacyDepartmentWiseSaleDTO> rows = new ArrayList<>();
    private Double departmentTotal = 0.0;

    public PharmacyDepartmentWiseSaleGroupDTO() {
    }

    public PharmacyDepartmentWiseSaleGroupDTO(Department department) {
        this.department = department;
    }

    /**
     * Adds a bill type row and keeps the department subtotal in step with it.
     */
    public void addRow(PharmacyDepartmentWiseSaleDTO row) {
        if (row == null) {
            return;
        }
        rows.add(row);
        if (row.getQuantity() != null) {
            departmentTotal += row.getQuantity();
        }
    }

    /**
     * Name of the department, or a placeholder when the bill carries no
     * department (possible on legacy rows).
     */
    public String getDepartmentName() {
        if (department == null || department.getName() == null) {
            return "Not Specified";
        }
        return department.getName();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<PharmacyDepartmentWiseSaleDTO> getRows() {
        return rows;
    }

    public void setRows(List<PharmacyDepartmentWiseSaleDTO> rows) {
        this.rows = rows;
    }

    public Double getDepartmentTotal() {
        return departmentTotal;
    }

    public void setDepartmentTotal(Double departmentTotal) {
        this.departmentTotal = departmentTotal;
    }

    @Override
    public String toString() {
        return "PharmacyDepartmentWiseSaleGroupDTO{"
                + "department=" + getDepartmentName()
                + ", rows=" + rows.size()
                + ", departmentTotal=" + departmentTotal
                + '}';
    }
}
