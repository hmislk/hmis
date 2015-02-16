/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class StaffLeaveSystem extends StaffLeave {

}
