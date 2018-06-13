package de.infinit.chatservice.configuration.beans;

import de.infinit.chatservice.configuration.ConfigConstants;

public class AlertsConfiguration {

    private int cctChatServiceNotAvailable = ConfigConstants.ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE_DEFAULT;
    private int primaryGmsNotAvailable = ConfigConstants.ALERTS_PRIMARY_GMS_NOT_AVAILABLE_DEFAULT;
    private int primaryGmsReconnect = ConfigConstants.ALERTS_PRIMARY_GMS_RECONNECT_DEFAULT;
    private int gmsNodesNotAvailable = ConfigConstants.ALERTS_GMS_NODES_NOT_AVAILABLE_DEFAULT;
    private int gmsNodesReconnect = ConfigConstants.ALERTS_GMS_NODES_RECONNECT_DEFAULT;
    
    private Integer cmeNotAvailable = ConfigConstants.ALERTS_CME_NOT_AVAILABLE_DEFAULT;
    private Integer cmeReconnect = ConfigConstants.ALERTS_CME_RECONNECT_DEFAULT;
    

    public int getChatServiceNotAvailable() {
        return cctChatServiceNotAvailable;
    }

    public void setChatServiceNotAvailable(int cctChatServiceNotAvailable) {
        this.cctChatServiceNotAvailable = cctChatServiceNotAvailable;
    }

    public int getPrimaryGmsNotAvailable() {
        return primaryGmsNotAvailable;
    }

    public void setPrimaryGmsNotAvailable(int primaryGmsNotAvailable) {
        this.primaryGmsNotAvailable = primaryGmsNotAvailable;
    }

    public int getPrimaryGmsReconnect() {
        return primaryGmsReconnect;
    }

    public void setPrimaryGmsReconnect(int primaryGmsReconnect) {
        this.primaryGmsReconnect = primaryGmsReconnect;
    }

    public int getGmsNodesNotAvailable() {
        return gmsNodesNotAvailable;
    }

    public void setGmsNodesNotAvailable(int gmsNodesNotAvailable) {
        this.gmsNodesNotAvailable = gmsNodesNotAvailable;
    }

    public int getGmsNodesReconnect() {
        return gmsNodesReconnect;
    }

    public void setGmsNodesReconnect(int gmsNodesReconnect) {
        this.gmsNodesReconnect = gmsNodesReconnect;
    }

    public Integer getCmeNotAvailable() {
        return cmeNotAvailable;
    }

    public void setCmeNotAvailable(Integer cmeNotAvailable) {
        this.cmeNotAvailable = cmeNotAvailable;
    }

    public Integer getCmeReconnect() {
        return cmeReconnect;
    }

    public void setCmeReconnect(Integer cmeReconnect) {
        this.cmeReconnect = cmeReconnect;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AlertsConfiguration [cctChatServiceNotAvailable=").append(cctChatServiceNotAvailable);
        builder.append(", primaryGmsNotAvailable=").append(primaryGmsNotAvailable);
        builder.append(", primaryGmsReconnect=").append(primaryGmsReconnect);
        builder.append(", gmsNodesNotAvailable=").append(gmsNodesNotAvailable);
        builder.append(", gmsNodesReconnect=").append(gmsNodesReconnect);
        builder.append(", cmeNotAvailable=").append(cmeNotAvailable);
        builder.append(", cmeReconnect=").append(cmeReconnect);
        builder.append("]");
        return builder.toString();
    }

}
