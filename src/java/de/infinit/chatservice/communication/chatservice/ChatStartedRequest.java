/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.chatservice;

import de.infinit.chatservice.communication.beans.Agent;

/**
 *
 * @author xxbamar
 */
public class ChatStartedRequest {
    private String userId;
    private Agent agent; 

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }
    
    
}
