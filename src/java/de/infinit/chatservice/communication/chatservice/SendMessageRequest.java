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
public class SendMessageRequest {
    private String userId;
    private LinkedList<ChatMessage> chatMessages = new LinkedList<>();

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
    
    @Override
    public String toString() {
        StringBuilder messages = new StringBuilder();
        for(int i = 0; i< chatMessages.size() ; i++){
            messages.append(chatMessages.get(i));
            if(i< chatMessages.size() - 1){
                messages.append(",");
            }
        }
        
        final StringBuilder builder = new StringBuilder();
        builder.append("SendMessageRequest [ userId=").append(userId);
        builder.append(", chatMessages=").append(messages.toString());
        
        builder.append("]");
        return builder.toString();
    }
}
