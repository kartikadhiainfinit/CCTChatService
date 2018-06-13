/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication;

import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.communication.beans.ChatCategory;
import de.infinit.chatservice.beans.ChatServiceDef;
import de.infinit.chatservice.communication.beans.ChatServiceAvailability;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import de.infinit.chatservice.configuration.beans.ServiceConfig;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.utils.OpeningHoursHelper;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author xxbamar
 */
public class ResponseFactory {

    public static LinkedList<ChatServiceAvailability> getServiceAvailabilities(LinkedList<ChatServiceDef> requestedServices) throws RequestFailedException, BackendUnavailableException {
        ApplicationOptions options = ChatService.getApplicationOptions();
        Integer estimatedWaitTimeInSecs = 0;

        int numberOfChatsAvailable = ChatService.getStats().getNumberOfChatsAvailable();
        LinkedList<ChatServiceAvailability> avails = new LinkedList();

        if (requestedServices != null && !requestedServices.isEmpty()) {
            // Get avail for requested services
            for (ChatServiceDef serviceDef : requestedServices) {
                for (ServiceConfig service : options.getServices().values()) {
                    if (service.getBrandId() == serviceDef.getBrandId() && service.getChannel().equals(serviceDef.getChannel())) {
                        ChatServiceAvailability avail = new ChatServiceAvailability();
                        avail.setAvailableSlots(numberOfChatsAvailable);
                        avail.setChatAvailable(service.isAvailable());
                        if (!service.isAvailable()) {
                            avail.setUserMessage(service.getSystemDownMessage());
                        }
                        avail.setEstimatedWaitTimeInSecs(estimatedWaitTimeInSecs);
                        avail.setChannel(service.getChannel());
                        avail.setBrandId(service.getBrandId());
                        avails.add(avail);
                    }
                }
            }
        } else {
            // Get avail for all registered services or all services if registered services are empty
            Collection<ServiceConfig> services = options.getRegisteredServices().isEmpty() ? options.getServices().values() : options.getRegisteredServices();
            for (ServiceConfig service : services) {
                ChatServiceAvailability avail = new ChatServiceAvailability();

                avail.setAvailableSlots(numberOfChatsAvailable);
                avail.setChatAvailable(service.isAvailable());
                if (!service.isAvailable()) {
                    avail.setUserMessage(service.getSystemDownMessage());
                }
                avail.setEstimatedWaitTimeInSecs(estimatedWaitTimeInSecs);
                avail.setChannel(service.getChannel());
                avail.setBrandId(service.getBrandId());
                avails.add(avail);
            }
        }
        return avails;
    }

    public static LinkedList<ChatCategory> getCategories() {
        ApplicationOptions options = ChatService.getApplicationOptions();
        Map<String, CategoryConfig> configuredCategories = options.getCategories();
        LinkedList<ChatCategory> categories = OpeningHoursHelper.evaluateCategoryAvailability(configuredCategories);
//        Date nextOHChangeTime = OpeningHoursHelper.getNextCategoryChangeTime(configuredCategories);

        ChatService.getInstance().startOpeningHourTimer();

        return categories;
    }
}
