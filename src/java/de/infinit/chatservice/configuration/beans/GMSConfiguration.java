package de.infinit.chatservice.configuration.beans;

public class GMSConfiguration {

    private String gmsPrimaryNodeName;
    private String gmsUser;
    private String gmsPassword;
    private String gmsChatService;

    public String getGmsPrimaryNodeName() {
        return gmsPrimaryNodeName;
    }

    public void setGmsPrimaryNodeName(String gmsPrimaryNodeName) {
        this.gmsPrimaryNodeName = gmsPrimaryNodeName;
    }

    public String getGmsUser() {
        return gmsUser;
    }

    public void setGmsUser(String gmsUser) {
        this.gmsUser = gmsUser;
    }

    public String getGmsPassword() {
        return gmsPassword;
    }

    public void setGmsPassword(String gmsPassword) {
        this.gmsPassword = gmsPassword;
    }

    public String getGmsChatService() {
        return gmsChatService;
    }

    public void setGmsChatService(String gmsChatService) {
        this.gmsChatService = gmsChatService;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("GMSConfiguration [gmsPrimaryNodeName=").append(gmsPrimaryNodeName);
        builder.append(", gmsUser=").append(gmsUser);
        builder.append(", gmsPassword=").append("[output suppressed]");
        builder.append(", gmsChatService=").append(gmsChatService);
        builder.append("]");
        return builder.toString();
    }

}
