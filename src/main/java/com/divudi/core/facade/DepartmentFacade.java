/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;

/**
 *
 * @author buddhika
 */
@Stateless
public class DepartmentFacade extends AbstractFacade<Department> {

    private static final Logger LOGGER = Logger.getLogger(DepartmentFacade.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public DepartmentFacade() {
        super(Department.class);
    }

    /**
     * Departments that most recently received bills of the given type from the
     * given department, newest first, limited to bills created in the last
     * {@code days} days. Retired and inactive departments are skipped. Never
     * throws: on failure it logs a warning and returns an empty list.
     *
     * Meant for getters that run while a page renders, so it must be fast. Bill
     * is large and has no index on (billTypeAtomic, fromDepartment), so an
     * unbounded scan, or a join to Department that makes MySQL drive from the
     * department side, can take many seconds. The scan is bounded by createdAt,
     * grouped on the department id column of Bill only, and the departments are
     * loaded afterwards. Grouping with an aggregate ORDER BY also stays valid
     * under ONLY_FULL_GROUP_BY (no DISTINCT + ORDER BY on a column outside the
     * select list).
     */
    public List<Department> findRecentToDepartments(BillTypeAtomic billTypeAtomic, Department fromDepartment, int days, int limit) {
        List<Department> recent = new ArrayList<>();
        try {
            String jpql = "select b.toDepartment.id, max(b.id) from Bill b "
                    + "where b.retired=false "
                    + "and b.billTypeAtomic=:bt "
                    + "and b.fromDepartment=:fd "
                    + "and b.createdAt > :since "
                    + "group by b.toDepartment.id "
                    + "order by max(b.id) desc";
            Map<String, Object> m = new HashMap<>();
            m.put("bt", billTypeAtomic);
            m.put("fd", fromDepartment);
            Calendar since = Calendar.getInstance();
            since.add(Calendar.DAY_OF_YEAR, -days);
            m.put("since", since.getTime());
            // Fetch extra ids: retired or inactive departments are skipped below.
            List<Object[]> rows = findObjectsArrayByJpql(jpql, m, TemporalType.TIMESTAMP, limit * 2);
            for (Object[] row : rows) {
                if (recent.size() >= limit) {
                    break;
                }
                if (row[0] == null) {
                    continue;
                }
                Department d = find((Long) row[0]);
                if (d != null && !d.isRetired() && !d.isInactive()) {
                    recent.add(d);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load recent departments for " + billTypeAtomic, e);
        }
        return recent;
    }

}
