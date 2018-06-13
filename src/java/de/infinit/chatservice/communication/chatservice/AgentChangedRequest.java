package de.infinit.chatservice.communication.chatservice;

import de.infinit.chatservice.communication.beans.Agent;
import de.infinit.chatservice.communication.beans.ChatMessage;
import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class AgentChangedRequest {
    private String userId;
    private Agent agent;
    private LinkedList<ChatMessage> messages;

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

    public LinkedList<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(LinkedList<ChatMessage> messages) {
        this.messages = messages;
    }
    
    
}
