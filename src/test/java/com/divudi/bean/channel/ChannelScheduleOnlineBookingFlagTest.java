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

    @Test
    public void savingScheduleWithOnlineBookingsOn_opensExistingSessions() {
        TestController c = new TestController();
        RecordingFacade f = new RecordingFacade();
        c.setSessionInstanceFacade(f);
        SessionInstance si = existingInstance(false);
        c.instances.add(si);
        ServiceSession ss = new ServiceSession();
        ss.setAcceptOnlineBookings(true);

        c.updateCreatedServicesesions(ss);

        assertTrue(si.isAcceptOnlineBookings());
        assertEquals(1, f.edited.size());
    }

    @Test
    public void savingScheduleWithOnlineBookingsOff_closesExistingSessions() {
        TestController c = new TestController();
        RecordingFacade f = new RecordingFacade();
        c.setSessionInstanceFacade(f);
        SessionInstance si = existingInstance(true);
        c.instances.add(si);
        ServiceSession ss = new ServiceSession();
        ss.setAcceptOnlineBookings(false);

        c.updateCreatedServicesesions(ss);

        assertFalse(si.isAcceptOnlineBookings());
    }
}
