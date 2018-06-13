/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.client;

import de.infinit.chatservice.beans.ChatServiceDef;
import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class ClientServiceAvailabilityRequest {
    private LinkedList<ChatServiceDef> chatServices = new LinkedList<>();

    public LinkedList<ChatServiceDef> getChatServices() {
        return chatServices;
    }

    public void setChatServices(LinkedList<ChatServiceDef> chatServices) {
        this.chatServices = chatServices;
    }
    
    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("ChatChannels = [");
        for(ChatServiceDef chatService : chatServices){
            result.append("{").append(chatService.toString()).append("}").append(",");
        }
        
        return result.toString();
    }
}
