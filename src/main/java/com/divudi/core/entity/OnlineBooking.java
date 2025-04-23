package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author Chinthaka Prasad
 */
@Entity
public class OnlineBooking implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof OnlineBooking)) {
            return false;
        }
        OnlineBooking ob = (OnlineBooking) object;

        return id != null && id.equals(ob.id);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.OnlineBooking[ id=" + id + " ]";
    }

}
