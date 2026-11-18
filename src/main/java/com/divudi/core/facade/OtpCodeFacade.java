package com.divudi.core.facade;

import com.divudi.core.data.OtpChannel;
import com.divudi.core.data.OtpPurpose;
import com.divudi.core.entity.OtpCode;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;

/**
 * Queries for one-time codes. Time windows are bound as TIMESTAMP (the
 * generic AbstractFacade helpers bind Date parameters as DATE, which drops
 * the time of day).
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class OtpCodeFacade extends AbstractFacade<OtpCode> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OtpCodeFacade() {
        super(OtpCode.class);
    }

    /**
     * Loads the code with a row lock (SELECT ... FOR UPDATE) so two
     * simultaneous verifications of the same code cannot both succeed. Must
     * be called inside the caller's transaction.
     */
    public OtpCode findForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(OtpCode.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Long findLatestActiveId(Long webUserId, OtpPurpose purpose) {
        List<Long> ids = em.createQuery("select o.id from OtpCode o"
                + " where o.webUser.id=:uid and o.purpose=:p"
                + " and o.consumedAt is null and o.invalidatedAt is null"
                + " order by o.id desc", Long.class)
                .setParameter("uid", webUserId)
                .setParameter("p", purpose)
                .setMaxResults(1)
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    public List<OtpCode> findActive(Long webUserId, OtpPurpose purpose) {
        return em.createQuery("select o from OtpCode o"
                + " where o.webUser.id=:uid and o.purpose=:p"
                + " and o.consumedAt is null and o.invalidatedAt is null", OtpCode.class)
                .setParameter("uid", webUserId)
                .setParameter("p", purpose)
                .getResultList();
    }

    /**
     * Creation times of the user's codes (any purpose) since the given time,
     * oldest first.
     */
    public List<Date> findCreatedTimesSince(Long webUserId, Date since) {
        return em.createQuery("select o.createdAt from OtpCode o"
                + " where o.webUser.id=:uid and o.createdAt>=:since"
                + " order by o.createdAt", Date.class)
                .setParameter("uid", webUserId)
                .setParameter("since", since, TemporalType.TIMESTAMP)
                .getResultList();
    }

    public Long countByChannelSince(OtpChannel channel, Date since) {
        return em.createQuery("select count(o) from OtpCode o"
                + " where o.channel=:c and o.createdAt>=:since", Long.class)
                .setParameter("c", channel)
                .setParameter("since", since, TemporalType.TIMESTAMP)
                .getSingleResult();
    }

    public int deleteCreatedBefore(Date before) {
        return em.createQuery("delete from OtpCode o where o.createdAt < :before")
                .setParameter("before", before, TemporalType.TIMESTAMP)
                .executeUpdate();
    }
}
