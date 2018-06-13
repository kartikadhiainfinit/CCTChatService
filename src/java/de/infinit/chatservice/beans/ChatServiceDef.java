/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.beans;

/**
 *
 * @author xxbamar
 */
public class ChatServiceDef {
    private String channel;
    private int brandId;
    
    public ChatServiceDef(){};
    public ChatServiceDef(String channel, int brandId){
        this.channel = channel;
        this.brandId = brandId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }
    
    @Override
    public String toString(){
        return "Channel = " + channel + ", BrandId = " + brandId;
    }
}
