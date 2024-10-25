package com.divudi.entity;

import com.google.gson.Gson;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Column;
import javax.persistence.Transient;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Senula Nanayakkara
 */
@Entity
public class AuditEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Date eventDataTime;
    private Date eventEndTime;
    private Long webUserId;
    @Column(columnDefinition = "CHAR(36)")
    private String uuid; // Changed from UUID to String

    @Lob
    private String url;
    private String eventTrigger;
    private Long institutionId;
    private Long departmentId;

    @Lob
    private String beforeJson;
    @Lob
    private String afterJson;
    private Long eventDuration;
    private String eventStatus;
    private String ipAddress;
    
    private String entityType;
    
    @Transient
    private String difference;

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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AuditEvent)) {
            return false;
        }
        AuditEvent other = (AuditEvent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.AuditEvent[ id=" + id + " ]";
    }
    
    public void calculateDifference() {
        if (beforeJson == null || afterJson == null) {
            this.difference = "";
            return;
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> beforeMap = gson.fromJson(beforeJson, type);
            Map<String, Object> afterMap = gson.fromJson(afterJson, type);
            this.difference = formatDifference(getDifference(beforeMap, afterMap));
        } catch (Exception e) {
            e.printStackTrace();
            this.difference = "";
        }
    }
    
    private Map<String, String> getDifference(Map<String, Object> before, Map<String, Object> after) {
        Map<String, String> diff = new HashMap<>();
        Set<String> allKeys = before.keySet();
        allKeys.addAll(after.keySet());
        for (String key : allKeys) {
            Object beforeValue = before.get(key);
            Object afterValue = after.get(key);
            if ((beforeValue == null && afterValue != null) ||
                (beforeValue != null && !beforeValue.equals(afterValue))) {
                diff.put(key, "Before: " + beforeValue + ", After: " + afterValue);
            }
        }
        return diff;
    }

    private String formatDifference(Map<String, String> diff) {
        StringBuilder sb = new StringBuilder();
        diff.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }

    public String getDifference() {
        if (difference == null) {
            calculateDifference();
        }
        return difference;
    }

    public Date getEventDataTime() {
        return eventDataTime;
    }

    public void setEventDataTime(Date eventDataTime) {
        this.eventDataTime = eventDataTime;
    }

    public Long getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(Long webUserId) {
        this.webUserId = webUserId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEventTrigger() {
        return eventTrigger;
    }

    public void setEventTrigger(String eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    public Long getEventDuration() {
        return eventDuration;
    }

    public void setEventDuration(Long eventDuration) {
        this.eventDuration = eventDuration;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

  

    public Date getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(Date eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    

}
