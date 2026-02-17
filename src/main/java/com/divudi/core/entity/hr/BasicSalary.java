/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.hr;

import com.divudi.core.data.hr.PaysheetComponentType;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author Buddhika
 */
@Entity

public class BasicSalary extends PaysheetComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public PaysheetComponentType getComponentType() {
        return PaysheetComponentType.BasicSalary;
    }

    @Override
    public void setComponentType(PaysheetComponentType componentType) {
        this.componentType = PaysheetComponentType.BasicSalary;
    }
}
