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
public class ChatCategory {

    private String id;
    private String name;
    private boolean isAvailable;

    public ChatCategory() {
    }

    public ChatCategory(String id, String name, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.isAvailable = isAvailable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
    
    
}
