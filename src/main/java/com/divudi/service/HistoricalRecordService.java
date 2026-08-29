/*
 * Created by ChatGPT on request by Dr. M. H. B. Ariyaratne
 */
package com.divudi.service;

import com.divudi.core.data.HistoricalRecordType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.HistoricalRecordFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author ASUS
 */
@Stateless
public class HistoricalRecordService {

    @EJB
    private HistoricalRecordFacade historicalRecordFacade;

    public HistoricalRecord findRecord(HistoricalRecordType historicalRecordType, Institution institution, Institution site, Department department, Date recordDate) {
        String jpql = "select hr "
                + " from HistoricalRecord hr "
                + " where hr.retired=false "
                + " and hr.historicalRecordType=:vn "
                + " and hr.recordDate=:rd ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vn", historicalRecordType);
        parameters.put("rd", recordDate);

        if (institution != null) {
            jpql += " and hr.institution=:ins ";
            parameters.put("ins", institution);
        } else {
            jpql += " and hr.institution is null ";
        }

        if (site != null) {
            jpql += " and hr.site=:site ";
            parameters.put("site", site);
        } else {
            jpql += " and hr.site is null ";
        }
        if (department != null) {
            jpql += " and hr.department=:dep ";
            parameters.put("dep", department);
        } else {
            jpql += " and hr.department is null ";
        }
        HistoricalRecord r = historicalRecordFacade.findFirstByJpql(jpql, parameters, TemporalType.DATE);
        return r;
    }

    public List<HistoricalRecord> findRecords(HistoricalRecordType historicalRecordType, Institution institution, Institution site, Department department, Date fromDate, Date toDate) {
        String jpql = "select hr "
                + " from HistoricalRecord hr "
                + " where hr.retired=false ";

        Map<String, Object> parameters = new HashMap<>();

        if (historicalRecordType != null) {
            jpql += " and hr.historicalRecordType=:vn ";
            parameters.put("vn", historicalRecordType);
        }

        if (institution != null) {
            jpql += " and hr.institution=:ins ";
            parameters.put("ins", institution);
        } 

        if (site != null) {
            jpql += " and hr.site=:site ";
            parameters.put("site", site);
        } 

        if (department != null) {
            jpql += " and hr.department=:dep ";
            parameters.put("dep", department);
        } 

        if (fromDate != null) {
            jpql += " and hr.recordDate >= :fd ";
            parameters.put("fd", fromDate);
        }

        if (toDate != null) {
            jpql += " and hr.recordDate <= :td ";
            parameters.put("td", toDate);
        }

        jpql += " order by hr.recordDate desc";

        return historicalRecordFacade.findByJpql(jpql, parameters, TemporalType.DATE);
    }

    public List<HistoricalRecordType> fetchHistoricalRecordTypes() {
        return java.util.Arrays.asList(HistoricalRecordType.values());
    }

    public HistoricalRecord createHistoricalRecord(HistoricalRecordType historicalRecordType,
            Institution institution,
            Institution site,
            Department department) {

        if (historicalRecordType == null) {
            return null;
        }

        HistoricalRecord hr = new HistoricalRecord();
        hr.setHistoricalRecordType(historicalRecordType);
        hr.setInstitution(institution);
        hr.setSite(site);
        hr.setDepartment(department);
        hr.setRecordDate(new Date());
        hr.setRecordDateTime(new Date());
        hr.setRecordValue(0.0);

        historicalRecordFacade.create(hr);
        return hr;
    }

    public HistoricalRecord createHistoricalRecord(HistoricalRecord rec) {
        if (rec == null) {
            return null;
        }
        historicalRecordFacade.create(rec);
        return rec;
    }

}
