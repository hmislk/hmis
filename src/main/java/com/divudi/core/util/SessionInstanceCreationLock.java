package com.divudi.core.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * JVM-wide striped lock that serialises the find-or-create of the
 * SessionInstance for one ServiceSession (issue #24972).
 *
 * Without it, two users opening the same doctor before the day's instance
 * exists can both find nothing and both create one, splitting the bookings
 * between two instances of the same session.
 *
 * Hold it around a find-or-create that commits in its own transaction
 * (SessionInstanceCreationService), so the next holder sees the new row.
 * Uses its own stripes, separate from SessionBookingLock, so the two locks
 * never wait on each other.
 *
 * Only correct while the application runs as a single server instance.
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
