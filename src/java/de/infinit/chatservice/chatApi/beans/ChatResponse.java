/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.chatApi.beans;

import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class ChatResponse {

    private LinkedList<ChatResponseMessage> messages = new LinkedList();
    private boolean chatEnded;
    private int statusCode;
    private String alias; // deprecated
    private String secureKey;
    private String userId; // deprecated
    private String chatId; // deprecated
    private int nextPosition;
    private String error;
    private int referenceId;
    private Object[] errors;

    public LinkedList<ChatResponseMessage> getMessages() {
        return messages;
    }

    public void setMessages(LinkedList<ChatResponseMessage> messages) {
        this.messages = messages;
    }

    public boolean isChatEnded() {
        return chatEnded;
    }

    public void setChatEnded(boolean chatEnded) {
        this.chatEnded = chatEnded;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSecureKey() {
        return secureKey;
    }

    public void setSecureKey(String secureKey) {
        this.secureKey = secureKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public int getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(int nextPosition) {
        this.nextPosition = nextPosition;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = referenceId;
    }

    public Object[] getErrors() {
        return errors;
    }

    public void setErrors(Object[] errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        StringBuilder m = new StringBuilder("[");
        for (ChatResponseMessage message : messages) {
            m.append(message.toString()).append(",");
        }
        m.append("]");
        return "{chatEnded: " + chatEnded + ", statusCode: " + statusCode + ", messages: " + m.toString() + ", secureKey: " + secureKey + ", nextPosition: " + nextPosition + ", error: " + error + ", referenceId: " + referenceId + "}";
    }
}
