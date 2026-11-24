/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Runs a block of work in one container transaction, so that every facade and
 * service call inside it commits together or rolls back together.
 *
 * <p>A JSF bean has no transaction of its own, so each facade call it makes
 * commits on its own. A save that writes several rows (bill, bill items, stock)
 * can then fail half way and leave a partial save. Passing the save to
 * {@link #run(Runnable)} makes it all or nothing: any runtime exception thrown
 * inside rolls back everything, and reaches the caller wrapped in an
 * {@code EJBException}.</p>
 *
 * <p>Inside the block:</p>
 * <ul>
 * <li>Use the {@code ...InNewTransaction} bill number methods, so the number
 * counter commits at once and a concurrent save cannot read the old value.</li>
 * <li>New entities get their ids only on flush; {@link #run(Runnable)} does not
 * flush, so read ids after it returns, or flush inside.</li>
 * <li>Methods marked {@code REQUIRES_NEW} still commit on their own and are not
 * rolled back.</li>
 * </ul>
 *
 * Issue #26021.
 */
@Stateless
public class SingleTransactionRunner {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void run(Runnable work) {
        work.run();
    }
}
