package com.divudi.bean.common;

import com.divudi.core.data.dto.UserPasswordResetDto;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.LoginsFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserPasswordHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Password Reset Report: when each web user's password was last updated and
 * whether the user changed it themselves or an administrator reset it.
 *
 * @author Claude Code
 */
@Named
@SessionScoped
public class PasswordResetReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A password change sets WebUser.lastPasswordResetAt and then writes a
     * WebUserPasswordHistory row a moment later. A history row further than
     * this from lastPasswordResetAt belongs to an earlier change, so the
     * latest change was made by a path that writes no history (Unknown).
     */
    private static final long HISTORY_MATCH_TOLERANCE_MS = 60_000L;

    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private WebUserPasswordHistoryFacade webUserPasswordHistoryFacade;
    @EJB
    private LoginsFacade loginsFacade;
    @Inject
    private SessionController sessionController;

    private String nameFilter;
    private String updatedByFilter;
    private boolean includeRetired;
    private List<UserPasswordResetDto> rows;
    private Date processedAt;
    private String processedBy;

    private long selfCount;
    private long adminCount;
    private long unknownCount;
    private long neverCount;

    public String navigateToPasswordResetReport() {
        fillReport();
        return "/admin/users/user_password_reset_report?faces-redirect=true";
    }

    public void fillReport() {
        processedAt = new Date();
        WebUser loggedUser = sessionController.getLoggedUser();
        if (loggedUser == null) {
            processedBy = null;
        } else if (loggedUser.getWebUserPerson() != null && loggedUser.getWebUserPerson().getName() != null) {
            processedBy = loggedUser.getWebUserPerson().getName();
        } else {
            processedBy = loggedUser.getName();
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.dto.UserPasswordResetDto("
                + "wu.id, wu.name, wu.code, sp.title, sp.name, wp.title, wp.name, "
                + "wu.lastPasswordResetAt, wu.retired) "
                + "FROM WebUser wu "
                + "LEFT JOIN wu.staff s "
                + "LEFT JOIN s.person sp "
                + "LEFT JOIN wu.webUserPerson wp "
                + "WHERE 1=1 ";
        if (!includeRetired) {
            jpql += "AND wu.retired = :ret ";
            params.put("ret", false);
        }
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql += "AND (LOWER(wu.name) LIKE :nm OR LOWER(sp.name) LIKE :nm OR LOWER(wp.name) LIKE :nm) ";
            params.put("nm", "%" + nameFilter.trim().toLowerCase() + "%");
        }
        jpql += "ORDER BY wu.lastPasswordResetAt DESC, wu.name";

        @SuppressWarnings("unchecked")
        List<UserPasswordResetDto> users = (List<UserPasswordResetDto>) webUserFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP, true);

        Map<Long, Object[]> latestHistoryByUser = findLatestPasswordHistoryByUser();
        Map<Long, Date> lastLoginByUser = findLastLoginByUser();

        selfCount = 0;
        adminCount = 0;
        unknownCount = 0;
        neverCount = 0;
        rows = new ArrayList<>();
        if (users == null) {
            return;
        }
        for (UserPasswordResetDto u : users) {
            classify(u, latestHistoryByUser.get(u.getWebUserId()));
            u.setLastLoginAt(lastLoginByUser.get(u.getWebUserId()));
            switch (u.getUpdatedBy()) {
                case UserPasswordResetDto.UPDATED_BY_SELF:
                    selfCount++;
                    break;
                case UserPasswordResetDto.UPDATED_BY_ADMIN:
                    adminCount++;
                    break;
                case UserPasswordResetDto.UPDATED_BY_NEVER:
                    neverCount++;
                    break;
                default:
                    unknownCount++;
            }
            if (updatedByFilter == null || updatedByFilter.isEmpty() || updatedByFilter.equals(u.getUpdatedBy())) {
                rows.add(u);
            }
        }
    }

    /**
     * Last login time of every user who has logged in, keyed by web user id.
     */
    private Map<Long, Date> findLastLoginByUser() {
        String jpql = "SELECT l.webUser.id, MAX(l.logedAt) "
                + "FROM Logins l "
                + "GROUP BY l.webUser.id";
        List<Object[]> results = loginsFacade.findObjectArrayByJpql(jpql, new HashMap<>(), TemporalType.TIMESTAMP);
        Map<Long, Date> map = new HashMap<>();
        if (results != null) {
            for (Object[] r : results) {
                map.put((Long) r[0], (Date) r[1]);
            }
        }
        return map;
    }

    /**
     * Latest password history row of every user, keyed by web user id. Each
     * value is {webUserId, createdAt, createrId}.
     */
    private Map<Long, Object[]> findLatestPasswordHistoryByUser() {
        String jpql = "SELECT h.webUser.id, h.createdAt, c.id "
                + "FROM WebUserPasswordHistory h "
                + "LEFT JOIN h.creater c "
                + "WHERE h.createdAt = ("
                + "SELECT MAX(h2.createdAt) FROM WebUserPasswordHistory h2 WHERE h2.webUser = h.webUser)";
        List<Object[]> results = webUserPasswordHistoryFacade.findObjectArrayByJpql(jpql, new HashMap<>(), TemporalType.TIMESTAMP);
        Map<Long, Object[]> map = new HashMap<>();
        if (results != null) {
            for (Object[] r : results) {
                map.put((Long) r[0], r);
            }
        }
        return map;
    }

    private void classify(UserPasswordResetDto u, Object[] history) {
        Date resetAt = u.getLastPasswordResetAt();
        if (resetAt == null) {
            u.setUpdatedBy(UserPasswordResetDto.UPDATED_BY_NEVER);
            return;
        }
        if (history == null || history[1] == null || history[2] == null
                || Math.abs(((Date) history[1]).getTime() - resetAt.getTime()) > HISTORY_MATCH_TOLERANCE_MS) {
            u.setUpdatedBy(UserPasswordResetDto.UPDATED_BY_UNKNOWN);
            return;
        }
        Long createrId = (Long) history[2];
        if (createrId.equals(u.getWebUserId())) {
            u.setUpdatedBy(UserPasswordResetDto.UPDATED_BY_SELF);
            return;
        }
        u.setUpdatedBy(UserPasswordResetDto.UPDATED_BY_ADMIN);
    }

    public void resetFilters() {
        nameFilter = null;
        updatedByFilter = null;
        includeRetired = false;
        fillReport();
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getUpdatedByFilter() {
        return updatedByFilter;
    }

    public void setUpdatedByFilter(String updatedByFilter) {
        this.updatedByFilter = updatedByFilter;
    }

    public boolean isIncludeRetired() {
        return includeRetired;
    }

    public void setIncludeRetired(boolean includeRetired) {
        this.includeRetired = includeRetired;
    }

    public List<UserPasswordResetDto> getRows() {
        return rows;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public long getSelfCount() {
        return selfCount;
    }

    public long getAdminCount() {
        return adminCount;
    }

    public long getUnknownCount() {
        return unknownCount;
    }

    public long getNeverCount() {
        return neverCount;
    }

    public long getTotalCount() {
        return selfCount + adminCount + unknownCount + neverCount;
    }
}
