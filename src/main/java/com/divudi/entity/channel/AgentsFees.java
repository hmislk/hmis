/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.channel;

import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.ServiceSession;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 *
 * @author safrin
 */
@Entity
public class AgentsFees implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private Institution agent;
    @ManyToOne
    private Fee fee;
    @ManyToOne
    private ServiceSession serviceSession;

    public AgentsFees() {
    }
    
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof AgentsFees)) {
            return false;
        }
        AgentsFees other = (AgentsFees) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.channel.AgentsFees[ id=" + id + " ]";
    }

    public Institution getAgent() {
        return agent;
    }

    public void setAgent(Institution agent) {
        this.agent = agent;
    }

    public Fee getFee() {
        return fee;
    }

    public void setFee(Fee fee) {
        this.fee = fee;
    }

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }
}
