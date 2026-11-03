package com.divudi.core.util;

import com.divudi.core.entity.channel.SessionInstance;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JVM-wide striped lock that serialises serial-number allocation and the
 * insert of the booking row for one channel session (issue #24785).
 *
 * Without it, two concurrent bookings can both read the same "next free"
 * serial number before either row is saved, and both receive it.
 *
 * Hold it from the moment the serial number is chosen until the BillSession
 * row has been committed. Callers that run outside a container transaction
 * (JSF controllers) commit on each facade call, so wrapping the allocate and
 * create calls is enough. A caller that runs inside one transaction must take
 * the lock before that transaction starts.
 *
 * Only correct while the application runs as a single server instance.
 */
public final class SessionBookingLock {

    private static final ReentrantLock[] LOCKS = new ReentrantLock[64];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new ReentrantLock();
        }
    }

    private SessionBookingLock() {
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

    public static Hold acquire(SessionInstance sessionInstance) {
        Long id = sessionInstance == null ? null : sessionInstance.getId();
        return acquire(id);
    }

    public static Hold acquire(Long sessionInstanceId) {
        long key = sessionInstanceId == null ? 0L : sessionInstanceId;
        ReentrantLock lock = LOCKS[(int) Math.floorMod(key, (long) LOCKS.length)];
        lock.lock();
        return new Hold(lock);
    }
}
