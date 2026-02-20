package com.divudi.service;

import java.text.SimpleDateFormat;
import java.util.Map;

import com.divudi.core.entity.Person;
import javax.ejb.Stateless;

@Stateless
public class PersonService {
    
    // PersonEdit Audit Event Log: personToAuditMap
    public void personToAuditMap(Map<String, Object> map ,Person person) {
        if (person == null || map == null) {
            return;
        }

        map.put("personId", person.getId());
        map.put("name", person.getName());
        map.put("title", person.getTitle());
        map.put("nic", person.getNic());
        map.put("fullName", person.getFullName());
        map.put("nameWithInitials", person.getNameWithInitials());
        map.put("surName", person.getSurName());
        map.put("lastName", person.getLastName());
        map.put("address", person.getAddress());
        map.put("gender", person.getSex());

        if (person.getDob() != null) {
            map.put("dob", new SimpleDateFormat("yyyy-MM-dd").format(person.getDob()));
        } else {
            map.put("dob", null);
        }

        // Contact details
        map.put("phone", person.getPhone());
        map.put("mobile", person.getMobile());
        map.put("email", person.getEmail());
        map.put("fax", person.getFax());
        return;
    }
}

            