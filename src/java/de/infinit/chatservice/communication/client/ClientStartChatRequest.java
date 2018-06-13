/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication.client;

/**
 *
 * @author xxbamar
 */
public class ClientStartChatRequest {
    private String sessionId;
    private String userId;
    private String firstName;
    private String lastName;
    private String nickname;
    private String salutation;
    private String msisdn;
    private String categoryId;
    private String email;
    private String channel;
    private int brandId;
    private boolean pkkCheck; 

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String supportCategoryId) {
        this.categoryId = supportCategoryId;
    }

    public boolean getPkkCheck() {
        return pkkCheck;
    }

    public void setPkkCheck(boolean pkkCheck) {
        this.pkkCheck = pkkCheck;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientStartChatRequest [sessionId=").append(sessionId);
        builder.append(", userId=").append(userId);
        builder.append(", firstName=").append(firstName);
        builder.append(", lastName=").append(lastName);
        builder.append(", nickname=").append(nickname);
        builder.append(", salutation=").append(salutation);
        builder.append(", msisdn=").append(msisdn);
        builder.append(", categoryId=").append(categoryId);
        builder.append(", email=").append(email);
        builder.append(", pkkCheck=").append(pkkCheck);
        builder.append("]");
        return builder.toString();
    }
}
