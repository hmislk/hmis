package com.divudi.service;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Application-scoped service that pushes a lightweight signal via JSF WebSocket
 * to a specific user's browser session(s) when new notifications are created.
 *
 * Must be @ApplicationScoped so it can push across HTTP sessions — the sender
 * (user creating a bill) is in a different session from the recipient.
 */
@Named
@ApplicationScoped
public class NotificationPushService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @Push
    private PushContext notifications;

    public void pushToUser(Long userId) {
        if (userId == null) {
            return;
        }
        notifications.send("new_notification", userId);
    }
}
