package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26090: saving the privilege tree must change only what was ticked or
 * unticked.
 */
public class PrivilegeSaveDiffTest {

    /** Stand-in for a stored privilege row. */
    private static final class Row {

        final Privileges privilege;

        Row(Privileges privilege) {
            this.privilege = privilege;
        }
    }

    private static final Function<Object, Privileges> NODE_PRIVILEGE = d -> d instanceof Privileges ? (Privileges) d : null;

    private static PrivilegeSaveDiff.Result<Row> diff(List<Row> rows, PrivilegeSaveDiff.TreeState state) {
        return PrivilegeSaveDiff.compute(rows, r -> r.privilege, state);
    }

    @Test
    public void tickingOneKeepsEveryOtherRow() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode group = new DefaultTreeNode("Room", root);
        TreeNode a = new DefaultTreeNode(Privileges.InwardRoom, group);
        TreeNode b = new DefaultTreeNode(Privileges.InwardRoomOccupancyAnyInstitute, group);
        TreeNode c = new DefaultTreeNode(Privileges.LabBillSearch, root);

        Row ra = new Row(Privileges.InwardRoom);
        Row rc = new Row(Privileges.LabBillSearch);
        // Admin ticks b; a and c were already ticked.
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Arrays.asList(a, b, c), NODE_PRIVILEGE);
        PrivilegeSaveDiff.Result<Row> r = diff(Arrays.asList(ra, rc), state);

        assertTrue(r.getToRetire().isEmpty());
        assertEquals(Collections.singletonList(Privileges.InwardRoomOccupancyAnyInstitute), r.getToAdd());
    }

    @Test
    public void untickedOfferedPrivilegeIsRetired() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode a = new DefaultTreeNode(Privileges.InwardRoom, root);
        new DefaultTreeNode(Privileges.LabBillSearch, root);

        Row ra = new Row(Privileges.InwardRoom);
        Row rc = new Row(Privileges.LabBillSearch);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Collections.singletonList(a), NODE_PRIVILEGE);
        PrivilegeSaveDiff.Result<Row> r = diff(Arrays.asList(ra, rc), state);

        assertEquals(Collections.singletonList(rc), r.getToRetire());
        assertTrue(r.getToAdd().isEmpty());
    }

    @Test
    public void privilegeNotOfferedInTreeIsKept() {
        TreeNode root = new DefaultTreeNode("root", null);
        new DefaultTreeNode(Privileges.InwardRoom, root);

        Row notOffered = new Row(Privileges.User);
        Row unknown = new Row(null);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, null, NODE_PRIVILEGE);
        PrivilegeSaveDiff.Result<Row> r = diff(Arrays.asList(notOffered, unknown), state);

        assertTrue(r.getToRetire().isEmpty());
        assertTrue(r.getToAdd().isEmpty());
    }

    @Test
    public void duplicateRowsOfTickedPrivilegeAreAllKept() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode first = new DefaultTreeNode(Privileges.LabBillSearch, root);
        TreeNode second = new DefaultTreeNode(Privileges.LabBillSearch, root);
        TreeNode sale = new DefaultTreeNode(Privileges.PharmacySaleWithoutStock, root);

        List<Row> rows = Arrays.asList(
                new Row(Privileges.LabBillSearch),
                new Row(Privileges.LabBillSearch),
                new Row(Privileges.PharmacySaleWithoutStock),
                new Row(Privileges.PharmacySaleWithOutStock));
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Arrays.asList(first, second, sale), NODE_PRIVILEGE);
        PrivilegeSaveDiff.Result<Row> r = diff(rows, state);

        assertTrue(r.getToRetire().isEmpty());
        assertTrue(r.getToAdd().isEmpty());
    }

    @Test
    public void legacySpellingRowIsNotDuplicatedOrRetired() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode sale = new DefaultTreeNode(Privileges.PharmacySaleWithoutStock, root);

        Row legacy = new Row(Privileges.PharmacySaleWithOutStock);
        PrivilegeSaveDiff.Result<Row> kept = diff(Collections.singletonList(legacy),
                PrivilegeSaveDiff.readTree(root, Collections.singletonList(sale), NODE_PRIVILEGE));
        assertTrue(kept.getToRetire().isEmpty());
        assertTrue(kept.getToAdd().isEmpty());

        PrivilegeSaveDiff.Result<Row> unticked = diff(Collections.singletonList(legacy),
                PrivilegeSaveDiff.readTree(root, null, NODE_PRIVILEGE));
        assertEquals(Collections.singletonList(legacy), unticked.getToRetire());
    }

    @Test
    public void partTickedPrivilegeParentIsLeftAsItIs() {
        // A privilege node with children (like "Cashier") drops out of the
        // checkbox selection when only some children are ticked.
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode parent = new DefaultTreeNode(Privileges.OpdCollectingCentreBilling, root);
        TreeNode child1 = new DefaultTreeNode(Privileges.OpdCollectingCentreBillingMenu, parent);
        new DefaultTreeNode(Privileges.OpdCollectingCentreBillSearch, parent);

        Row parentRow = new Row(Privileges.OpdCollectingCentreBilling);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Collections.singletonList(child1), NODE_PRIVILEGE);
        PrivilegeSaveDiff.Result<Row> r = diff(Collections.singletonList(parentRow), state);

        assertTrue(r.getToRetire().isEmpty());
        assertEquals(Collections.singletonList(Privileges.OpdCollectingCentreBillingMenu), r.getToAdd());

        // Not held and part-ticked: not added either.
        PrivilegeSaveDiff.Result<Row> none = diff(new ArrayList<>(), state);
        assertEquals(Collections.singletonList(Privileges.OpdCollectingCentreBillingMenu), none.getToAdd());
    }

    @Test
    public void fullyUntickedPrivilegeParentIsRetired() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode parent = new DefaultTreeNode(Privileges.OpdCollectingCentreBilling, root);
        new DefaultTreeNode(Privileges.OpdCollectingCentreBillingMenu, parent);

        Row parentRow = new Row(Privileges.OpdCollectingCentreBilling);
        PrivilegeSaveDiff.Result<Row> r = diff(Collections.singletonList(parentRow),
                PrivilegeSaveDiff.readTree(root, null, NODE_PRIVILEGE));

        assertEquals(Collections.singletonList(parentRow), r.getToRetire());
    }

    @Test
    public void rolePrivilegesReplaceOnlyOfferedOnes() {
        Row offeredNotInRole = new Row(Privileges.InwardRoom);
        Row inRole = new Row(Privileges.LabBillSearch);
        Row notOffered = new Row(Privileges.User);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.forPrivileges(
                Arrays.asList(Privileges.InwardRoom, Privileges.LabBillSearch, Privileges.LabBilling),
                Arrays.asList(Privileges.LabBillSearch, Privileges.LabBilling));
        PrivilegeSaveDiff.Result<Row> r = diff(Arrays.asList(offeredNotInRole, inRole, notOffered), state);

        assertEquals(Collections.singletonList(offeredNotInRole), r.getToRetire());
        assertEquals(Collections.singletonList(Privileges.LabBilling), r.getToAdd());
    }

    @Test
    public void realTreeSavedUnchangedRetiresNothing() {
        UserPrivilageController controller = new UserPrivilageController();
        TreeNode root = controller.getRootTreeNode();
        List<TreeNode> selected = new ArrayList<>();
        collect(root, Privileges.LabBillSearch, selected);
        collect(root, Privileges.PharmacySaleWithoutStock, selected);
        assertTrue(selected.size() >= 2);

        List<Row> rows = Arrays.asList(
                new Row(Privileges.LabBillSearch),
                new Row(Privileges.PharmacySaleWithOutStock),
                new Row(Privileges.User));
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, selected, UserPrivilageController::privilegeOfNodeData);
        PrivilegeSaveDiff.Result<Row> r = diff(rows, state);

        assertTrue(r.getToRetire().isEmpty());
        assertTrue(r.getToAdd().isEmpty());
    }

    @Test
    public void rebuiltTreeMatchesByPrivilegeValue() {
        // Nodes ticked on one tree instance, read against a rebuilt tree.
        TreeNode renderedRoot = new UserPrivilageController().getRootTreeNode();
        TreeNode rebuiltRoot = new UserPrivilageController().getRootTreeNode();
        List<TreeNode> ticked = new ArrayList<>();
        collect(renderedRoot, Privileges.LabBillSearch, ticked);
        collect(renderedRoot, Privileges.InwardRoomOccupancyAnyInstitute, ticked);

        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(rebuiltRoot, ticked, UserPrivilageController::privilegeOfNodeData);
        assertTrue(state.getSelected().contains(Privileges.LabBillSearch));
        assertTrue(state.getSelected().contains(Privileges.InwardRoomOccupancyAnyInstitute));

        List<Row> rows = Arrays.asList(new Row(Privileges.LabBillSearch), new Row(Privileges.User));
        PrivilegeSaveDiff.Result<Row> r = diff(rows, state);
        assertTrue(r.getToRetire().isEmpty());
        assertEquals(Collections.singletonList(Privileges.InwardRoomOccupancyAnyInstitute), r.getToAdd());

        // The save itself is refused, because the ticked nodes are not from the current tree.
        assertTrue(state.isForeignSelection());
        assertNotNull(PrivilegeSaveDiff.refusalReason(state, rows, x -> x.privilege));
    }

    @Test
    public void detachedNodesWithEqualPrivilegeMatch() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode parent = new DefaultTreeNode(Privileges.OpdCollectingCentreBilling, root);
        new DefaultTreeNode(Privileges.OpdCollectingCentreBillingMenu, parent);
        new DefaultTreeNode(Privileges.InwardRoom, root);
        // Different node objects carrying equal privilege values.
        TreeNode detachedMenu = new DefaultTreeNode(Privileges.OpdCollectingCentreBillingMenu, null);
        TreeNode detachedRoom = new DefaultTreeNode(Privileges.InwardRoom, null);

        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Arrays.asList(detachedMenu, detachedRoom), NODE_PRIVILEGE);
        assertEquals(new java.util.HashSet<>(Arrays.asList(Privileges.OpdCollectingCentreBillingMenu, Privileges.InwardRoom)), state.getSelected());
        // Part-ticked parent is found by value too.
        assertEquals(Collections.singleton(Privileges.OpdCollectingCentreBilling), state.getUndetermined());

        Row parentRow = new Row(Privileges.OpdCollectingCentreBilling);
        Row room = new Row(Privileges.InwardRoom);
        PrivilegeSaveDiff.Result<Row> r = diff(Arrays.asList(parentRow, room), state);
        assertTrue(r.getToRetire().isEmpty());
        assertEquals(Collections.singletonList(Privileges.OpdCollectingCentreBillingMenu), r.getToAdd());
    }

    @Test
    public void partSelectedFlagMarksParentAsPartTicked() {
        CheckboxTreeNode root = new CheckboxTreeNode("root", null);
        CheckboxTreeNode parent = new CheckboxTreeNode(Privileges.Cashier, root);
        new CheckboxTreeNode(Privileges.OpdBilling, parent);
        parent.setPartialSelected(true);

        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, null, NODE_PRIVILEGE);
        assertTrue(state.getUndetermined().contains(Privileges.Cashier));
        assertTrue(diff(Collections.singletonList(new Row(Privileges.Cashier)), state).getToRetire().isEmpty());
    }

    @Test
    public void emptySelectionThatWouldRemoveShownPrivilegesIsRefused() {
        TreeNode root = new DefaultTreeNode("root", null);
        new DefaultTreeNode(Privileges.InwardRoom, root);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, null, NODE_PRIVILEGE);

        assertNotNull(PrivilegeSaveDiff.refusalReason(state, Collections.singletonList(new Row(Privileges.InwardRoom)), x -> x.privilege));
        // Nothing shown would be removed: allowed.
        assertNull(PrivilegeSaveDiff.refusalReason(state, Collections.singletonList(new Row(Privileges.User)), x -> x.privilege));
        assertNull(PrivilegeSaveDiff.refusalReason(state, new ArrayList<Row>(), x -> x.privilege));
    }

    @Test
    public void currentTreeSelectionIsNotRefused() {
        TreeNode root = new DefaultTreeNode("root", null);
        TreeNode a = new DefaultTreeNode(Privileges.InwardRoom, root);
        new DefaultTreeNode(Privileges.LabBillSearch, root);
        PrivilegeSaveDiff.TreeState state = PrivilegeSaveDiff.readTree(root, Collections.singletonList(a), NODE_PRIVILEGE);

        assertFalse(state.isForeignSelection());
        assertNull(PrivilegeSaveDiff.refusalReason(state,
                Arrays.asList(new Row(Privileges.InwardRoom), new Row(Privileges.LabBillSearch)), x -> x.privilege));
    }

    private static void collect(TreeNode node, Privileges p, List<TreeNode> out) {
        if (UserPrivilageController.privilegeOfNodeData(node.getData()) == p) {
            out.add(node);
        }
        for (Object child : node.getChildren()) {
            collect((TreeNode) child, p, out);
        }
    }
}
