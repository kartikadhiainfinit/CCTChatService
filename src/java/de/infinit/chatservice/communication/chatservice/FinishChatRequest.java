/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.chatservice;

import de.infinit.chatservice.communication.beans.ChatMessage;
import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class FinishChatRequest {
    private String userId;
    private LinkedList<ChatMessage> chatMessages = new LinkedList<>();
    private String source; // system, customer

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LinkedList<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(LinkedList<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    
    
}
