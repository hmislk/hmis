package com.divudi.core.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * JVM-wide striped lock that serialises the find-or-create of a channel
 * SessionInstance for one ServiceSession (issue #24972).
 *
 * Without it, two users opening the same doctor at the same moment, before
 * the day's instance exists, can both find nothing and both create one, which
 * splits that day's bookings across two instances.
 *
 * Hold it around a call that finds and creates the instance in its own
 * (REQUIRES_NEW) transaction, so the new row is committed before the lock is
 * released and the next holder finds it.
 *
 * It only reduces contention inside one server: two applications that share
 * a database are serialised by the row lock that findOrCreateSessionInstance
 * takes on the service session.
 */
public final class SessionInstanceCreationLock {

    private static final ReentrantLock[] LOCKS = new ReentrantLock[64];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new ReentrantLock();
        }
    }

    private SessionInstanceCreationLock() {
    }

    public static final class Hold implements AutoCloseable {

        private final ReentrantLock lock;

        private Hold(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    public static Hold acquire(Long serviceSessionId) {
        long key = serviceSessionId == null ? 0L : serviceSessionId;
        ReentrantLock lock = LOCKS[(int) Math.floorMod(key, (long) LOCKS.length)];
        lock.lock();
        return new Hold(lock);
    }
}
