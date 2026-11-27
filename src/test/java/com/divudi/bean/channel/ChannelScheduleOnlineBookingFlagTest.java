package com.divudi.bean.channel;

import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.SessionInstanceFacade;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelScheduleOnlineBookingFlagTest {

    private static class RecordingFacade extends SessionInstanceFacade {
        final List<SessionInstance> edited = new ArrayList<>();

        @Override
        public void edit(SessionInstance t) {
            edited.add(t);
        }
    }

    private static class TestController extends ChannelScheduleController {
        final List<SessionInstance> instances = new ArrayList<>();

        @Override
        public List<SessionInstance> fetchCreatedSessionsInstances(ServiceSession ss) {
            return instances;
        }
    }

    private static SessionInstance existingInstance(boolean online) {
        SessionInstance si = new SessionInstance();
        si.setId(1L);
        si.setAcceptOnlineBookings(online);
        return si;
    }

    private static ServiceSession schedule(boolean online) {
        ServiceSession ss = new ServiceSession();
        ss.setAcceptOnlineBookings(online);
        return ss;
    }

    @Test
    public void unrelatedSave_keepsHandClosedSessionClosed() {
        TestController c = new TestController();
        c.setSessionInstanceFacade(new RecordingFacade());
        SessionInstance closedByHand = existingInstance(false);
        c.instances.add(closedByHand);

        c.updateCreatedServicesesions(schedule(true), Boolean.TRUE);

        assertFalse(closedByHand.isAcceptOnlineBookings());
    }

    @Test
    public void unknownPreviousValue_changesNothing() {
        TestController c = new TestController();
        c.setSessionInstanceFacade(new RecordingFacade());
        SessionInstance closed = existingInstance(false);
        SessionInstance open = existingInstance(true);
        c.instances.add(closed);
        c.instances.add(open);

        c.updateCreatedServicesesions(schedule(true), null);
        assertFalse(closed.isAcceptOnlineBookings());

        c.updateCreatedServicesesions(schedule(false), null);
        assertTrue(open.isAcceptOnlineBookings());
    }

    @Test
    public void turnedOn_opensNormalSessionsAndSkipsStartedCompletedCancelledHoliday() {
        TestController c = new TestController();
        RecordingFacade f = new RecordingFacade();
        c.setSessionInstanceFacade(f);
        SessionInstance normal = existingInstance(false);
        SessionInstance started = existingInstance(false);
        started.setStarted(true);
        SessionInstance completed = existingInstance(false);
        completed.setCompleted(true);
        SessionInstance cancelled = existingInstance(false);
        cancelled.setCancelled(true);
        SessionInstance holiday = existingInstance(false);
        holiday.setDoctorHoliday(true);
        c.instances.add(normal);
        c.instances.add(started);
        c.instances.add(completed);
        c.instances.add(cancelled);
        c.instances.add(holiday);

        c.updateCreatedServicesesions(schedule(true), Boolean.FALSE);

        assertTrue(normal.isAcceptOnlineBookings());
        assertFalse(started.isAcceptOnlineBookings());
        assertFalse(completed.isAcceptOnlineBookings());
        assertFalse(cancelled.isAcceptOnlineBookings());
        assertFalse(holiday.isAcceptOnlineBookings());
        assertEquals(5, f.edited.size());
    }

    @Test
    public void turnedOff_closesAllFutureSessions() {
        TestController c = new TestController();
        c.setSessionInstanceFacade(new RecordingFacade());
        SessionInstance normal = existingInstance(true);
        SessionInstance started = existingInstance(true);
        started.setStarted(true);
        SessionInstance holiday = existingInstance(true);
        holiday.setDoctorHoliday(true);
        c.instances.add(normal);
        c.instances.add(started);
        c.instances.add(holiday);

        c.updateCreatedServicesesions(schedule(false), Boolean.TRUE);

        assertFalse(normal.isAcceptOnlineBookings());
        assertFalse(started.isAcceptOnlineBookings());
        assertFalse(holiday.isAcceptOnlineBookings());
    }
}
