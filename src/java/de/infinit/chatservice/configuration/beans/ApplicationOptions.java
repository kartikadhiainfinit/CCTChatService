package de.infinit.chatservice.configuration.beans;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ApplicationOptions{

    private final DatabaseConfiguration database;
    private final CommonConfiguration common;
    private final GMSConfiguration gms;
    private final AlertsConfiguration alerts;
    private final AdapterEndpointConfiguration adapterEndpoint;
    private final StatisticsConfiguration statistics;

    private int servicesDBId;
    private int categoriesDBId;
    private LinkedList<ServiceConfig> registeredServices = new LinkedList();
    private final Map<String, ServiceConfig> services = new HashMap<>();
    private final Map<String, CategoryConfig> categories = new HashMap<>();

    public ApplicationOptions() {
        this.database = new DatabaseConfiguration();
        this.common = new CommonConfiguration();
        this.gms = new GMSConfiguration();
        this.alerts = new AlertsConfiguration();
        this.adapterEndpoint = new AdapterEndpointConfiguration();
        this.statistics = new StatisticsConfiguration();
    }

    /**
     * @return the alerts configuration section
     */
    public AlertsConfiguration getAlerts() {
        return alerts;
    }

    /**
     * @return the database configuration section
     */
    public DatabaseConfiguration getDatabase() {
        return database;
    }

    /**
     * @return the common configuration section
     */
    public CommonConfiguration getCommon() {
        return common;
    }

    public StatisticsConfiguration getStatistics() {
        return statistics;
    }

    /**
     * return the gms configuration section
     *
     * @return
     */
    public GMSConfiguration getGms() {
        return gms;
    }

    public AdapterEndpointConfiguration getAdapterEndpoint() {
        return adapterEndpoint;
    }

    public LinkedList<ServiceConfig> getRegisteredServices() {
        return registeredServices;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public Map<String, CategoryConfig> getCategories() {
        return categories;
    }

    public int getServicesDBId() {
        return servicesDBId;
    }

    public void setServicesDBId(int servicesDBId) {
        this.servicesDBId = servicesDBId;
    }

    public int getCategoriesDBId() {
        return categoriesDBId;
    }

    public void setCategoriesDBId(int categoriesDBId) {
        this.categoriesDBId = categoriesDBId;
    }

    @Override
    public String toString() {
        return "ConfigurationData [\n\t database=" + database + "\n\t common=" + common + "\n\t gms=" + gms + "\n\t alerts=" + alerts + "\n]";
    }    
}
