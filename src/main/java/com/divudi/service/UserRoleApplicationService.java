/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import com.divudi.core.data.Icon;
import com.divudi.core.data.LoginPage;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.TriggerType;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.TriggerSubscription;
import com.divudi.core.entity.UserIcon;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserDefaultLoginPage;
import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.entity.WebUserRole;
import com.divudi.core.entity.WebUserRoleUser;
import com.divudi.core.facade.TriggerSubscriptionFacade;
import com.divudi.core.facade.UserIconFacade;
import com.divudi.core.facade.WebUserDefaultLoginPageFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.core.facade.WebUserRolePrivilegeFacade;
import com.divudi.core.facade.WebUserRoleUserFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Single engine for role-template based user-management operations (used by
 * the admin UI, the REST API, and the AI assistant). Roles are admin-time
 * templates only; runtime behaviour reads user-level records exclusively.
 * This service stamps/resets those user-level records from a role template.
 *
 * JPQL only, batched writes, one AuditEvent + one WebUserRoleUser history
 * row per {@link #apply} call.
 *
 * @author www.divudi.com
 */
@Stateless
public class UserRoleApplicationService {

    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    private WebUserRolePrivilegeFacade webUserRolePrivilegeFacade;
    @EJB
    private UserIconFacade userIconFacade;
    @EJB
    private TriggerSubscriptionFacade triggerSubscriptionFacade;
    @EJB
    private WebUserDefaultLoginPageFacade webUserDefaultLoginPageFacade;
    @EJB
    private WebUserRoleUserFacade webUserRoleUserFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private AuditEventService auditEventService;

    public enum RoleAspect {
        PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE
    }

    public enum RoleOperation {
        RESET, EXPAND, NARROW
    }

    /**
     * Result of applying one role operation to one user. Counts are keyed by
     * aspect so the UI/AI can report "added N privileges, retired M icons"
     * style summaries.
     */
    public static class RoleApplicationResult implements Serializable {

        private static final long serialVersionUID = 1L;
        private final WebUser user;
        private final Map<RoleAspect, Integer> added = new EnumMap<>(RoleAspect.class);
        private final Map<RoleAspect, Integer> retired = new EnumMap<>(RoleAspect.class);
        private String errorMessage;

        public RoleApplicationResult(WebUser user) {
            this.user = user;
        }

        void addCounts(RoleAspect aspect, int addedCount, int retiredCount) {
            added.merge(aspect, addedCount, Integer::sum);
            retired.merge(aspect, retiredCount, Integer::sum);
        }

        public WebUser getUser() {
            return user;
        }

        public Map<RoleAspect, Integer> getAdded() {
            return added;
        }

        public Map<RoleAspect, Integer> getRetired() {
            return retired;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }

        public int getTotalAdded() {
            int total = 0;
            for (int v : added.values()) {
                total += v;
            }
            return total;
        }

        public int getTotalRetired() {
            int total = 0;
            for (int v : retired.values()) {
                total += v;
            }
            return total;
        }

        public String summary() {
            if (!isSuccess()) {
                return "error=" + errorMessage;
            }
            return "added=" + added + ", retired=" + retired;
        }

        public static String summarize(List<RoleApplicationResult> results) {
            if (results == null || results.isEmpty()) {
                return "no users processed";
            }
            int succeeded = 0;
            int failed = 0;
            int totalAdded = 0;
            int totalRetired = 0;
            for (RoleApplicationResult r : results) {
                if (r.isSuccess()) {
                    succeeded++;
                    totalAdded += r.getTotalAdded();
                    totalRetired += r.getTotalRetired();
                } else {
                    failed++;
                }
            }
            return "users=" + results.size() + ", succeeded=" + succeeded + ", failed=" + failed
                    + ", totalAdded=" + totalAdded + ", totalRetired=" + totalRetired;
        }
    }

    /**
     * Applies a role operation to one user across the given departments and
     * aspects. RESET converges the user's active set to exactly the role
     * template's set (records already matching the template are left
     * untouched rather than being blindly retired-and-recreated, so audit
     * counts reflect real changes only).
     */
    public RoleApplicationResult apply(RoleOperation op, WebUser targetUser, WebUserRole role, List<Department> departments,
            Set<RoleAspect> aspects, boolean updateUserRole, WebUser actor) {

        RoleApplicationResult result = new RoleApplicationResult(targetUser);

        if (op == null) {
            result.setErrorMessage("Operation is required");
            return result;
        }
        if (targetUser == null) {
            result.setErrorMessage("Target user is required");
            return result;
        }
        if (departments == null || departments.isEmpty()) {
            result.setErrorMessage("At least one department is required");
            return result;
        }
        if (aspects == null || aspects.isEmpty()) {
            result.setErrorMessage("At least one aspect is required");
            return result;
        }

        WebUserRole effectiveRole = role != null ? role : targetUser.getRole();
        if (effectiveRole == null) {
            result.setErrorMessage("No role available: role not specified and user has no default role");
            return result;
        }

        Date now = new Date();

        List<WebUserPrivilege> privilegesToCreate = new ArrayList<>();
        List<WebUserPrivilege> privilegesToEdit = new ArrayList<>();
        List<UserIcon> iconsToCreate = new ArrayList<>();
        List<UserIcon> iconsToEdit = new ArrayList<>();
        List<TriggerSubscription> subscriptionsToCreate = new ArrayList<>();
        List<TriggerSubscription> subscriptionsToEdit = new ArrayList<>();
        List<WebUserDefaultLoginPage> loginPagesToCreate = new ArrayList<>();
        List<WebUserDefaultLoginPage> loginPagesToEdit = new ArrayList<>();
        List<WebUserRoleUser> historyRows = new ArrayList<>();

        for (Department dept : departments) {
            if (dept == null) {
                continue;
            }
            if (aspects.contains(RoleAspect.PRIVILEGES)) {
                applyPrivilegesAspect(op, targetUser, effectiveRole, dept, actor, now, result, privilegesToCreate, privilegesToEdit);
            }
            if (aspects.contains(RoleAspect.ICONS)) {
                applyIconsAspect(op, targetUser, effectiveRole, dept, now, result, iconsToCreate, iconsToEdit);
            }
            if (aspects.contains(RoleAspect.SUBSCRIPTIONS)) {
                applySubscriptionsAspect(op, targetUser, effectiveRole, dept, actor, now, result, subscriptionsToCreate, subscriptionsToEdit);
            }
            if (aspects.contains(RoleAspect.LOGIN_PAGE)) {
                applyLoginPageAspect(op, targetUser, effectiveRole, dept, actor, now, result, loginPagesToCreate, loginPagesToEdit);
            }
            if (op == RoleOperation.RESET || op == RoleOperation.EXPAND) {
                WebUserRoleUser history = new WebUserRoleUser();
                history.setWebUserRole(effectiveRole);
                history.setWebUser(targetUser);
                history.setDepartment(dept);
                history.setCreater(actor);
                history.setCreatedAt(now);
                historyRows.add(history);
            }
        }

        if (!privilegesToEdit.isEmpty()) {
            webUserPrivilegeFacade.batchEdit(privilegesToEdit);
        }
        if (!privilegesToCreate.isEmpty()) {
            webUserPrivilegeFacade.batchCreate(privilegesToCreate);
        }
        if (!iconsToEdit.isEmpty()) {
            userIconFacade.batchEdit(iconsToEdit);
        }
        if (!iconsToCreate.isEmpty()) {
            userIconFacade.batchCreate(iconsToCreate);
        }
        if (!subscriptionsToEdit.isEmpty()) {
            triggerSubscriptionFacade.batchEdit(subscriptionsToEdit);
        }
        if (!subscriptionsToCreate.isEmpty()) {
            triggerSubscriptionFacade.batchCreate(subscriptionsToCreate);
        }
        if (!loginPagesToEdit.isEmpty()) {
            webUserDefaultLoginPageFacade.batchEdit(loginPagesToEdit);
        }
        if (!loginPagesToCreate.isEmpty()) {
            webUserDefaultLoginPageFacade.batchCreate(loginPagesToCreate);
        }
        if (!historyRows.isEmpty()) {
            webUserRoleUserFacade.batchCreate(historyRows);
        }

        if (op == RoleOperation.RESET && updateUserRole && !effectiveRole.equals(targetUser.getRole())) {
            targetUser.setRole(effectiveRole);
            webUserFacade.edit(targetUser);
        }

        writeAuditEvent(op, targetUser, effectiveRole, departments, aspects, result, actor);

        return result;
    }

    /**
     * Loops {@link #apply} over many users. When {@code role} is null each
     * user's own current role is used; a user with no role and no role
     * specified gets an error result instead of being skipped silently.
     */
    public List<RoleApplicationResult> applyBulk(RoleOperation op, List<WebUser> targetUsers, WebUserRole role,
            List<Department> departments, Set<RoleAspect> aspects, boolean updateUserRole, WebUser actor) {
        List<RoleApplicationResult> results = new ArrayList<>();
        if (targetUsers == null) {
            return results;
        }
        for (WebUser user : targetUsers) {
            if (user == null) {
                continue;
            }
            WebUserRole effectiveRole = role != null ? role : user.getRole();
            if (effectiveRole == null) {
                RoleApplicationResult r = new RoleApplicationResult(user);
                r.setErrorMessage("User has no default role and no role was specified");
                results.add(r);
                continue;
            }
            results.add(apply(op, user, effectiveRole, departments, aspects, updateUserRole, actor));
        }
        return results;
    }

    /**
     * Read-only preview of how many records each aspect would add+retire,
     * without writing anything. Used by UI confirm dialogs and the AI
     * assistant's confirm flow.
     */
    public Map<RoleAspect, Long> previewCounts(RoleOperation op, WebUser targetUser, WebUserRole role,
            List<Department> departments, Set<RoleAspect> aspects) {
        Map<RoleAspect, Long> preview = new EnumMap<>(RoleAspect.class);
        if (op == null || targetUser == null || departments == null || departments.isEmpty()
                || aspects == null || aspects.isEmpty()) {
            return preview;
        }
        WebUserRole effectiveRole = role != null ? role : targetUser.getRole();
        if (effectiveRole == null) {
            return preview;
        }
        for (RoleAspect aspect : aspects) {
            long total = 0;
            for (Department dept : departments) {
                if (dept == null) {
                    continue;
                }
                total += previewAspectCount(op, targetUser, effectiveRole, dept, aspect);
            }
            preview.put(aspect, total);
        }
        return preview;
    }

    // <editor-fold desc="Privileges aspect">
    private void applyPrivilegesAspect(RoleOperation op, WebUser user, WebUserRole role, Department dept, WebUser actor, Date now,
            RoleApplicationResult result, List<WebUserPrivilege> toCreate, List<WebUserPrivilege> toEdit) {
        Set<Privileges> templateSet = fetchTemplatePrivileges(role);
        List<WebUserPrivilege> active = fetchActivePrivileges(user, dept);
        Set<Privileges> activeValues = new HashSet<>();
        for (WebUserPrivilege w : active) {
            if (w.getPrivilege() != null) {
                activeValues.add(w.getPrivilege());
            }
        }

        int added = 0;
        int retired = 0;

        if (op == RoleOperation.RESET || op == RoleOperation.NARROW) {
            for (WebUserPrivilege w : active) {
                boolean matchesTemplate = w.getPrivilege() != null && templateSet.contains(w.getPrivilege());
                boolean shouldRetire = op == RoleOperation.NARROW ? matchesTemplate : !matchesTemplate;
                if (shouldRetire) {
                    w.setRetired(true);
                    w.setRetirer(actor);
                    w.setRetiredAt(now);
                    toEdit.add(w);
                    retired++;
                }
            }
        }

        if (op == RoleOperation.RESET || op == RoleOperation.EXPAND) {
            Set<Privileges> missing = new HashSet<>();
            for (Privileges p : templateSet) {
                if (!activeValues.contains(p)) {
                    missing.add(p);
                }
            }
            Map<Privileges, WebUserPrivilege> retiredCandidates = fetchRetiredPrivilegeCandidates(user, dept, missing);
            for (Privileges p : missing) {
                WebUserPrivilege candidate = retiredCandidates.get(p);
                if (candidate != null) {
                    candidate.setRetired(false);
                    toEdit.add(candidate);
                } else {
                    WebUserPrivilege fresh = new WebUserPrivilege();
                    fresh.setWebUser(user);
                    fresh.setDepartment(dept);
                    fresh.setPrivilege(p);
                    fresh.setCreater(actor);
                    fresh.setCreatedAt(now);
                    toCreate.add(fresh);
                }
                added++;
            }
        }

        result.addCounts(RoleAspect.PRIVILEGES, added, retired);
    }

    private Set<Privileges> fetchTemplatePrivileges(WebUserRole role) {
        List<?> rows = webUserRolePrivilegeFacade.findLightsByJpql(
                "select distinct p.privilege from WebUserRolePrivilege p where p.webUserRole=:role and p.retired=false and p.privilege is not null",
                params("role", role));
        Set<Privileges> set = new HashSet<>();
        for (Object o : rows) {
            set.add((Privileges) o);
        }
        return set;
    }

    private List<WebUserPrivilege> fetchActivePrivileges(WebUser user, Department dept) {
        return webUserPrivilegeFacade.findByJpql(
                "select w from WebUserPrivilege w where w.webUser=:user and w.department=:dept and w.retired=false",
                params("user", user, "dept", dept));
    }

    private Map<Privileges, WebUserPrivilege> fetchRetiredPrivilegeCandidates(WebUser user, Department dept, Set<Privileges> values) {
        Map<Privileges, WebUserPrivilege> map = new HashMap<>();
        if (values.isEmpty()) {
            return map;
        }
        List<WebUserPrivilege> rows = webUserPrivilegeFacade.findByJpql(
                "select w from WebUserPrivilege w where w.webUser=:user and w.department=:dept and w.retired=true and w.privilege in :vals order by w.id desc",
                params("user", user, "dept", dept, "vals", values));
        for (WebUserPrivilege w : rows) {
            map.putIfAbsent(w.getPrivilege(), w);
        }
        return map;
    }
    // </editor-fold>

    // <editor-fold desc="Icons aspect">
    private void applyIconsAspect(RoleOperation op, WebUser user, WebUserRole role, Department dept, Date now,
            RoleApplicationResult result, List<UserIcon> toCreate, List<UserIcon> toEdit) {
        Map<Icon, UserIcon> template = fetchTemplateIcons(role);
        Set<Icon> templateSet = template.keySet();
        List<UserIcon> active = fetchActiveIcons(user, dept);
        Set<Icon> activeValues = new HashSet<>();
        for (UserIcon u : active) {
            if (u.getIcon() != null) {
                activeValues.add(u.getIcon());
            }
        }

        int added = 0;
        int retired = 0;

        if (op == RoleOperation.RESET || op == RoleOperation.NARROW) {
            for (UserIcon u : active) {
                boolean matchesTemplate = u.getIcon() != null && templateSet.contains(u.getIcon());
                boolean shouldRetire = op == RoleOperation.NARROW ? matchesTemplate : !matchesTemplate;
                if (shouldRetire) {
                    u.setRetired(true);
                    toEdit.add(u);
                    retired++;
                }
            }
        }

        if (op == RoleOperation.RESET || op == RoleOperation.EXPAND) {
            Set<Icon> missing = new HashSet<>();
            for (Icon icon : templateSet) {
                if (!activeValues.contains(icon)) {
                    missing.add(icon);
                }
            }
            Map<Icon, UserIcon> retiredCandidates = fetchRetiredIconCandidates(user, dept, missing);
            for (Icon icon : missing) {
                UserIcon candidate = retiredCandidates.get(icon);
                if (candidate != null) {
                    candidate.setRetired(false);
                    toEdit.add(candidate);
                } else {
                    UserIcon fresh = new UserIcon();
                    fresh.setWebUser(user);
                    fresh.setDepartment(dept);
                    fresh.setIcon(icon);
                    UserIcon templateRow = template.get(icon);
                    fresh.setOrderNumber(templateRow != null ? templateRow.getOrderNumber() : 0d);
                    toCreate.add(fresh);
                }
                added++;
            }
        }

        result.addCounts(RoleAspect.ICONS, added, retired);
    }

    private Map<Icon, UserIcon> fetchTemplateIcons(WebUserRole role) {
        List<UserIcon> rows = userIconFacade.findByJpql(
                "select u from UserIcon u where u.webUserRole=:role and u.webUser is null and u.retired=false and u.icon is not null",
                params("role", role));
        Map<Icon, UserIcon> map = new LinkedHashMap<>();
        for (UserIcon u : rows) {
            map.put(u.getIcon(), u);
        }
        return map;
    }

    private List<UserIcon> fetchActiveIcons(WebUser user, Department dept) {
        return userIconFacade.findByJpql(
                "select u from UserIcon u where u.webUser=:user and u.department=:dept and u.retired=false",
                params("user", user, "dept", dept));
    }

    private Map<Icon, UserIcon> fetchRetiredIconCandidates(WebUser user, Department dept, Set<Icon> values) {
        Map<Icon, UserIcon> map = new HashMap<>();
        if (values.isEmpty()) {
            return map;
        }
        List<UserIcon> rows = userIconFacade.findByJpql(
                "select u from UserIcon u where u.webUser=:user and u.department=:dept and u.retired=true and u.icon in :vals order by u.id desc",
                params("user", user, "dept", dept, "vals", values));
        for (UserIcon u : rows) {
            map.putIfAbsent(u.getIcon(), u);
        }
        return map;
    }
    // </editor-fold>

    // <editor-fold desc="Subscriptions aspect">
    private void applySubscriptionsAspect(RoleOperation op, WebUser user, WebUserRole role, Department dept, WebUser actor, Date now,
            RoleApplicationResult result, List<TriggerSubscription> toCreate, List<TriggerSubscription> toEdit) {
        Map<TriggerType, TriggerSubscription> template = fetchTemplateSubscriptions(role);
        Set<TriggerType> templateSet = template.keySet();
        List<TriggerSubscription> active = fetchActiveSubscriptions(user, dept);
        Set<TriggerType> activeValues = new HashSet<>();
        for (TriggerSubscription t : active) {
            if (t.getTriggerType() != null) {
                activeValues.add(t.getTriggerType());
            }
        }

        int added = 0;
        int retired = 0;

        if (op == RoleOperation.RESET || op == RoleOperation.NARROW) {
            for (TriggerSubscription t : active) {
                boolean matchesTemplate = t.getTriggerType() != null && templateSet.contains(t.getTriggerType());
                boolean shouldRetire = op == RoleOperation.NARROW ? matchesTemplate : !matchesTemplate;
                if (shouldRetire) {
                    t.setRetired(true);
                    t.setRetirer(actor);
                    t.setRetiredAt(now);
                    toEdit.add(t);
                    retired++;
                }
            }
        }

        if (op == RoleOperation.RESET || op == RoleOperation.EXPAND) {
            Set<TriggerType> missing = new HashSet<>();
            for (TriggerType tt : templateSet) {
                if (!activeValues.contains(tt)) {
                    missing.add(tt);
                }
            }
            Map<TriggerType, TriggerSubscription> retiredCandidates = fetchRetiredSubscriptionCandidates(user, dept, missing);
            for (TriggerType tt : missing) {
                TriggerSubscription candidate = retiredCandidates.get(tt);
                if (candidate != null) {
                    candidate.setRetired(false);
                    toEdit.add(candidate);
                } else {
                    TriggerSubscription fresh = new TriggerSubscription();
                    fresh.setWebUser(user);
                    fresh.setDepartment(dept);
                    fresh.setTriggerType(tt);
                    TriggerSubscription templateRow = template.get(tt);
                    fresh.setOrderNumber(templateRow != null ? templateRow.getOrderNumber() : 0d);
                    fresh.setCreater(actor);
                    fresh.setCreatedAt(now);
                    toCreate.add(fresh);
                }
                added++;
            }
        }

        result.addCounts(RoleAspect.SUBSCRIPTIONS, added, retired);
    }

    private Map<TriggerType, TriggerSubscription> fetchTemplateSubscriptions(WebUserRole role) {
        List<TriggerSubscription> rows = triggerSubscriptionFacade.findByJpql(
                "select t from TriggerSubscription t where t.webUserRole=:role and t.webUser is null and t.retired=false and t.triggerType is not null",
                params("role", role));
        Map<TriggerType, TriggerSubscription> map = new LinkedHashMap<>();
        for (TriggerSubscription t : rows) {
            map.put(t.getTriggerType(), t);
        }
        return map;
    }

    private List<TriggerSubscription> fetchActiveSubscriptions(WebUser user, Department dept) {
        return triggerSubscriptionFacade.findByJpql(
                "select t from TriggerSubscription t where t.webUser=:user and t.department=:dept and t.retired=false",
                params("user", user, "dept", dept));
    }

    private Map<TriggerType, TriggerSubscription> fetchRetiredSubscriptionCandidates(WebUser user, Department dept, Set<TriggerType> values) {
        Map<TriggerType, TriggerSubscription> map = new HashMap<>();
        if (values.isEmpty()) {
            return map;
        }
        List<TriggerSubscription> rows = triggerSubscriptionFacade.findByJpql(
                "select t from TriggerSubscription t where t.webUser=:user and t.department=:dept and t.retired=true and t.triggerType in :vals order by t.id desc",
                params("user", user, "dept", dept, "vals", values));
        for (TriggerSubscription t : rows) {
            map.putIfAbsent(t.getTriggerType(), t);
        }
        return map;
    }
    // </editor-fold>

    // <editor-fold desc="Login page aspect">
    private void applyLoginPageAspect(RoleOperation op, WebUser user, WebUserRole role, Department dept, WebUser actor, Date now,
            RoleApplicationResult result, List<WebUserDefaultLoginPage> toCreate, List<WebUserDefaultLoginPage> toEdit) {
        LoginPage templateValue = role.getLoginPage();
        WebUserDefaultLoginPage active = fetchActiveLoginPage(user, dept);

        int added = 0;
        int retired = 0;

        switch (op) {
            case RESET:
                if (active != null) {
                    active.setRetired(true);
                    active.setRetirer(actor);
                    active.setRetiredAt(now);
                    toEdit.add(active);
                    retired++;
                }
                if (templateValue != null) {
                    WebUserDefaultLoginPage fresh = new WebUserDefaultLoginPage();
                    fresh.setWebUser(user);
                    fresh.setDepartment(dept);
                    fresh.setLoginPage(templateValue);
                    fresh.setCreater(actor);
                    fresh.setCreatedAt(now);
                    toCreate.add(fresh);
                    added++;
                }
                break;
            case EXPAND:
                if (active == null && templateValue != null) {
                    WebUserDefaultLoginPage fresh = new WebUserDefaultLoginPage();
                    fresh.setWebUser(user);
                    fresh.setDepartment(dept);
                    fresh.setLoginPage(templateValue);
                    fresh.setCreater(actor);
                    fresh.setCreatedAt(now);
                    toCreate.add(fresh);
                    added++;
                }
                break;
            case NARROW:
                if (active != null && templateValue != null && templateValue == active.getLoginPage()) {
                    active.setRetired(true);
                    active.setRetirer(actor);
                    active.setRetiredAt(now);
                    toEdit.add(active);
                    retired++;
                }
                break;
            default:
                break;
        }

        result.addCounts(RoleAspect.LOGIN_PAGE, added, retired);
    }

    private WebUserDefaultLoginPage fetchActiveLoginPage(WebUser user, Department dept) {
        return webUserDefaultLoginPageFacade.findFirstByJpql(
                "select w from WebUserDefaultLoginPage w where w.webUser=:user and w.department=:dept and w.retired=false order by w.id desc",
                params("user", user, "dept", dept));
    }
    // </editor-fold>

    // <editor-fold desc="Preview">
    private long previewAspectCount(RoleOperation op, WebUser user, WebUserRole role, Department dept, RoleAspect aspect) {
        switch (aspect) {
            case PRIVILEGES: {
                Set<Privileges> templateSet = fetchTemplatePrivileges(role);
                Set<Privileges> activeSet = new HashSet<>();
                for (WebUserPrivilege w : fetchActivePrivileges(user, dept)) {
                    if (w.getPrivilege() != null) {
                        activeSet.add(w.getPrivilege());
                    }
                }
                return previewDiffCount(op, templateSet, activeSet);
            }
            case ICONS: {
                Set<Icon> templateSet = fetchTemplateIcons(role).keySet();
                Set<Icon> activeSet = new HashSet<>();
                for (UserIcon u : fetchActiveIcons(user, dept)) {
                    if (u.getIcon() != null) {
                        activeSet.add(u.getIcon());
                    }
                }
                return previewDiffCount(op, templateSet, activeSet);
            }
            case SUBSCRIPTIONS: {
                Set<TriggerType> templateSet = fetchTemplateSubscriptions(role).keySet();
                Set<TriggerType> activeSet = new HashSet<>();
                for (TriggerSubscription t : fetchActiveSubscriptions(user, dept)) {
                    if (t.getTriggerType() != null) {
                        activeSet.add(t.getTriggerType());
                    }
                }
                return previewDiffCount(op, templateSet, activeSet);
            }
            case LOGIN_PAGE: {
                LoginPage templateValue = role.getLoginPage();
                WebUserDefaultLoginPage active = fetchActiveLoginPage(user, dept);
                switch (op) {
                    case RESET:
                        return (active != null ? 1 : 0) + (templateValue != null ? 1 : 0);
                    case EXPAND:
                        return (active == null && templateValue != null) ? 1 : 0;
                    case NARROW:
                        return (active != null && templateValue != null && templateValue == active.getLoginPage()) ? 1 : 0;
                    default:
                        return 0;
                }
            }
            default:
                return 0;
        }
    }

    private <V> long previewDiffCount(RoleOperation op, Set<V> templateSet, Set<V> activeSet) {
        switch (op) {
            case RESET: {
                long count = 0;
                for (V v : activeSet) {
                    if (!templateSet.contains(v)) {
                        count++;
                    }
                }
                for (V v : templateSet) {
                    if (!activeSet.contains(v)) {
                        count++;
                    }
                }
                return count;
            }
            case EXPAND: {
                long count = 0;
                for (V v : templateSet) {
                    if (!activeSet.contains(v)) {
                        count++;
                    }
                }
                return count;
            }
            case NARROW: {
                long count = 0;
                for (V v : activeSet) {
                    if (templateSet.contains(v)) {
                        count++;
                    }
                }
                return count;
            }
            default:
                return 0;
        }
    }
    // </editor-fold>

    private void writeAuditEvent(RoleOperation op, WebUser targetUser, WebUserRole role, List<Department> departments,
            Set<RoleAspect> aspects, RoleApplicationResult result, WebUser actor) {
        try {
            AuditEvent ae = new AuditEvent();
            ae.setEventDataTime(new Date());
            ae.setEventTrigger("ROLE_TEMPLATE_" + op.name());
            ae.setEntityType("WebUser");
            ae.setObjectId(targetUser.getId());
            if (actor != null) {
                ae.setWebUserId(actor.getId());
            }
            ae.setBeforeJson(buildRequestSummary(op, targetUser, role, departments, aspects));
            ae.setAfterJson(result.summary());
            auditEventService.saveAuditEvent(ae);
        } catch (Exception e) {
            // Audit logging must never break the role-template operation itself.
        }
    }

    private String buildRequestSummary(RoleOperation op, WebUser targetUser, WebUserRole role, List<Department> departments, Set<RoleAspect> aspects) {
        StringBuilder sb = new StringBuilder();
        sb.append("operation=").append(op.name());
        sb.append(", user=").append(targetUser.getName());
        sb.append(", role=").append(role != null ? role.getName() : "null");
        sb.append(", departments=").append(departmentNames(departments));
        sb.append(", aspects=").append(aspects);
        return sb.toString();
    }

    private List<String> departmentNames(List<Department> departments) {
        List<String> names = new ArrayList<>();
        for (Department d : departments) {
            if (d != null) {
                names.add(d.getName());
            }
        }
        return names;
    }

    private Map<String, Object> params(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

}
