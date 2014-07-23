/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.SessionNumberType;
import java.io.Serializable;
import java.nio.channels.SeekableByteChannel;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Service extends Item implements Serializable {

    private static final long serialVersionUID = 1L;

}
