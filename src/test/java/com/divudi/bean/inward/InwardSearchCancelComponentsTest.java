package com.divudi.bean.inward;

import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillItem;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** #25843: each reversed item gets only its own components. */
public class InwardSearchCancelComponentsTest {

    private static BillItem item(long id) {
        BillItem i = new BillItem();
        i.setId(id);
        return i;
    }

    private static BillComponent comp(BillItem owner) {
        BillComponent c = new BillComponent();
        c.setBillItem(owner);
        return c;
    }

    @Test
    public void twoItemsEachGetOnlyTheirOwnComponent() {
        BillItem a = item(1L);
        BillItem b = item(2L);
        BillComponent ca = comp(a);
        BillComponent cb = comp(b);
        List<BillComponent> all = Arrays.asList(ca, cb);

        List<BillComponent> forA = InwardSearch.componentsForItem(all, item(1L));
        assertEquals(1, forA.size());
        assertSame(ca, forA.get(0));
        assertSame(cb, InwardSearch.componentsForItem(all, b).get(0));
    }

    @Test
    public void componentsWithoutItemAreSeparate() {
        BillComponent orphan = comp(null);
        List<BillComponent> all = Arrays.asList(comp(item(1L)), orphan);

        assertEquals(0, InwardSearch.componentsForItem(all, item(3L)).size());
        assertEquals(1, InwardSearch.componentsWithoutItem(all).size());
        assertSame(orphan, InwardSearch.componentsWithoutItem(all).get(0));
    }
}
