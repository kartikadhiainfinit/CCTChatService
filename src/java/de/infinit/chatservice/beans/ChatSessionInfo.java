package de.infinit.chatservice.beans;

import java.util.Date;
import java.util.LinkedList;

public class ChatSessionInfo {

    private String sessionId;
    private String userId;
    private String secureKey;
    private int nextPosition;
    private Date modifiedTime;
    private StatusEnum status;
    private String disconnectReason;
    private String gmsUserId;
    private String gmsChatId;
    private String gmsAlias;
    private LinkedList<String> chatServiceInstances = new LinkedList<>();

    public ChatSessionInfo() {
        nextPosition = 0;
        status = StatusEnum.OPEN;
    }

    public String getGmsAlias() {
        return gmsAlias;
    }

    public void setGmsAlias(String gmsAlias) {
        this.gmsAlias = gmsAlias;
    }    

    public String getGmsUserId() {
        return gmsUserId;
    }

    public void setGmsUserId(String gmsUserId) {
        this.gmsUserId = gmsUserId;
    }

    public String getGmsChatId() {
        return gmsChatId;
    }

    public void setGmsChatId(String gmsChatId) {
        this.gmsChatId = gmsChatId;
    }
    
    

    /**
     * @return the chatId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @param chatSessionId the chatId to set
     */
    public void setSessionId(final String chatSessionId) {
        this.sessionId = chatSessionId;
    }

    /**
     * @return the secureKey
     */
    public String getSecureKey() {
        return secureKey;
    }

    /**
     * @param secureKey the secureKey to set
     */
    public void setSecureKey(final String secureKey) {
        this.secureKey = secureKey;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * @param nextPosition
     */
    public void setNextPosition(final int nextPosition) {
        this.nextPosition = nextPosition;
    }

    /**
     * @return the nextPosition
     */
    public int getNextPosition() {
        return nextPosition;
    }

    /**
     * @param modifiedTime the modifiedTime to set
     */
    public void setModifiedTime(final Date modifiedTime) {
        this.modifiedTime = new Date(modifiedTime.getTime());
    }

    /**
     * @return the modifiedTime
     */
    public Date getModifiedTime() {
        return this.modifiedTime;
    }

    /**
     * @return the status
     */
    public StatusEnum getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(final StatusEnum status) {
        this.status = status;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
    }

    public LinkedList<String> getChatServiceInstances() {
        return chatServiceInstances;
    }
    
    public boolean isChatServiceInstancesListEmpty() {
        return this.chatServiceInstances == null || this.chatServiceInstances.isEmpty();
    }

    public void setChatServiceInstances(final LinkedList<String> chatServiceInstances) {
        this.chatServiceInstances = chatServiceInstances;
    }

    
    public void addPrimaryChatService(String chatServiceName){
        chatServiceInstances.add(chatServiceName);
    }
    
    public String getPrimaryChatService(){
        return chatServiceInstances.getLast();
    }
    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ChatSessionInfo [sessionId=").append(sessionId);
        builder.append(", userId=").append(userId);
        builder.append(", secureKey=").append(secureKey);
        builder.append(", lastIndex=").append(nextPosition);
        builder.append(", modifiedTime=").append(modifiedTime);
        builder.append(", status=").append(status);
        builder.append(", chatServiceInstances=").append(chatServiceInstances);
        builder.append("]");
        return builder.toString();
    }

}
