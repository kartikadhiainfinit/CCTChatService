package de.infinit.chatservice.communication.beans;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author xxbamar
 */
public class ChatServiceAvailability {
    public boolean chatAvailable;
    public Integer availableSlots;
    public Integer estimatedWaitTimeInSecs;
    public String userMessage;
    public String channel;
    public Integer brandId;

    public boolean isChatAvailable() {
        return chatAvailable;
    }

    public void setChatAvailable(boolean chatAvailable) {
        this.chatAvailable = chatAvailable;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Integer getEstimatedWaitTimeInSecs() {
        return estimatedWaitTimeInSecs;
    }

    public void setEstimatedWaitTimeInSecs(Integer estimatedWaitTimeInSecs) {
        this.estimatedWaitTimeInSecs = estimatedWaitTimeInSecs;
    }

    public String getUserMessaage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getBrandId() {
        return brandId;
    }

    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
    }
    
    @Override
    public String toString(){
        return "chatAvailable: " + chatAvailable + ", " +
            "availableSlots: " + availableSlots + ", " +
            "estimatedWaitTimeInSecs: " + estimatedWaitTimeInSecs + ", " +
            "userMessaage: " + userMessage;
    }
}
