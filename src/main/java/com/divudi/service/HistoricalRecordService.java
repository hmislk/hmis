/*
 * Created by ChatGPT on request by Dr. M. H. B. Ariyaratne
 */
package com.divudi.service;

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

    public HistoricalRecord findRecord(String variableName, Institution institution, Institution site, Department department, Date recordDate) {
        String jpql = "select hr "
                + " from HistoricalRecord hr "
                + " where hr.retired=false "
                + " and hr.variableName=:vn "
                + " and hr.recordDate=:rd ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vn", variableName);
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

    public List<HistoricalRecord> findRecords(String variableName, Institution institution, Institution site, Department department, Date fromDate, Date toDate) {
        String jpql = "select hr "
                + " from HistoricalRecord hr "
                + " where hr.retired=false "
                + " and hr.variableName=:vn ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vn", variableName);

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

    public List<String> fetchVariableNames() {
        String jpql = "select distinct(hr.variableName) "
                + " from HistoricalRecord hr "
                + " where hr.retired=false "
                + " order by hr.variableName";
        return historicalRecordFacade.findStringListByJpql(jpql);
    }

}
