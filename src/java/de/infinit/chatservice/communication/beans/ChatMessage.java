/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.beans;

/**
 *
 * @author xxbamar
 */
public class ChatMessage {

    public String text;
    public Long sentTimeMillis;
    private String source; // System, Agent, Client

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getSentTimeMillis() {
        return sentTimeMillis;
    }

    public void setSentTimeMillis(Long sentTimeMillis) {
        this.sentTimeMillis = sentTimeMillis;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ChatMessage [text=").append(text);
        builder.append(", sentTimeMillis=").append(sentTimeMillis);
        builder.append(", source=").append(source);
        builder.append("]");
        return builder.toString();
    }

}
