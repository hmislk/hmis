/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.data;

/**
 *
 * @author buddhika
 */
public class SmsSentResponse {
    private boolean sentSuccefully;
    private String receivedMessage;

    public boolean isSentSuccefully() {
        return sentSuccefully;
    }

    public void setSentSuccefully(boolean sentSuccefully) {
        this.sentSuccefully = sentSuccefully;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }

    public void setReceivedMessage(String receivedMessage) {
        this.receivedMessage = receivedMessage;
    }
    
}
