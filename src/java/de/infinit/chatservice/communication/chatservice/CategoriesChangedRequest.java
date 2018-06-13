/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.chatservice;
import de.infinit.chatservice.communication.beans.ChatCategory;
import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class CategoriesChangedRequest {
      private LinkedList<ChatCategory> chatCategories; 

    public LinkedList<ChatCategory> getChatCategories() {
        return chatCategories;
    }

    public void setChatCategories(LinkedList<ChatCategory> chatCategories) {
        this.chatCategories = chatCategories;
    }
      
      
}
