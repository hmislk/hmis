package com.divudi.service;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

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

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void pushToUser(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            int status = transactionSynchronizationRegistry.getTransactionStatus();
            if (status == Status.STATUS_ACTIVE) {
                transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == Status.STATUS_COMMITTED) {
                            sendNow(userId);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
            // Fall through to immediate push when no transaction context is available.
        }
        sendNow(userId);
    }

    private void sendNow(Long userId) {
        notifications.send("new_notification", userId);
    }
}
