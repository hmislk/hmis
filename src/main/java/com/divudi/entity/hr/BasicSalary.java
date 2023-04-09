/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.PaysheetComponentType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
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
