package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.primefaces.model.TreeNode;

/**
 * Works out what a privilege-tree save changes (#26090).
 *
 * A save changes only what the admin ticked or unticked:
 * <ul>
 * <li>a stored row is retired only when its privilege is offered in the tree
 * and is not selected;</li>
 * <li>rows whose privilege the tree does not offer, or whose privilege cannot
 * be read (null), are kept;</li>
 * <li>a privilege node that also has child nodes and is shown part-ticked
 * (not selected, but some child selected) is left as it is, because the
 * checkbox tree drops such a node from the selection when a child is
 * ticked;</li>
 * <li>only selected privileges that have no stored row are added; existing
 * rows are kept, so their ids stay stable.</li>
 * </ul>
 * Privileges are compared with {@link UserPrivilageController#canonicalPrivilege}
 * so both stored spellings of one permission match.
 */
public final class PrivilegeSaveDiff {

    private PrivilegeSaveDiff() {
    }

    /**
     * What the tree offers and what the admin left selected.
     */
    public static final class TreeState {

        private final Set<Privileges> offered = new LinkedHashSet<>();
        private final Set<Privileges> selected = new LinkedHashSet<>();
        private final Set<Privileges> undetermined = new LinkedHashSet<>();

        public Set<Privileges> getOffered() {
            return Collections.unmodifiableSet(offered);
        }

        public Set<Privileges> getSelected() {
            return Collections.unmodifiableSet(selected);
        }

        public Set<Privileges> getUndetermined() {
            return Collections.unmodifiableSet(undetermined);
        }
    }

    /**
     * Rows to retire and privileges to add.
     *
     * @param <R> stored row type
     */
    public static final class Result<R> {

        private final List<R> toRetire = new ArrayList<>();
        private final List<Privileges> toAdd = new ArrayList<>();

        public List<R> getToRetire() {
            return toRetire;
        }

        public List<Privileges> getToAdd() {
            return toAdd;
        }
    }

    /**
     * Reads the tree.
     *
     * @param root tree root
     * @param selectedNodes nodes the tree submitted as selected (may be null)
     * @param privilegeOf reads the privilege from a node's data, or null for a
     * grouping node
     * @return the offered, selected and part-ticked privileges
     */
    public static TreeState readTree(TreeNode root, Collection<TreeNode> selectedNodes, Function<Object, Privileges> privilegeOf) {
        TreeState state = new TreeState();
        Set<TreeNode> selectedSet = Collections.newSetFromMap(new IdentityHashMap<>());
        if (selectedNodes != null) {
            for (TreeNode n : selectedNodes) {
                if (n != null) {
                    selectedSet.add(n);
                }
            }
        }
        if (root != null) {
            walk(root, selectedSet, privilegeOf, state);
        }
        state.undetermined.removeAll(state.selected);
        return state;
    }

    /**
     * Builds a state from plain lists, for a save that does not come from a
     * tree selection (for example applying a role's privileges).
     *
     * @param offered privileges the save may change
     * @param selected privileges that must be live after the save
     * @return the state; selected privileges count as offered
     */
    public static TreeState forPrivileges(Collection<Privileges> offered, Collection<Privileges> selected) {
        TreeState state = new TreeState();
        if (offered != null) {
            for (Privileges p : offered) {
                Privileges c = UserPrivilageController.canonicalPrivilege(p);
                if (c != null) {
                    state.offered.add(c);
                }
            }
        }
        if (selected != null) {
            for (Privileges p : selected) {
                Privileges c = UserPrivilageController.canonicalPrivilege(p);
                if (c != null) {
                    state.offered.add(c);
                    state.selected.add(c);
                }
            }
        }
        return state;
    }

    /**
     * Walks a node and its children. Returns true when this node or any
     * descendant is selected.
     */
    private static boolean walk(TreeNode node, Set<TreeNode> selectedSet, Function<Object, Privileges> privilegeOf, TreeState state) {
        boolean nodeSelected = selectedSet.contains(node);
        boolean descendantSelected = false;
        List<?> children = node.getChildren();
        if (children != null) {
            for (Object child : children) {
                if (child instanceof TreeNode && walk((TreeNode) child, selectedSet, privilegeOf, state)) {
                    descendantSelected = true;
                }
            }
        }
        Privileges p = UserPrivilageController.canonicalPrivilege(privilegeOf.apply(node.getData()));
        if (p != null) {
            state.offered.add(p);
            if (nodeSelected) {
                state.selected.add(p);
            } else if (descendantSelected) {
                state.undetermined.add(p);
            }
        }
        return nodeSelected || descendantSelected;
    }

    /**
     * Computes the change for a save.
     *
     * @param <R> stored row type
     * @param currentRows the live stored rows
     * @param privilegeOfRow reads a row's privilege
     * @param state the tree state
     * @return rows to retire and privileges to add
     */
    public static <R> Result<R> compute(List<R> currentRows, Function<R, Privileges> privilegeOfRow, TreeState state) {
        Result<R> result = new Result<>();
        Set<Privileges> held = new LinkedHashSet<>();
        if (currentRows != null) {
            for (R row : currentRows) {
                Privileges p = UserPrivilageController.canonicalPrivilege(privilegeOfRow.apply(row));
                if (p == null) {
                    continue;
                }
                held.add(p);
                if (state.offered.contains(p)
                        && !state.selected.contains(p)
                        && !state.undetermined.contains(p)) {
                    result.toRetire.add(row);
                }
            }
        }
        for (Privileges p : state.selected) {
            if (!held.contains(p)) {
                result.toAdd.add(p);
            }
        }
        return result;
    }
}
