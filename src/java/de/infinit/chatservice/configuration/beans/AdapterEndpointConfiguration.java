package de.infinit.chatservice.configuration.beans;

public class AdapterEndpointConfiguration {

    private String adapterUrl;
    private String adapterUser;
    private String adapterPassword;
    private Integer adapterRequestTimeoutMs;
    private String primaryAdapter;

    public String getPrimaryAdapter() {
        return primaryAdapter;
    }

    public void setPrimaryAdapter(String primaryAdapter) {
        this.primaryAdapter = primaryAdapter;
    }

    public String getAdapterUrl() {
        return adapterUrl;
    }

    public void setAdapterUrl(String adapterUrl) {
        this.adapterUrl = adapterUrl;
    }

    public String getAdapterUser() {
        return adapterUser;
    }

    public void setAdapterUser(String adapterUser) {
        this.adapterUser = adapterUser;
    }

    public String getAdapterPassword() {
        return adapterPassword;
    }

    public void setAdapterPassword(String adapterPassword) {
        this.adapterPassword = adapterPassword;
    }

    public Integer getAdapterRequestTimeoutMs() {
        return adapterRequestTimeoutMs;
    }

    public void setAdapterRequestTimeoutMs(Integer adapterRequestTimeoutMs) {
        this.adapterRequestTimeoutMs = adapterRequestTimeoutMs;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("GMSConfiguration [adapterUrl=").append(adapterUrl);
        builder.append(", adapterUser=").append(adapterUser);
        builder.append(", adapterPassword=").append("[output suppressed]");
        builder.append(", adapterRequestTimeoutMs=").append(adapterRequestTimeoutMs);
        builder.append("]");
        return builder.toString();
    }

}
