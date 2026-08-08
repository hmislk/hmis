/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.entity.UserFavoriteReport;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.UserFavoriteReportFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Self-service "favorite reports" (star icon) for the Reports index page.
 * Each user marks their own favorites; there is no admin assignment.
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class UserFavoriteReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @EJB
    private UserFavoriteReportFacade ejbFacade;

    /**
     * Session-cached, non-retired favorites for the current logged-in user,
     * ordered by orderNumber. Lazily loaded on first access; reloaded after
     * every toggleFavorite call.
     */
    private List<UserFavoriteReport> favorites;

    public UserFavoriteReportController() {
    }

    /**
     * True if the current logged-in user has a non-retired favorite with
     * this reportKey. Backed by a session-cached list (loaded once per
     * session, not re-queried per call) since this is invoked once per
     * report button on the Reports index page (~242 times per render).
     */
    public boolean isFavorite(String reportKey) {
        if (sessionController == null || sessionController.getLoggedUser() == null) {
            return false;
        }
        if (reportKey == null) {
            return false;
        }
        for (UserFavoriteReport f : getCachedFavorites()) {
            if (reportKey.equals(f.getReportKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles the favorite state of a report for the current logged-in
     * user: retires the existing favorite (soft-delete) if one exists,
     * otherwise creates a new one. Refreshes the cached list afterwards so
     * subsequent isFavorite calls on the same request/AJAX round-trip
     * reflect the change.
     */
    public void toggleFavorite(String reportKey, String reportLabel, String category) {
        WebUser u = sessionController == null ? null : sessionController.getLoggedUser();
        if (u == null || reportKey == null) {
            return;
        }
        UserFavoriteReport existing = null;
        for (UserFavoriteReport f : getCachedFavorites()) {
            if (reportKey.equals(f.getReportKey())) {
                existing = f;
                break;
            }
        }
        if (existing != null) {
            existing.setRetired(true);
            save(existing);
        } else {
            UserFavoriteReport nf = new UserFavoriteReport();
            nf.setWebUser(u);
            nf.setReportKey(reportKey);
            nf.setReportLabel(reportLabel);
            nf.setCategory(category);
            nf.setOrderNumber(getCachedFavorites().size() + 1);
            nf.setRetired(false);
            save(nf);
        }
        loadFavorites();
    }

    /**
     * The current logged-in user's non-retired favorites, ordered by
     * orderNumber, for the Favorites tab to iterate with ui:repeat.
     */
    public List<UserFavoriteReport> getFavorites() {
        if (sessionController == null || sessionController.getLoggedUser() == null) {
            return new ArrayList<>();
        }
        return getCachedFavorites();
    }

    private List<UserFavoriteReport> getCachedFavorites() {
        if (favorites == null) {
            loadFavorites();
        }
        return favorites;
    }

    private void loadFavorites() {
        WebUser u = sessionController == null ? null : sessionController.getLoggedUser();
        if (u == null) {
            favorites = new ArrayList<>();
            return;
        }
        String jpql = "select f from UserFavoriteReport f where f.webUser=:u and f.retired=:ret order by f.orderNumber";
        Map<String, Object> m = new HashMap<>();
        m.put("u", u);
        m.put("ret", false);
        List<UserFavoriteReport> result = getFacade().findByJpql(jpql, m);
        favorites = result == null ? new ArrayList<>() : result;
    }

    private void save(UserFavoriteReport f) {
        if (f == null) {
            return;
        }
        if (f.getId() != null) {
            getFacade().edit(f);
        } else {
            getFacade().create(f);
        }
    }

    private UserFavoriteReportFacade getFacade() {
        return ejbFacade;
    }

}
