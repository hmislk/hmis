package com.divudi.service;


import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.analytics.AggregatedRecord;
import com.divudi.core.facade.AggregatedRecordFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class AggregatedRecordService {

    @EJB
    private AggregatedRecordFacade aggregatedRecordFacade;

    public AggregatedRecord findOrCreateAggregatedRecord(String key, Institution institution, Department department,
                                                         Institution fromInstitution, Institution toInstitution,
                                                         Department fromDepartment, Department toDepartment,
                                                         Staff fromStaff, Staff toStaff,
                                                         WebUser webUser, WebUser fromWebUser, WebUser toWebUser,
                                                         Date fromDate, Date toDate,
                                                         boolean createNewRecord) {
        if (key == null || fromDate == null || toDate == null) {
            return null;
        }

        String jpql = "SELECT a FROM AggregatedRecord a WHERE "
                + "a.key=:key AND a.fromDate=:fromDate AND a.toDate=:toDate "
                + "AND a.retired=false ";

        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " AND a.institution=:institution ";
            params.put("institution", institution);
        }

        if (department != null) {
            jpql += " AND a.department=:department ";
            params.put("department", department);
        }

        if (fromInstitution != null) {
            jpql += " AND a.fromInstitution=:fromInstitution ";
            params.put("fromInstitution", fromInstitution);
        }

        if (toInstitution != null) {
            jpql += " AND a.toInstitution=:toInstitution ";
            params.put("toInstitution", toInstitution);
        }

        if (fromDepartment != null) {
            jpql += " AND a.fromDepartment=:fromDepartment ";
            params.put("fromDepartment", fromDepartment);
        }

        if (toDepartment != null) {
            jpql += " AND a.toDepartment=:toDepartment ";
            params.put("toDepartment", toDepartment);
        }

        if (fromStaff != null) {
            jpql += " AND a.fromStaff=:fromStaff ";
            params.put("fromStaff", fromStaff);
        }

        if (toStaff != null) {
            jpql += " AND a.toStaff=:toStaff ";
            params.put("toStaff", toStaff);
        }

        if (webUser != null) {
            jpql += " AND a.webUser=:webUser ";
            params.put("webUser", webUser);
        }

        if (fromWebUser != null) {
            jpql += " AND a.fromWebUser=:fromWebUser ";
            params.put("fromWebUser", fromWebUser);
        }

        if (toWebUser != null) {
            jpql += " AND a.toWebUser=:toWebUser ";
            params.put("toWebUser", toWebUser);
        }

        AggregatedRecord record = aggregatedRecordFacade.findFirstByJpql(jpql, params, TemporalType.DATE);

        if (record == null && createNewRecord) {
            record = new AggregatedRecord();
            record.setKey(key);
            record.setInstitution(institution);
            record.setDepartment(department);
            record.setFromInstitution(fromInstitution);
            record.setToInstitution(toInstitution);
            record.setFromDepartment(fromDepartment);
            record.setToDepartment(toDepartment);
            record.setFromStaff(fromStaff);
            record.setToStaff(toStaff);
            record.setWebUser(webUser);
            record.setFromWebUser(fromWebUser);
            record.setToWebUser(toWebUser);
            record.setFromDate(fromDate);
            record.setToDate(toDate);
            record.setCreatedAt(new Date());
            aggregatedRecordFacade.create(record);
        }

        return record;
    }
}
