package com.divudi.service;

import com.divudi.core.entity.Family;
import com.divudi.core.entity.FamilyMember;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.FamilyFacade;
import com.divudi.core.facade.FamilyMemberFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class MembershipService {

    @EJB
    FamilyMemberFacade familyMemberFacade;
    @EJB
    FamilyFacade familyFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;

    public void deleteFamilyMember(FamilyMember familyMember, WebUser user) {
        if (familyMember == null || user == null) {
            return;
        }
        familyMember.setRetired(true);
        familyMember.setRetiredAt(new Date());
        familyMember.setRetirer(user);
        familyMemberFacade.edit(familyMember);
        familyMember.getPatient().getPerson().setMembershipScheme(null);
        patientFacade.edit(familyMember.getPatient());
        personFacade.edit(familyMember.getPatient().getPerson());
    }

    public List<FamilyMember> fetchFamilyMembers(Family family) {
        if (family == null) {
            return null;
        }
        String jpql = "select fm from FamilyMember fm where fm.retired = :ret and fm.family = :family";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("family", family);
        return familyMemberFacade.findByJpql(jpql, params);
    }

    public void deleteFamily(Family family, WebUser user) {
        if (family == null || user == null) {
            return;
        }
        family.setRetired(true);
        family.setRetiredAt(new Date());
        family.setRetirer(user);
        familyFacade.edit(family);
    }

    public void deleteFamilyAndMembers(Family family, WebUser user) {
        if (family == null || user == null) {
            return;
        }
        List<FamilyMember> members = fetchFamilyMembers(family);
        if (members != null) {
            for (FamilyMember m : members) {
                deleteFamilyMember(m, user);
            }
        }
        deleteFamily(family, user);
    }
}
