/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.configuration.beans;

import com.genesyslab.platform.applicationblocks.com.objects.CfgStatDay;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author xxbamar
 */
public class CategoryConfig {
    private String sectionName;
    private String displayName;
    private String id;
    private String openingTimetable;
    private boolean enableQueuingMessage;
    
    private String queuingMessage;
    private String routingService;
    
    private Integer openingTimetableDBID;
    private List<CfgStatDay> openingHours = new ArrayList<>();

    public boolean isEnableQueuingMessage() {
        return enableQueuingMessage;
    }

    public void setEnableQueuingMessage(boolean enableQueuingMessage) {
        this.enableQueuingMessage = enableQueuingMessage;
    }
    

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOpeningTimetable() {
        return openingTimetable;
    }

    public void setOpeningTimetable(String openingTimetable) {
        this.openingTimetable = openingTimetable;
    }

    public String getQueuingMessage() {
        return queuingMessage;
    }

    public void setQueuingMessage(String queuingMessage) {
        this.queuingMessage = queuingMessage;
    }

    public String getRoutingService() {
        return routingService;
    }

    public void setRoutingService(String routingService) {
        this.routingService = routingService;
    }

    public Integer getOpeningTimetableDBID() {
        return openingTimetableDBID;
    }

    public void setOpeningTimetableDBID(Integer openingTimetableDBID) {
        this.openingTimetableDBID = openingTimetableDBID;
    }

    public List<CfgStatDay> getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(List<CfgStatDay> openingHours) {
        this.openingHours = openingHours;
    }
}
