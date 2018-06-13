/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.chatservice;

import de.infinit.chatservice.communication.beans.ChatServiceAvailability;
import java.util.LinkedList;

/**
 *
 * @author xxbamar
 */
public class ServiceAvailabilityChangedRequest {
    LinkedList<ChatServiceAvailability> serviceAvailabilities = new LinkedList<>();

    public LinkedList<ChatServiceAvailability> getServiceAvailabilities() {
        return serviceAvailabilities;
    }

    public void setServiceAvailabilities(LinkedList<ChatServiceAvailability> serviceAvailabilities) {
        this.serviceAvailabilities = serviceAvailabilities;
    }
}
