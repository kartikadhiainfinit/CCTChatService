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
public class ChatResponseMessageFrom {
    private String nickname;
    private int participantId;
    private String type; // Agent, Client

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String toString(){
        return "{nickname: "+nickname+", participantId: "+participantId+", type: "+type+"}";
    }
}
