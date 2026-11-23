package com.divudi.service;

import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ItemFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Shared helper that records every create and every name-type change
 * (name, code, printName, fullName, shortName) of an Item or any of its
 * subtypes (AMP, VMP, VTM, ATM, AMPP, VMPP, Service, Investigation, TimedItem,
 * Package, ...) in the audit database, through {@link AuditService}.
 *
 * Usage on an edit path:
 * <pre>
 *   Map&lt;String, Object&gt; before = itemAuditHelper.snapshotFromDb(item.getId()); // before the save
 *   facade.edit(item);
 *   itemAuditHelper.logUpdate(before, item, user, "UI");
 * </pre>
 * Where the entity is loaded and changed inside one transaction (API
 * services), take {@link #snapshot(Item)} right after loading, before any
 * setter is called.
 *
 * An audit failure never blocks the save: every method catches and logs.
 */
@Stateless
public class ItemAuditHelper {

    private static final Logger LOGGER = Logger.getLogger(ItemAuditHelper.class.getName());

    public static final String TRIGGER_CREATE = "Item Create";
    public static final String TRIGGER_NAME_CHANGE = "Item Name Change";

    @EJB
    private AuditService auditService;
    @EJB
    private ItemFacade itemFacade;

    /**
     * Adds the three name fields that the older per-controller audit maps did
     * not carry.
     */
    public static void putNameFields(Map<String, Object> target, Item item) {
        if (target == null || item == null) {
            return;
        }
        target.put("printName", item.getPrintName());
        target.put("fullName", item.getFullName());
        target.put("shortName", item.getShortName());
    }

    /**
     * Name fields of an item as they are right now in memory.
     */
    public static Map<String, Object> snapshot(Item item) {
        if (item == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("name", item.getName());
        m.put("code", item.getCode());
        m.put("printName", item.getPrintName());
        m.put("fullName", item.getFullName());
        m.put("shortName", item.getShortName());
        return m;
    }

    /**
     * Name fields of an item as stored in the database, found fresh by id.
     * Call this before the new values are saved. Returns null for a new item
     * (no id) or when the lookup fails.
     */
    public Map<String, Object> snapshotFromDb(Long id) {
        if (id == null) {
            return null;
        }
        try {
            Item stored = itemFacade.find(id);
            return snapshot(stored);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not read item " + id + " for audit", e);
            return null;
        }
    }

    public static boolean namesChanged(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return false;
        }
        String[] keys = {"name", "code", "printName", "fullName", "shortName"};
        for (String k : keys) {
            if (!Objects.equals(emptyToNull(before.get(k)), emptyToNull(after.get(k)))) {
                return true;
            }
        }
        return false;
    }

    private static Object emptyToNull(Object o) {
        if (o instanceof String && ((String) o).isEmpty()) {
            return null;
        }
        return o;
    }

    /**
     * Always logs a create.
     *
     * @param source where the change came from, e.g. "UI", "API", "Upload"
     */
    public void logCreate(Item item, WebUser user, String source) {
        try {
            if (item == null) {
                return;
            }
            Map<String, Object> after = snapshot(item);
            after.put("source", source);
            auditService.logAudit(null, after, user, entityType(item), TRIGGER_CREATE, item.getId());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Item create audit failed", e);
        }
    }

    /**
     * Logs an update only when one of name, code, printName, fullName or
     * shortName differs from the "before" snapshot.
     *
     * @param before snapshot taken before the save; null (previous state
     * unknown) means nothing is logged
     */
    public void logUpdate(Map<String, Object> before, Item item, WebUser user, String source) {
        try {
            if (before == null || item == null) {
                return;
            }
            Map<String, Object> after = snapshot(item);
            if (!namesChanged(before, after)) {
                return;
            }
            before.put("source", source);
            after.put("source", source);
            auditService.logAudit(before, after, user, entityType(item), TRIGGER_NAME_CHANGE, item.getId());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Item name-change audit failed", e);
        }
    }

    /**
     * Same as {@link #logUpdate} but looks the saved item up again by id,
     * for callers that only keep the id (API services that apply an update
     * inside a helper and return a DTO).
     */
    public void logUpdateById(Map<String, Object> before, Long id, WebUser user, String source) {
        try {
            if (before == null || id == null) {
                return;
            }
            logUpdate(before, itemFacade.find(id), user, source);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Item name-change audit failed", e);
        }
    }

    /**
     * For paths where the same code serves create and edit: creates are
     * logged when {@code before} is null, edits when it is not.
     */
    public void logSave(Map<String, Object> before, Item item, WebUser user, String source) {
        if (before == null) {
            logCreate(item, user, source);
        } else {
            logUpdate(before, item, user, source);
        }
    }

    private static String entityType(Item item) {
        return item.getClass().getSimpleName();
    }
}
