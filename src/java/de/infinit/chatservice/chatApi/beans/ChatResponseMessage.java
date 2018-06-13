/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.chatApi.beans;

/**
 *
 * @author xxbamar
 */
public class ChatResponseMessage {

    private ChatResponseMessageFrom from;
    private int index;
    private String messageType;
    private String text;
    private String type; // ParticipantJoined, Message, ParticipantLeft
    private long utcTime;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }    

    public ChatResponseMessageFrom getFrom() {
        return from;
    }

    public void setFrom(ChatResponseMessageFrom from) {
        this.from = from;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(long utcTime) {
        this.utcTime = utcTime;
    }

    @Override
    public String toString() {
        return "{index: " + index + ", text: " + text + ", type: " + type + ", utcTime: " + utcTime + ",from: " + from + "}";
    }
}
