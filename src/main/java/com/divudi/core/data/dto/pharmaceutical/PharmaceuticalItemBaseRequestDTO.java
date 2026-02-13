package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Base request DTO for all pharmaceutical item types (VTM, ATM, VMP, AMP, VMPP, AMPP).
 * Contains shared fields: name, code, descreption, departmentType.
 *
 * @author Buddhika
 */
public class PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String code;
    private String descreption;
    private String departmentType;

    public PharmaceuticalItemBaseRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescreption() {
        return descreption;
    }

    public void setDescreption(String descreption) {
        this.descreption = descreption;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }
}
