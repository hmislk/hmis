package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Remembers the last privilege list shown in the shared privilege tree
 * (#26090): which target it was for (a user in a department, or a role) and
 * the privileges shown as ticked (the save baseline). A save may go ahead
 * only when the list is loaded and was listed for that same target. Each
 * List sets it; each tree reload or page navigation clears it.
 */
public class PrivilegeListGuard implements Serializable {

    private static final long serialVersionUID = 1L;

    private String listedKey;
    private Set<Privileges> baseline;

    public static String userKey(Long userId, Long departmentId) {
        return "user:" + userId + "|" + departmentId;
    }

    public static String roleKey(Long roleId) {
        return "role:" + roleId;
    }

    /**
     * Records a list.
     *
     * @param key the target key ({@link #userKey} or {@link #roleKey})
     * @param shownTicked privileges shown as ticked
     */
    public void listed(String key, Set<Privileges> shownTicked) {
        this.listedKey = key;
        this.baseline = shownTicked == null ? new LinkedHashSet<>() : new LinkedHashSet<>(shownTicked);
    }

    /**
     * Forgets the last list. Called when the tree must be listed again.
     */
    public void clear() {
        this.listedKey = null;
        this.baseline = null;
    }

    /**
     * The baseline for a save, or null when the save must be refused: the
     * list is not loaded, or was listed for another target.
     *
     * @param privilegesLoaded whether the page shows a loaded list
     * @param key the save's target key
     * @return the baseline, or null
     */
    public Set<Privileges> baselineForSave(boolean privilegesLoaded, String key) {
        if (!privilegesLoaded || listedKey == null || baseline == null || !Objects.equals(listedKey, key)) {
            return null;
        }
        return Collections.unmodifiableSet(baseline);
    }

    public String getListedKey() {
        return listedKey;
    }
}
