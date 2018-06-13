package de.infinit.chatservice.configuration.beans;

import de.infinit.chatservice.configuration.ConfigConstants;

public class CommonConfiguration {

    private String idleChatCleanupTime;
    private int idleTimeoutMin;
    private String chatCategoriesLO;
    private String serviceAvailabilityLO;
    private boolean enabled = true;
    private String chatChannel;
    private boolean sendBackClientMessages;
    private int maxClientMessageLength;
    private int openChatSyncTimeout;
    private int pingClientIntervalMs;
    private int messagePostponeMs = ConfigConstants.COMMON_POSTPONE_MESSAGES_DEFAULT;
    private int brandId = ConfigConstants.COMMON_BRAND_ID_DEFAULT;
    private String channel = ConfigConstants.COMMON_CHANNEL_DEFAULT;
    private String externalNickname = ConfigConstants.COMMON_EXTERNAL_NICKNAME_DEFAULT;

    public int getPingClientIntervalMs() {
        return pingClientIntervalMs;
    }

    public void setPingClientIntervalMs(int pingClientIntervalMs) {
        this.pingClientIntervalMs = pingClientIntervalMs;
    }

    public int getMaxClientMessageLength() {
        return maxClientMessageLength;
    }

    public void setMaxClientMessageLength(int maxClientMessageLength) {
        this.maxClientMessageLength = maxClientMessageLength;
    }

    public int getOpenChatSyncTimeout() {
        return openChatSyncTimeout;
    }

    public void setOpenChatSyncTimeout(int openChatSyncTimeout) {
        this.openChatSyncTimeout = openChatSyncTimeout;
    }
    
    public String getIdleChatCleanupTime() {
        return idleChatCleanupTime;
    }

    public void setIdleChatCleanupTime(String idleChatCleanupTime) {
        this.idleChatCleanupTime = idleChatCleanupTime;
    }

    /**
     * @return the webApiRequestTimeout as Integer. Null is returned if a string value can't be converted to integer
     */
    public Integer getIdleTimeoutMin() {
        if (idleTimeoutMin == 0) {
            return null;
        }

        return idleTimeoutMin;
    }

    public void setIdleTimeoutMin(int idleTimeoutMin) {
        this.idleTimeoutMin = idleTimeoutMin;
    }

    public String getChatCategoriesLOName() {
        return chatCategoriesLO;
    }

    public void setChatCategoriesLOName(String name) {
        this.chatCategoriesLO = name;
    }

    public String getServiceAvailabilityLOName() {
        return serviceAvailabilityLO;
    }

    public void setServiceAvailabilityLOName(String name) {
        this.serviceAvailabilityLO = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChatChannel() {
        return chatChannel;
    }

    public void setChatChannel(String chatChannel) {
        this.chatChannel = chatChannel;
    }

    public boolean isSendBackClientMessages() {
        return sendBackClientMessages;
    }

    public void setSendBackClientMessages(boolean sendBackClientMessages) {
        this.sendBackClientMessages = sendBackClientMessages;
    }

    public int getMessagePostponeMs() {
        return messagePostponeMs;
    }

    public void setMessagePostponeMs(int messagePostponeMs) {
        this.messagePostponeMs = messagePostponeMs;
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

    public String getExternalNickname() {
        return externalNickname;
    }

    public void setExternalNickname(String externalNickname) {
        this.externalNickname = externalNickname;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CommonConfiguration [idleChatCleanupTime=").append(idleChatCleanupTime);
        builder.append(", idleTimeoutMin=").append(idleTimeoutMin);
        builder.append(", LO_ChatCategories=").append(chatCategoriesLO);
        builder.append(", LO_ServiceAvailability=").append(serviceAvailabilityLO);
        builder.append(", enabled=").append(enabled);
        builder.append(", chatChannel=").append(chatChannel);
        builder.append(", sendBackClientMessages=").append(sendBackClientMessages);
        builder.append(", messagePostponeMs=").append(messagePostponeMs);
        builder.append(", channel=").append(channel);
        builder.append(", brandId=").append(brandId);
        builder.append(", externalNickname=").append(externalNickname);
        builder.append("]");
        return builder.toString();
    }

}
