package com.divudi.service;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.divudi.bean.common.AuditEventController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.PersonFacade;
import java.text.SimpleDateFormat;
import org.json.JSONObject;

@Stateless
public class PersonService {
    
    @EJB
    private PersonFacade personFacade;
    @EJB
    private AuditService auditService;

    @Inject
    private SessionController sessionController;

    public void editPerson(Person original, Person current) {
        getPersonFacade().edit(current);
        
        String BEFOREJSON = personToJsonForEditAuditLog(original);
        String AFTERJSON = personToJsonForEditAuditLog(current);
        
        getAuditService().logAudit(BEFOREJSON, AFTERJSON, sessionController.getLoggedUser(), "Person", "updatePerson", current.getId());   
    } 
    
    public void editAndFlushPerson(Person original, Person current) {
        getPersonFacade().editAndFlush(current);
        
        String BEFOREJSON = personToJsonForEditAuditLog(original);
        String AFTERJSON = personToJsonForEditAuditLog(current);
        
        getAuditService().logAudit(BEFOREJSON, AFTERJSON, sessionController.getLoggedUser(), "Person", "updatePerson", current.getId()); 
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }
    
    public String personToJsonForEditAuditLog(Person p) {

        if (p == null) {
            return "{}";
        }

        JSONObject json = new JSONObject();

        json.put("personId", p.getId());
        json.put("name", p.getName());
        json.put("title", p.getTitle());
        json.put("surname", p.getSurName());

        json.put("dob",
            p.getDob() == null ? null :
            new SimpleDateFormat("yyyy-MM-dd").format(p.getDob())
        );

        json.put("sex", p.getSex());
        json.put("nic", p.getNic());
        json.put("address", p.getAddress());
        json.put("mobile", p.getMobile());
        json.put("email", p.getEmail());
        json.put("phone", p.getPhone());
        json.put("initials", p.getInitials());
        json.put("fullName", p.getFullName());
        json.put("lastName", p.getLastName());

        json.put("area",
            p.getArea() == null ? null : p.getArea().getId()
        );

        return json.toString();
    }


}
