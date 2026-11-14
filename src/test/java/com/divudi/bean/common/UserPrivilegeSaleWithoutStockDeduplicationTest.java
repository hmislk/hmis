package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.TreeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Issue #25602:
 * Privilege tree lists the sale-without-stock privilege in several nodes,
 * so saving can retire a ticked privilege.
 */
public class UserPrivilegeSaleWithoutStockDeduplicationTest {

    private UserPrivilageController controller;

    @BeforeEach
    public void setUp() {
        controller = new UserPrivilageController();
    }

    @Test
    public void testPrivilegeTreeHasSingleSaleWithoutStockNode() {
        TreeNode root = controller.getRootTreeNode();
        int count = countPrivilegeInTree(root, Privileges.PharmacySaleWithoutStock);
        count += countPrivilegeInTree(root, Privileges.PharmacySaleWithOutStock);

        assertEquals(1, count, "Pharmacy sale without stock privilege must appear exactly once in the tree");
    }

    @Test
    public void testPrivilegeHolderEquivalence() {
        UserPrivilageController.PrivilegeHolder holder1 = controller.new PrivilegeHolder(Privileges.PharmacySaleWithoutStock, "Pharmacy Sale Without Stock");
        UserPrivilageController.PrivilegeHolder holder2 = controller.new PrivilegeHolder(Privileges.PharmacySaleWithOutStock, "Pharmacy Sale With Out Stock");

        assertEquals(holder1, holder2);
        assertEquals(holder2, holder1);
        assertEquals(holder1.hashCode(), holder2.hashCode());
    }

    private int countPrivilegeInTree(TreeNode node, Privileges target) {
        if (node == null) {
            return 0;
        }
        int count = 0;
        Object data = node.getData();
        if (data instanceof UserPrivilageController.PrivilegeHolder) {
            UserPrivilageController.PrivilegeHolder ph = (UserPrivilageController.PrivilegeHolder) data;
            if (ph.getPrivilege() == target) {
                count++;
            }
        }
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode) {
                count += countPrivilegeInTree((TreeNode) childObj, target);
            }
        }
        return count;
    }
}
