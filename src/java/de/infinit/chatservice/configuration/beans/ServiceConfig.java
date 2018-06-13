/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.configuration.beans;

/**
 *
 * @author xxbamar
 */
public class ServiceConfig{
    private String sectionName;
    private boolean available;
    private int brandId;
    private String channel;
    private String systemDownMessage;

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSystemDownMessage() {
        return systemDownMessage;
    }

    public void setSystemDownMessage(String systemDownMessage) {
        this.systemDownMessage = systemDownMessage;
    }
}
