package com.divudi.bean.common;

import com.divudi.core.data.dto.DailyReturnDTO;
import com.divudi.core.data.dto.DailyReturnItemDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.service.DailyReturnDtoService;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Controller for DTO-based Daily Return report.
 */
@Named
@SessionScoped
public class DailyReturnDtoController implements Serializable {

    @EJB
    private DailyReturnDtoService dailyReturnDtoService;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private boolean withProfessionalFee;

    private List<DailyReturnDTO> dailyReturnDtos;
    private List<DailyReturnItemDTO> dailyReturnItemDtos;

    public void generateDailyReturnDto() {
        dailyReturnDtos = dailyReturnDtoService.fetchDailyReturnByPaymentMethod(fromDate, toDate);
        dailyReturnItemDtos = dailyReturnDtoService.fetchDailyReturnItems(fromDate, toDate);
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

    public List<DailyReturnDTO> getDailyReturnDtos() {
        return dailyReturnDtos;
    }

    public void setDailyReturnDtos(List<DailyReturnDTO> dailyReturnDtos) {
        this.dailyReturnDtos = dailyReturnDtos;
    }

    public List<DailyReturnItemDTO> getDailyReturnItemDtos() {
        return dailyReturnItemDtos;
    }

    public void setDailyReturnItemDtos(List<DailyReturnItemDTO> dailyReturnItemDtos) {
        this.dailyReturnItemDtos = dailyReturnItemDtos;
    }
}
