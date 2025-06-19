
package com.divudi.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
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
    
    @OnOpen
    public void onOpen(Session session){
       sessions.add(session);
        System.out.println("session is created - "+session.getId());
    }
    
    @OnClose
    public void onClose(Session session){
        sessions.remove(session);
        System.out.println("session is closed - " + session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable){
        System.out.println("error occured from -"+session.getId() +" "+throwable.getMessage());
    }
    
    @OnMessage
    public void onMessage(String message, Session session){
        System.out.println("message recieved from "+session.getId() + " message - "+message);
    }
    
    public static void broadcastToSessions(String message){
        if(sessions.isEmpty()){
            System.out.println("no ws sessions");
            return;
        }
        
        for(Session session : sessions){
            if(session.isOpen()){
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    System.out.println("Error when broadcasting - "+ e.getMessage());
                }
            }
        }
    }
 
}
