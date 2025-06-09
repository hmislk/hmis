package com.divudi.core.data.channel;

import com.divudi.core.entity.channel.SessionInstance;
import org.primefaces.model.DefaultScheduleEvent;

public class SessionInstanceEvent extends DefaultScheduleEvent<SessionInstance> {

    private SessionInstance sessionInstance;

    public SessionInstanceEvent() {
    }

    // Getter and Setter for SessionInstance
    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    // Builder Class
    public static final class Builder {
        private SessionInstance sessionInstance;

        public Builder() {
        }

        public Builder sessionInstance(SessionInstance sessionInstance) {
            this.sessionInstance = sessionInstance;
            return this;
        }

        public SessionInstanceEvent build() {
            SessionInstanceEvent event = new SessionInstanceEvent();
            event.setSessionInstance(sessionInstance);
            return event;
        }
    }
}
