package com.divudi.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Chinthaka Prasad
 */
@ServerEndpoint(value = "/ws/notify")
public class WebSocketService {

    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static final Logger LOGGER = Logger.getLogger(WebSocketService.class.getName());

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.info("session is created - " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("session is closed - " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.info("error occured from -" + session.getId() + " " + throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("message recieved from " + session.getId() + " message - " + message);
    }

    public static void broadcastToSessions(String message) {
        if (sessions.isEmpty()) {
            LOGGER.info("no ws sessions");
            return;
        }

        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    LOGGER.info("Error when broadcasting - " + e.getMessage());
                }
            }
        }
    }

}
