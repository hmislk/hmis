package com.divudi.bean.pharmacy;

import com.divudi.core.data.dto.DuplicateItemDto;
import com.divudi.core.data.dto.DuplicateItemGroup;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

/**
 * Detects and lists <b>possible duplicate pharmaceutical items</b> for review
 * under Administration &gt; Pharmaceutical Management &gt; Check Entered Data.
 *
 * <p>Duplicate item masters (e.g. two AMPs sharing the same item code but a
 * stray difference in name) silently split stock across two item ids. That
 * causes confusing situations such as a transfer issue page showing a blank
 * stock/expiry/quantity row, because the requested item id has no stock in the
 * issuing department even though the "same" product (a different item id) does
 * (see issue #21313).</p>
 *
 * <p>This is a <b>read-only</b> report: it groups items that look like
 * duplicates so an administrator can recognise and clean them up via the
 * existing AMP/AMPP/VMP/VMPP screens. It does not retire or merge anything.</p>
 *
 * <p>Four detection strategies are offered, each independently toggleable:</p>
 * <ul>
 *   <li><b>Same Code</b> – items sharing a non-blank item code.</li>
 *   <li><b>Same Barcode</b> – items sharing a non-blank barcode.</li>
 *   <li><b>Same Name</b> – items whose names match after trimming, collapsing
 *       internal whitespace and ignoring case.</li>
 *   <li><b>Similar Name</b> – a fuzzy pass that groups names within a small
 *       edit distance of each other (catches spelling/spacing variants that do
 *       not normalise identically).</li>
 * </ul>
 */
@Named
@ViewScoped
public class DuplicatePharmaceuticalItemController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private StockFacade stockFacade;

    // Detection strategy toggles (all on by default)
    private boolean detectByCode = true;
    private boolean detectByBarcode = true;
    private boolean detectByName = true;
    private boolean detectByFuzzyName = false;

    /**
     * Max normalised-name edit distance to treat two names as "similar" in the
     * fuzzy pass. Kept small to avoid flooding the report with false positives.
     */
    private int fuzzyMaxDistance = 2;

    private List<DuplicateItemGroup> duplicateGroups;
    private boolean searched;

    public String navigateToPossibleDuplicateItems() {
        duplicateGroups = null;
        searched = false;
        return "/pharmacy/pharmacy_report_possible_duplicate_items?faces-redirect=true";
    }

    /**
     * Runs the enabled detection strategies and builds the grouped result.
     */
    public void detectDuplicates() {
        searched = true;
        duplicateGroups = new ArrayList<>();

        List<DuplicateItemDto> items = fetchCandidateItems();
        if (items.isEmpty()) {
            return;
        }

        populateStockTotals(items);

        if (detectByCode) {
            duplicateGroups.addAll(groupByKey(items, DuplicateItemGroup.MatchType.CODE));
        }
        if (detectByBarcode) {
            duplicateGroups.addAll(groupByKey(items, DuplicateItemGroup.MatchType.BARCODE));
        }
        if (detectByName) {
            duplicateGroups.addAll(groupByKey(items, DuplicateItemGroup.MatchType.NAME));
        }
        if (detectByFuzzyName) {
            duplicateGroups.addAll(groupByFuzzyName(items));
        }
    }

    /**
     * Loads all non-retired pharmaceutical item masters (Amp/Ampp/Vmp/Vmpp)
     * with the few fields the report needs.
     */
    @SuppressWarnings("unchecked")
    private List<DuplicateItemDto> fetchCandidateItems() {
        String dto = "com.divudi.core.data.dto.DuplicateItemDto";
        List<DuplicateItemDto> all = new ArrayList<>();

        // For AMP/VMP, stock is keyed by the item itself, so stockItemId = i.id.
        // For AMPP/VMPP, stock is keyed by the backing AMP/VMP, so we project
        // that id as stockItemId (mirrors StockController.listStocksOfSelectedItem).
        // type label, type entity, stock-item-id projection
        String[][] typeQueries = {
            {"AMP", "Amp", "i.id"},
            {"AMPP", "Ampp", "i.amp.id"},
            {"VMP", "Vmp", "i.id"},
            {"VMPP", "Vmpp", "i.vmp.id"}
        };

        for (String[] tq : typeQueries) {
            String jpql = "SELECT new " + dto + "("
                    + "i.id, i.name, COALESCE(i.code,''), COALESCE(i.barcode,''), '" + tq[0] + "', "
                    + "COALESCE(" + tq[2] + ", i.id)) "
                    + "FROM " + tq[1] + " i "
                    + "WHERE i.retired = false";
            all.addAll((List<DuplicateItemDto>) itemFacade.findLightsByJpql(jpql));
        }
        return all;
    }

    /**
     * Sums stock across all departments for each candidate item in a single
     * query and writes it back onto the DTOs.
     */
    private void populateStockTotals(List<DuplicateItemDto> items) {
        // Stock is keyed by the AMP/VMP. Several DTOs can map to the same stock
        // item id (an AMP and each of its AMPPs), so index a list per stock id.
        Map<Long, List<DuplicateItemDto>> byStockItemId = new HashMap<>();
        for (DuplicateItemDto it : items) {
            Long stockItemId = it.getStockItemId() != null ? it.getStockItemId() : it.getId();
            byStockItemId.computeIfAbsent(stockItemId, k -> new ArrayList<>()).add(it);
        }
        if (byStockItemId.isEmpty()) {
            return;
        }

        String jpql = "SELECT s.itemBatch.item.id, SUM(s.stock) "
                + "FROM Stock s "
                + "WHERE s.retired = false "
                + "AND s.itemBatch.item.id IN :ids "
                + "GROUP BY s.itemBatch.item.id";
        Map<String, Object> params = new HashMap<>();
        params.put("ids", new ArrayList<>(byStockItemId.keySet()));

        List<Object[]> rows = stockFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Long itemId = (Long) row[0];
            Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            List<DuplicateItemDto> dtos = byStockItemId.get(itemId);
            if (dtos != null) {
                for (DuplicateItemDto dto : dtos) {
                    dto.setTotalStock(total);
                }
            }
        }
    }

    /**
     * Groups items that share the same key (code, barcode or normalised name)
     * and returns only the groups with more than one member.
     */
    private List<DuplicateItemGroup> groupByKey(List<DuplicateItemDto> items, DuplicateItemGroup.MatchType matchType) {
        Map<String, List<DuplicateItemDto>> buckets = new LinkedHashMap<>();
        for (DuplicateItemDto it : items) {
            String key = keyFor(it, matchType);
            if (key == null || key.isEmpty()) {
                continue;
            }
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
        }

        List<DuplicateItemGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<DuplicateItemDto>> e : buckets.entrySet()) {
            if (e.getValue().size() > 1) {
                groups.add(new DuplicateItemGroup(matchType, e.getKey(), e.getValue()));
            }
        }
        return groups;
    }

    private String keyFor(DuplicateItemDto it, DuplicateItemGroup.MatchType matchType) {
        switch (matchType) {
            case CODE:
                return it.getCode() == null ? null : it.getCode().trim().toUpperCase();
            case BARCODE:
                return it.getBarcode() == null ? null : it.getBarcode().trim().toUpperCase();
            case NAME:
                return normalizeName(it.getName());
            default:
                return null;
        }
    }

    /**
     * Trims, collapses internal whitespace and upper-cases a name so that
     * "SPOON  - WOOD SPOON" and "SPOON - WOOD SPOON" normalise to the same key.
     */
    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /**
     * Fuzzy pass: groups normalised names that are within {@link #fuzzyMaxDistance}
     * edit operations of each other but are NOT already identical (those are
     * caught by the Same Name pass).
     *
     * <p>Uses Union-Find over the similarity graph so transitive chains are
     * captured: if A~B and B~C (but A is not directly close to C), all three
     * still land in one group. Intended for occasional administrative review,
     * not high-volume use (the pairwise comparison is O(n^2) over distinct
     * normalised names).</p>
     */
    private List<DuplicateItemGroup> groupByFuzzyName(List<DuplicateItemDto> items) {
        // De-duplicate to one representative per exact normalised name first,
        // so the O(n^2) pass works over distinct names, not raw rows.
        Map<String, List<DuplicateItemDto>> byNormName = new LinkedHashMap<>();
        for (DuplicateItemDto it : items) {
            String n = normalizeName(it.getName());
            if (n == null || n.isEmpty()) {
                continue;
            }
            byNormName.computeIfAbsent(n, k -> new ArrayList<>()).add(it);
        }

        List<String> names = new ArrayList<>(byNormName.keySet());
        int n = names.size();

        // Union-Find: every name starts as its own component.
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        // Add an undirected edge (union) for each pair within the fuzzy distance.
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int distance = boundedLevenshtein(names.get(i), names.get(j), fuzzyMaxDistance);
                if (distance >= 1 && distance <= fuzzyMaxDistance) {
                    union(parent, i, j);
                }
            }
        }

        // Collect members per connected component, preserving first-seen order.
        Map<Integer, List<DuplicateItemDto>> components = new LinkedHashMap<>();
        Map<Integer, String> componentKey = new LinkedHashMap<>();
        Map<Integer, Integer> distinctNameCount = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            components.computeIfAbsent(root, k -> new ArrayList<>()).addAll(byNormName.get(names.get(i)));
            componentKey.putIfAbsent(root, names.get(i));
            distinctNameCount.merge(root, 1, Integer::sum);
        }

        List<DuplicateItemGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<DuplicateItemDto>> e : components.entrySet()) {
            // Only report components that fuzzy-linked at least two DISTINCT
            // normalised names. A component of a single name (even with several
            // items) is an exact-name duplicate already covered by the Same Name
            // pass, so we skip it here to avoid redundant groups.
            if (distinctNameCount.getOrDefault(e.getKey(), 0) > 1) {
                groups.add(new DuplicateItemGroup(
                        DuplicateItemGroup.MatchType.FUZZY_NAME, componentKey.get(e.getKey()), e.getValue()));
            }
        }
        return groups;
    }

    private int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]; // path halving
            x = parent[x];
        }
        return x;
    }

    private void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[ra] = rb;
        }
    }

    /**
     * Levenshtein edit distance with early exit once the distance is known to
     * exceed {@code max} (returns {@code max + 1} in that case).
     */
    private int boundedLevenshtein(String a, String b, int max) {
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        int la = a.length();
        int lb = b.length();
        if (Math.abs(la - lb) > max) {
            return max + 1;
        }

        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            int rowMin = curr[0];
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= lb; j++) {
                int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
                if (curr[j] < rowMin) {
                    rowMin = curr[j];
                }
            }
            if (rowMin > max) {
                return max + 1;
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[lb];
    }

    public int getTotalDuplicateGroups() {
        return duplicateGroups != null ? duplicateGroups.size() : 0;
    }

    // Getters / setters
    public boolean isDetectByCode() {
        return detectByCode;
    }

    public void setDetectByCode(boolean detectByCode) {
        this.detectByCode = detectByCode;
    }

    public boolean isDetectByBarcode() {
        return detectByBarcode;
    }

    public void setDetectByBarcode(boolean detectByBarcode) {
        this.detectByBarcode = detectByBarcode;
    }

    public boolean isDetectByName() {
        return detectByName;
    }

    public void setDetectByName(boolean detectByName) {
        this.detectByName = detectByName;
    }

    public boolean isDetectByFuzzyName() {
        return detectByFuzzyName;
    }

    public void setDetectByFuzzyName(boolean detectByFuzzyName) {
        this.detectByFuzzyName = detectByFuzzyName;
    }

    public int getFuzzyMaxDistance() {
        return fuzzyMaxDistance;
    }

    public void setFuzzyMaxDistance(int fuzzyMaxDistance) {
        this.fuzzyMaxDistance = fuzzyMaxDistance;
    }

    public List<DuplicateItemGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public boolean isSearched() {
        return searched;
    }
}
