package de.infinit.chatservice.configuration;

import com.genesyslab.platform.commons.collections.KeyValueCollection;
import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.configuration.beans.ServiceConfig;
import de.infinit.chatservice.beans.Tuple;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import de.infinit.configuration.beans.TimeTableConfig;

public class CmeHelper {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CmeHelper.class);

    /**
     * Evaluate Callback Wrapper application options from KVCollection
     *
     * @param applicationName
     * @param optionList
     * @return
     */
    public static ApplicationOptions evaluateApplicationOptions(final String applicationName, final KeyValueCollection optionList) {
        if (log.isDebugEnabled()) {
            log.debug("[evaluateApplicationOptions] Input: applicationName=" + applicationName + ", optionList=[not logged]");
        }
        // get Application options
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (optionList.containsKey(ConfigConstants.CFG_SEC_GMS)) {
            KeyValueCollection gmsOptions = optionList.getList(ConfigConstants.CFG_SEC_GMS);
            options.getGms().setGmsPrimaryNodeName(gmsOptions.getString(ConfigConstants.CFG_KEY_PRIMARY_GMS));
            options.getGms().setGmsUser(gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_USER));
            options.getGms().setGmsPassword(gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_PASSWORD));
        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_GMS + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (optionList.containsKey(ConfigConstants.CFG_SEC_ALERTS)) {
            KeyValueCollection alertsOptions = optionList.getList(ConfigConstants.CFG_SEC_ALERTS);
            loadAlerts(alertsOptions);
        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_ALERTS + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (optionList.containsKey(ConfigConstants.CFG_SEC_COMMON)) {
            KeyValueCollection commonOptions = optionList.getList(ConfigConstants.CFG_SEC_COMMON);
            loadCommon(commonOptions);
        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_COMMON + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (optionList.containsKey(ConfigConstants.CFG_SEC_DATABASE)) {
            KeyValueCollection databaseOptions = optionList.getList(ConfigConstants.CFG_SEC_DATABASE);
            loadDatabase(databaseOptions);
        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_DATABASE + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (optionList.containsKey(ConfigConstants.CFG_SEC_ADAPTER)) {
            KeyValueCollection adapterOptions = optionList.getList(ConfigConstants.CFG_SEC_ADAPTER);
            loadAdapter(adapterOptions);
        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_ADAPTER + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (optionList.containsKey(ConfigConstants.CFG_SEC_STATISTICS)) {
            KeyValueCollection statisticsOptions = optionList.getList(ConfigConstants.CFG_SEC_STATISTICS);
            loadStatistics(statisticsOptions);

        } else {
            log.warn("Section \"" + ConfigConstants.CFG_SEC_STATISTICS + "\" is not configured in CME application[" + applicationName + "] options.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Application options for " + applicationName + ": " + options);
        }

        return options;
    }

    public static boolean updateGmsApplicationOptions(final KeyValueCollection updated, final KeyValueCollection deleted, final KeyValueCollection added) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean updateGmsAvailability = false;

        if (updated != null && updated.containsKey(ConfigConstants.CFG_SEC_GMS)) {
            KeyValueCollection gmsOptions = updated.getList(ConfigConstants.CFG_SEC_GMS);
            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_PRIMARY_GMS)) {
                String primaryNode = gmsOptions.getString(ConfigConstants.CFG_KEY_PRIMARY_GMS);
                log.info(ConfigConstants.CFG_KEY_PRIMARY_GMS + " was changed to " + primaryNode);
                options.getGms().setGmsPrimaryNodeName(primaryNode);
                updateGmsAvailability = true;
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_USER)) {
                String gmsUser = gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_USER);
                log.info(ConfigConstants.CFG_KEY_GMS_USER + " was changed to " + gmsUser);
                options.getGms().setGmsUser(gmsUser);
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_PASSWORD)) {
                log.info(ConfigConstants.CFG_KEY_GMS_PASSWORD + " was changed to [output-suppressed]");
                options.getGms().setGmsPassword(gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_PASSWORD));
            }
        }

        if (added != null && added.containsKey(ConfigConstants.CFG_SEC_GMS)) {
            KeyValueCollection gmsOptions = added.getList(ConfigConstants.CFG_SEC_GMS);
            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_PRIMARY_GMS)) {
                String primaryNode = gmsOptions.getString(ConfigConstants.CFG_KEY_PRIMARY_GMS);
                log.info(ConfigConstants.CFG_KEY_PRIMARY_GMS + " was changed to " + primaryNode);
                options.getGms().setGmsPrimaryNodeName(primaryNode);
                updateGmsAvailability = true;
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_USER)) {
                String gmsUser = gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_USER);
                log.info(ConfigConstants.CFG_KEY_GMS_USER + " was changed to " + gmsUser);
                options.getGms().setGmsUser(gmsUser);
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_PASSWORD)) {
                log.info(ConfigConstants.CFG_KEY_GMS_PASSWORD + " was changed to [output-suppressed]");
                options.getGms().setGmsPassword(gmsOptions.getString(ConfigConstants.CFG_KEY_GMS_PASSWORD));
            }
        }

        if (deleted != null && deleted.containsKey(ConfigConstants.CFG_SEC_GMS)) {
            KeyValueCollection gmsOptions = deleted.getList(ConfigConstants.CFG_SEC_GMS);
            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_PRIMARY_GMS)) {
                log.warn("It is not possible to delete the " + ConfigConstants.CFG_KEY_PRIMARY_GMS + " when the application is running. The primary node will remain set to " + options.getGms().getGmsPrimaryNodeName());
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_USER)) {
                log.warn("Mandatory key " + ConfigConstants.CFG_KEY_GMS_USER + " was deleted from application configuration option. Cache is not updated");
            }

            if (gmsOptions.containsKey(ConfigConstants.CFG_KEY_GMS_PASSWORD)) {
                log.warn("Mandatory key " + ConfigConstants.CFG_KEY_GMS_PASSWORD + " was deleted from application configuration option. Cache is not updated");
            }
        }

        return updateGmsAvailability;
    }

    public static void updateApplicationOptions(final KeyValueCollection updated, final KeyValueCollection deleted, final KeyValueCollection added) {
        boolean chatServiceInstanceStatusChanged = false;
        if (updated != null) {
            if (updated.containsKey(ConfigConstants.CFG_SEC_ALERTS)) {
                KeyValueCollection alertsOptions = updated.getList(ConfigConstants.CFG_SEC_ALERTS);
                CmeHelper.loadAlerts(alertsOptions);
            }
            if (updated.containsKey(ConfigConstants.CFG_SEC_COMMON)) {
                KeyValueCollection commonOptions = updated.getList(ConfigConstants.CFG_SEC_COMMON);
                chatServiceInstanceStatusChanged = CmeHelper.loadCommon(commonOptions);
            }
            if (updated.containsKey(ConfigConstants.CFG_SEC_DATABASE)) {
                KeyValueCollection databaseOptions = updated.getList(ConfigConstants.CFG_SEC_DATABASE);
                CmeHelper.loadDatabase(databaseOptions);
            }
            if (updated.containsKey(ConfigConstants.CFG_SEC_ADAPTER)) {
                KeyValueCollection adapterOptions = updated.getList(ConfigConstants.CFG_SEC_ADAPTER);
                CmeHelper.loadAdapter(adapterOptions);
            }
            if (updated.containsKey(ConfigConstants.CFG_SEC_ADAPTER)) {
                KeyValueCollection adapterOptions = updated.getList(ConfigConstants.CFG_SEC_ADAPTER);
                CmeHelper.loadStatistics(adapterOptions);
            }
        }

        if (added != null) {
            if (added.containsKey(ConfigConstants.CFG_SEC_ALERTS)) {
                KeyValueCollection alertsOptions = added.getList(ConfigConstants.CFG_SEC_ALERTS);
                CmeHelper.loadAlerts(alertsOptions);
            }
            if (added.containsKey(ConfigConstants.CFG_SEC_COMMON)) {
                KeyValueCollection commonOptions = added.getList(ConfigConstants.CFG_SEC_COMMON);
                chatServiceInstanceStatusChanged = chatServiceInstanceStatusChanged | CmeHelper.loadCommon(commonOptions);
                log.debug("chatServiceInstanceStatusChanged=" + chatServiceInstanceStatusChanged);
            }
            if (added.containsKey(ConfigConstants.CFG_SEC_DATABASE)) {
                KeyValueCollection databaseOptions = added.getList(ConfigConstants.CFG_SEC_DATABASE);
                CmeHelper.loadDatabase(databaseOptions);
            }
            if (added.containsKey(ConfigConstants.CFG_SEC_ADAPTER)) {
                KeyValueCollection adapterOptions = added.getList(ConfigConstants.CFG_SEC_ADAPTER);
                CmeHelper.loadAdapter(adapterOptions);
            }
            if (added.containsKey(ConfigConstants.CFG_SEC_STATISTICS)) {
                KeyValueCollection statisticsOptions = added.getList(ConfigConstants.CFG_SEC_STATISTICS);
                CmeHelper.loadStatistics(statisticsOptions);
            }
        }

        if (deleted != null) {
            if (deleted.containsKey(ConfigConstants.CFG_SEC_ALERTS)) {
                KeyValueCollection alertsOptions = deleted.getList(ConfigConstants.CFG_SEC_ALERTS);
                CmeHelper.loadAlertsDefaults(alertsOptions);
            }
            if (deleted.containsKey(ConfigConstants.CFG_SEC_COMMON)) {
                KeyValueCollection commonOptions = deleted.getList(ConfigConstants.CFG_SEC_COMMON);
                CmeHelper.loadCommonDefaults(commonOptions);
            }
            if (deleted.containsKey(ConfigConstants.CFG_SEC_DATABASE)) {
                KeyValueCollection databaseOptions = deleted.getList(ConfigConstants.CFG_SEC_DATABASE);
                CmeHelper.loadDatabaseDefaults(databaseOptions);
            }
            if (deleted.containsKey(ConfigConstants.CFG_SEC_ADAPTER)) {
                KeyValueCollection adapterOptions = deleted.getList(ConfigConstants.CFG_SEC_ADAPTER);
                CmeHelper.loadAdapterDefaults(adapterOptions);
            }
            if (deleted.containsKey(ConfigConstants.CFG_SEC_STATISTICS)) {
                KeyValueCollection statisticsOptions = deleted.getList(ConfigConstants.CFG_SEC_STATISTICS);
                CmeHelper.loadStatisticsDefaults(statisticsOptions);
            }
        }
    }

    public static boolean loadCategory(String name, KeyValueCollection collection) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean categoryNotificationRequired = false;
        boolean isNew = !options.getCategories().containsKey(name);
        boolean isValid = true;

        CategoryConfig category = isNew ? new CategoryConfig() : options.getCategories().get(name);

        log.info("Loading category " + name);
        category.setSectionName(name);

        // Display name - mandatory
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME)) {
            String orig = category.getDisplayName();
            category.setDisplayName(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME));
            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME + "] = " + category.getDisplayName());
            categoryNotificationRequired = orig == null || !orig.equals(category.getDisplayName());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME + " is mandatory but is not configured and has no default.");
        }

        // Id - mandatory
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ID)) {
            String orig = category.getId();
            category.setId(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_ID));
            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_ID + "] = " + category.getId());
            categoryNotificationRequired = orig == null || !orig.equals(category.getId());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.CATEGORIES_LO_KEY_ID + " is mandatory and is but configured.");
        }

        // Opening timetable - mandatory
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE)) {
            String orig = category.getOpeningTimetable();
            category.setOpeningTimetable(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE));

            //load time
            if (orig == null || !orig.equals(category.getOpeningTimetable())) {
                TimeTableConfig timetable = ChatService.getCme().getCmeConnector().getTimetable(category.getOpeningTimetable(), true);
                category.setOpeningHours(timetable.getStatDays());
                category.setOpeningTimetableDBID(timetable.getDbid());
            }

            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE + "] = " + category.getOpeningTimetable());
            categoryNotificationRequired = orig == null || !orig.equals(category.getOpeningTimetable());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE + " is mandatory but is not configured and has no default.");
        }

        // Queuing message
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_QUEUING_MESSAGE)) {
            category.setQueuingMessage(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_QUEUING_MESSAGE));
            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_QUEUING_MESSAGE + "] = " + category.getQueuingMessage());
        }

        // Enable queuing message
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE)) {
            category.setEnableQueuingMessage(CmeHelper.getBooleanValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE, ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS));
            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE + "] = " + category.isEnableQueuingMessage());
        } else {
            category.setEnableQueuingMessage(ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS);
            log.info("Loading DEFAULT category key [" + ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE + "] = " + ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS);
        }

        // Routing service - mandatory
        if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE)) {
            category.setRoutingService(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE));
            log.info("Loading category key [" + ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE + "] = " + category.getRoutingService());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE + " is mandatory but is not configured and has no default.");
        }

        if (isNew && isValid) {
            options.getCategories().put(name, category);
        } else if (isNew && !isValid) {
            log.warn("Category " + name + " is invalid. Missing mandatory param");
        }

        return categoryNotificationRequired;
    }

    public static boolean deleteCategoryConfig(String name, KeyValueCollection collection) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean categoryNotificationRequired = false; // not used - all are mandatory. Always returns false

        if (options.getCategories().containsKey(name)) {
            CategoryConfig category = options.getCategories().get(name);

            // Display name - mandatory
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME)) {
                log.info("Mandatory service service key [" + ConfigConstants.CATEGORIES_LO_KEY_DISPLAY_NAME + "] deleted. Cache is not updated");
            }

            // Id - mandatory
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ID)) {
                log.info("Mandatory service service key [" + ConfigConstants.CATEGORIES_LO_KEY_ID + "] deleted. Cache is not updated");
            }

            // Opening timetable - mandatory
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE)) {
                log.info("Mandatory service service key [" + ConfigConstants.CATEGORIES_LO_KEY_OPENING_TIMETABLE + "] deleted. Cache is not updated");
            }

            // Queuing message
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_QUEUING_MESSAGE)) {
                category.setQueuingMessage("");
                log.info("Deleting service key [" + ConfigConstants.CATEGORIES_LO_KEY_QUEUING_MESSAGE + "]. Message set to empty string");
            }

            // Enable queuing message
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE)) {
                category.setEnableQueuingMessage(ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS);
                log.info("Deleting service key [" + ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE + "]. Message set to default value " + ConfigConstants.CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS);
            }

            // Routing service - mandatory
            if (collection.containsKey(ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE)) {
                log.info("Mandatory service service key [" + ConfigConstants.CATEGORIES_LO_KEY_ROUTING_SERVICE + "] deleted. Cache is not updated");
            }
        }

        return categoryNotificationRequired;
    }

    public static Tuple<ServiceConfig, Boolean> loadService(String name, KeyValueCollection collection) {
        if (log.isDebugEnabled()) {
            log.debug("[loadService] Input: name=" + name + ", collection=[not logged]");
        }
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean serviceNotificationRequired = false;
        boolean isNew = !options.getServices().containsKey(name);
        boolean isValid = true;

        ServiceConfig service = isNew ? new ServiceConfig() : options.getServices().get(name);

        log.info("Loading service " + name);
        service.setSectionName(name);

        // Available - mandatory with default
        if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_AVAILBALE)) {
            boolean orig = service.isAvailable();
            service.setAvailable(CmeHelper.getBooleanValueFromKVCollection(collection, ConfigConstants.SERVICES_LO_KEY_AVAILBALE, ConfigConstants.SERVICES_LO_KEY_AVAILBALE_DEFAULTS));
            log.info("Loading service key [" + ConfigConstants.SERVICES_LO_KEY_AVAILBALE + "] = " + service.isAvailable());
            serviceNotificationRequired = orig != service.isAvailable();
        } else if (isNew) {
            //if init and available option not configured then set default
            service.setAvailable(ConfigConstants.SERVICES_LO_KEY_AVAILBALE_DEFAULTS);
            log.info("Loading DEFAULT service key [" + ConfigConstants.SERVICES_LO_KEY_AVAILBALE + "] = " + ConfigConstants.SERVICES_LO_KEY_AVAILBALE_DEFAULTS);
        }

        // Brand id - mandatory
        if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_BRAND_ID)) {
            service.setBrandId(CmeHelper.getIntegerValueFromKVCollection(collection, ConfigConstants.SERVICES_LO_KEY_BRAND_ID).intValue());
            log.info("Loading service key [" + ConfigConstants.SERVICES_LO_KEY_BRAND_ID + "] = " + service.getBrandId());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.SERVICES_LO_KEY_BRAND_ID + " is mandatory but is not configured and has no default.");
        }

        // Channel - mandatory
        if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_CHANNEL)) {
            service.setChannel(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.SERVICES_LO_KEY_CHANNEL));
            log.info("Loading service key [" + ConfigConstants.SERVICES_LO_KEY_CHANNEL + "] = " + service.getChannel());
        } else if (isNew) { // Missing mandatory param in new category
            isValid = false;
            log.warn("Key " + ConfigConstants.SERVICES_LO_KEY_CHANNEL + " is mandatory but is not configured and has no default.");
        }

        // System down message
        if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE)) {
            service.setSystemDownMessage(CmeHelper.getStringValueFromKVCollection(collection, ConfigConstants.SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE));
            log.info("Loading service key [" + ConfigConstants.SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE + "] = " + service.getSystemDownMessage());
        }

        if (isNew && isValid) {
            options.getServices().put(name, service);
        } else if (isNew && !isValid) {
            log.warn("Service " + name + " is invalid. Missing mandatory param");
        }

        return new Tuple(service, serviceNotificationRequired);
    }

    public static Tuple<ServiceConfig, Boolean> deleteServiceConfig(String name, KeyValueCollection collection) {
        boolean serviceNotificationRequired = false;
        ApplicationOptions options = ChatService.getApplicationOptions();
        if (options.getServices().containsKey(name)) {
            ServiceConfig service = options.getServices().get(name);

            // Available - mandatory with default
            if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_AVAILBALE)) {
                boolean orig = service.isAvailable();
                service.setAvailable(ConfigConstants.SERVICES_LO_KEY_AVAILBALE_DEFAULTS);
                log.info("Deleting service key [" + ConfigConstants.SERVICES_LO_KEY_AVAILBALE + "] and setting to default " + ConfigConstants.SERVICES_LO_KEY_AVAILBALE_DEFAULTS);
                serviceNotificationRequired = orig != service.isAvailable();
            }

            // Brand id - mandatory
            if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_BRAND_ID)) {
                log.info("Mandatory service service key [" + ConfigConstants.SERVICES_LO_KEY_BRAND_ID + "] deleted. Cache is not updated");
            }

            // Channel - mandatory
            if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_CHANNEL)) {
                log.info("Mandatory service service key [" + ConfigConstants.SERVICES_LO_KEY_CHANNEL + "] deleted. Cache is not updated");
            }

            // System down message
            if (collection.containsKey(ConfigConstants.SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE)) {
                service.setSystemDownMessage("");
                log.info("Deleting service key [" + ConfigConstants.SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE + "]. Message set to empty string");
            }

            return new Tuple(service, serviceNotificationRequired);
        }

        return null;
    }

    private static void loadAlerts(KeyValueCollection alertsOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE)) {
            options.getAlerts().setChatServiceNotAvailable(CmeHelper.getIntegerValueFromKVCollection(alertsOptions, ConfigConstants.CFG_KEY_ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE + "] = " + options.getAlerts().getChatServiceNotAvailable());
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE)) {
            options.getAlerts().setPrimaryGmsNotAvailable(CmeHelper.getIntegerValueFromKVCollection(alertsOptions, ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE + "] = " + options.getAlerts().getPrimaryGmsNotAvailable());
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT)) {
            options.getAlerts().setPrimaryGmsReconnect(CmeHelper.getIntegerValueFromKVCollection(alertsOptions, ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT + "] = " + options.getAlerts().getPrimaryGmsReconnect());
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE)) {
            options.getAlerts().setGmsNodesNotAvailable(CmeHelper.getIntegerValueFromKVCollection(alertsOptions, ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE + "] = " + options.getAlerts().getGmsNodesNotAvailable());
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_RECONNECT)) {
            options.getAlerts().setGmsNodesReconnect(CmeHelper.getIntegerValueFromKVCollection(alertsOptions, ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_RECONNECT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_RECONNECT + "] = " + options.getAlerts().getGmsNodesReconnect());
        }
    }

    private static void loadAlertsDefaults(KeyValueCollection alertsOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE)) {
            options.getAlerts().setPrimaryGmsNotAvailable(ConfigConstants.ALERTS_PRIMARY_GMS_NOT_AVAILABLE_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE + " was deleted from application configuration option. Setting up default " + ConfigConstants.ALERTS_PRIMARY_GMS_NOT_AVAILABLE_DEFAULT);
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT)) {
            options.getAlerts().setPrimaryGmsReconnect(ConfigConstants.ALERTS_PRIMARY_GMS_RECONNECT_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT + " was deleted from application configuration option. Setting up default " + ConfigConstants.ALERTS_PRIMARY_GMS_RECONNECT_DEFAULT);
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE)) {
            options.getAlerts().setGmsNodesNotAvailable(ConfigConstants.ALERTS_GMS_NODES_NOT_AVAILABLE_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE + " was deleted from application configuration option. Setting up default " + ConfigConstants.ALERTS_GMS_NODES_NOT_AVAILABLE_DEFAULT);
        }
        if (alertsOptions.containsKey(ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_RECONNECT)) {
            options.getAlerts().setGmsNodesReconnect(ConfigConstants.ALERTS_GMS_NODES_RECONNECT_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_ALERTS_GMS_NODES_RECONNECT + " was deleted from application configuration option. Setting up default " + ConfigConstants.ALERTS_GMS_NODES_RECONNECT_DEFAULT);
        }
    }

    private static boolean loadCommon(KeyValueCollection commonOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean availabilityChanged = false;

        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_ENABLED)) {
            boolean origAvailability = options.getCommon().isEnabled();
            options.getCommon().setEnabled(CmeHelper.getBooleanValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_ENABLED, ConfigConstants.COMMON_ENABLED_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_ENABLED + "] = " + options.getCommon().isEnabled());
            availabilityChanged = origAvailability != options.getCommon().isEnabled();
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_CHAT_CHANNEL)) {
            options.getCommon().setChatChannel(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_CHAT_CHANNEL));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_CHAT_CHANNEL + "] = " + options.getCommon().getChatChannel());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES)) {
            options.getCommon().setSendBackClientMessages(CmeHelper.getBooleanValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES, ConfigConstants.COMMON_SEND_BACK_CLIENT_MESSAGES_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES + "] = " + options.getCommon().isSendBackClientMessages());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME)) {
            options.getCommon().setIdleChatCleanupTime(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME + "] = " + options.getCommon().getIdleChatCleanupTime());
            ChatService.getInstance().startCleanupTimer();
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH)) {
            options.getCommon().setMaxClientMessageLength(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH, ConfigConstants.COMMON_MAX_CLIENT_MESSAGE_LENGTH_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH + "] = " + options.getCommon().getMaxClientMessageLength());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT)) {
            options.getCommon().setOpenChatSyncTimeout(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT, ConfigConstants.COMMON_OPEN_CHAT_SYNC_TIMEOUT_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT + "] = " + options.getCommon().getOpenChatSyncTimeout());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_IDLE_TIMEOUT_MIN)) {
            options.getCommon().setIdleTimeoutMin(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_IDLE_TIMEOUT_MIN, 0));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_IDLE_TIMEOUT_MIN + "] = " + options.getCommon().getIdleTimeoutMin());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_POSPONE_EVENT_MS)) {
            options.getCommon().setMessagePostponeMs(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_POSPONE_EVENT_MS, ConfigConstants.COMMON_POSTPONE_MESSAGES_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_POSPONE_EVENT_MS + "] = " + options.getCommon().getMessagePostponeMs());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_BRAND_ID)) {
            options.getCommon().setBrandId(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_BRAND_ID, ConfigConstants.COMMON_BRAND_ID_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_BRAND_ID + "] = " + options.getCommon().getBrandId());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_CHANNEL)) {
            options.getCommon().setChannel(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_CHANNEL));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_CHANNEL + "] = " + options.getCommon().getChannel());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_EXTERNAL_NICKNAME)) {
            options.getCommon().setExternalNickname(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_EXTERNAL_NICKNAME));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_EXTERNAL_NICKNAME + "] = " + options.getCommon().getExternalNickname());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS)) {
            options.getCommon().setPingClientIntervalMs(CmeHelper.getIntegerValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS, ConfigConstants.COMMON_PING_CLIENT_INTERVAL_MS_DEFAULT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS + "] = " + options.getCommon().getPingClientIntervalMs());
            ChatService.getInstance().startPingAdapterTimer();
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_LO_CHAT_CATEGORIES)) {
            options.getCommon().setChatCategoriesLOName(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_LO_CHAT_CATEGORIES));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_LO_CHAT_CATEGORIES + "] = " + options.getCommon().getChatCategoriesLOName());
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY)) {
            options.getCommon().setServiceAvailabilityLOName(CmeHelper.getStringValueFromKVCollection(commonOptions, ConfigConstants.CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY + "] = " + options.getCommon().getServiceAvailabilityLOName());
        }

        return availabilityChanged;
    }

    private static boolean loadCommonDefaults(KeyValueCollection commonOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        boolean availabilityChanged = false;

        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_ENABLED)) {
            boolean origAvailability = options.getCommon().isEnabled();
            options.getCommon().setEnabled(ConfigConstants.COMMON_ENABLED_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_ENABLED + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_ENABLED_DEFAULT);
            availabilityChanged = origAvailability != options.getCommon().isEnabled();
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_CHAT_CHANNEL)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_COMMON_CHAT_CHANNEL + " was deleted from application configuration option. Cache is not updated");
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES)) {
            options.getCommon().setSendBackClientMessages(ConfigConstants.COMMON_SEND_BACK_CLIENT_MESSAGES_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_SEND_BACK_CLIENT_MESSAGES_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME)) {
            options.getCommon().setIdleChatCleanupTime(ConfigConstants.COMMON_IDLE_CHAT_CLEANUP_TIME_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_IDLE_CHAT_CLEANUP_TIME_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH)) {
            options.getCommon().setMaxClientMessageLength(ConfigConstants.COMMON_MAX_CLIENT_MESSAGE_LENGTH_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_MAX_CLIENT_MESSAGE_LENGTH_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT)) {
            options.getCommon().setOpenChatSyncTimeout(ConfigConstants.COMMON_OPEN_CHAT_SYNC_TIMEOUT_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_OPEN_CHAT_SYNC_TIMEOUT_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_POSPONE_EVENT_MS)) {
            options.getCommon().setMessagePostponeMs(ConfigConstants.COMMON_POSTPONE_MESSAGES_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_POSPONE_EVENT_MS + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_POSTPONE_MESSAGES_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_IDLE_TIMEOUT_MIN)) {
            options.getCommon().setIdleTimeoutMin(ConfigConstants.COMMON_IDLE_TIMEOUT_MIN_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_IDLE_TIMEOUT_MIN + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_IDLE_TIMEOUT_MIN_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS)) {
            options.getCommon().setPingClientIntervalMs(ConfigConstants.COMMON_PING_CLIENT_INTERVAL_MS_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_PING_CLIENT_INTERVAL_MS_DEFAULT);
            ChatService.getInstance().startPingAdapterTimer();
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_LO_CHAT_CATEGORIES)) {
            options.getCommon().setChatCategoriesLOName(ConfigConstants.COMMON_LO_CHAT_CATEGORIES_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_LO_CHAT_CATEGORIES + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_LO_CHAT_CATEGORIES_DEFAULT);
        }
        if (commonOptions.containsKey(ConfigConstants.CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY)) {
            options.getCommon().setServiceAvailabilityLOName(ConfigConstants.COMMON_LO_SERVICE_AVAILABILITY_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY + " was deleted from application configuration option. Setting up default " + ConfigConstants.COMMON_LO_SERVICE_AVAILABILITY_DEFAULT);
        }

        return availabilityChanged;
    }

    private static void loadDatabase(KeyValueCollection databaseOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (databaseOptions.containsKey(ConfigConstants.CFG_KEY_DATABASE_CASSANDRA_TIMEOUT)) {
            options.getDatabase().setCassandraTimeout(CmeHelper.getIntegerValueFromKVCollection(databaseOptions, ConfigConstants.CFG_KEY_DATABASE_CASSANDRA_TIMEOUT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_DATABASE_CASSANDRA_TIMEOUT + "] = " + options.getDatabase().getCassandraTimeout());
        }
        if (databaseOptions.containsKey(ConfigConstants.CFG_KEY_DATABASE_TTL)) {
            options.getDatabase().setTtl(CmeHelper.getIntegerValueFromKVCollection(databaseOptions, ConfigConstants.CFG_KEY_DATABASE_TTL));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_DATABASE_TTL + "] = " + options.getDatabase().getTtl());
        }
    }

    private static void loadDatabaseDefaults(KeyValueCollection databaseOptions) {
        if (databaseOptions.containsKey(ConfigConstants.CFG_KEY_DATABASE_CASSANDRA_TIMEOUT)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_DATABASE_CASSANDRA_TIMEOUT + " was deleted from application configuration option. Cache is not updated");
        }
        if (databaseOptions.containsKey(ConfigConstants.CFG_KEY_DATABASE_TTL)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_DATABASE_TTL + " was deleted from application configuration option. Cache is not updated");
        }
    }

    private static void loadAdapter(KeyValueCollection adapterOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_TIMEOUT)) {
            options.getAdapterEndpoint().setAdapterRequestTimeoutMs(CmeHelper.getIntegerValueFromKVCollection(adapterOptions, ConfigConstants.CFG_KEY_ADAPTER_TIMEOUT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ADAPTER_TIMEOUT + "] = " + options.getAdapterEndpoint().getAdapterRequestTimeoutMs());
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_URL)) {
            options.getAdapterEndpoint().setAdapterUrl(CmeHelper.getStringValueFromKVCollection(adapterOptions, ConfigConstants.CFG_KEY_ADAPTER_URL));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ADAPTER_URL + "] = " + options.getAdapterEndpoint().getAdapterUrl());
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_PRIMARY_ADAPTER)) {
            options.getAdapterEndpoint().setPrimaryAdapter(CmeHelper.getStringValueFromKVCollection(adapterOptions, ConfigConstants.CFG_KEY_PRIMARY_ADAPTER));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_PRIMARY_ADAPTER + "] = " + options.getAdapterEndpoint().getPrimaryAdapter());
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_USERNAME)) {
            options.getAdapterEndpoint().setAdapterUser(CmeHelper.getStringValueFromKVCollection(adapterOptions, ConfigConstants.CFG_KEY_ADAPTER_USERNAME));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ADAPTER_USERNAME + "] = " + options.getAdapterEndpoint().getAdapterUser());
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_PASSWORD)) {
            options.getAdapterEndpoint().setAdapterPassword(CmeHelper.getStringValueFromKVCollection(adapterOptions, ConfigConstants.CFG_KEY_ADAPTER_PASSWORD));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_ADAPTER_PASSWORD + "] ");
        }
    }

    private static void loadAdapterDefaults(KeyValueCollection adapterOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_TIMEOUT)) {
            options.getAdapterEndpoint().setAdapterRequestTimeoutMs(ConfigConstants.ADAPTER_TIMEOUT_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_ADAPTER_URL + " was deleted from application configuration option. Setting up default " + ConfigConstants.ADAPTER_TIMEOUT_DEFAULT);
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_URL)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_ADAPTER_URL + " was deleted from application configuration option. Cache is not updated");
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_USERNAME)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_ADAPTER_USERNAME + " was deleted from application configuration option. Cache is not updated");
        }
        if (adapterOptions.containsKey(ConfigConstants.CFG_KEY_ADAPTER_PASSWORD)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_ADAPTER_PASSWORD + " was deleted from application configuration option. Cache is not updated");
        }
    }

    private static void loadStatistics(KeyValueCollection statisticsOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS)) {
            options.getStatistics().setConcurrentChatThreshold(CmeHelper.getIntegerValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS + "] = " + options.getStatistics().getConcurrentChatThreshold());
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS)) {
            options.getStatistics().setGroupOfAllChatAgents(CmeHelper.getStringValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS + "] = " + options.getStatistics().getGroupOfAllChatAgents());
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES)) {
            options.getStatistics().setGroupOfAllChatQueues(CmeHelper.getStringValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES + "] = " + options.getStatistics().getGroupOfAllChatQueues());
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_TIMEOUT)) {
            options.getStatistics().setStatisticsTimeout(CmeHelper.getIntegerValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_TIMEOUT));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_TIMEOUT + "] = " + options.getStatistics().getStatisticsTimeout());
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS)) {
            options.getStatistics().setCurrentNumberAcceptedChats(CmeHelper.getStringValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS + "] = " + options.getStatistics().getCurrentNumberAcceptedChats());
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_WAITING_CHATS)) {
            options.getStatistics().setWaitingChats(CmeHelper.getStringValueFromKVCollection(statisticsOptions, ConfigConstants.CFG_KEY_STATISTICS_WAITING_CHATS));
            log.info("Loading config key [" + ConfigConstants.CFG_KEY_STATISTICS_WAITING_CHATS + "] = " + options.getStatistics().getWaitingChats());
        }
    }

    private static void loadStatisticsDefaults(KeyValueCollection statisticsOptions) {
        ApplicationOptions options = ChatService.getApplicationOptions();

        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS + " was deleted from application configuration option. Cache is not updated");
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS + " was deleted from application configuration option. Cache is not updated");
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES + " was deleted from application configuration option. Cache is not updated");
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_TIMEOUT)) {
            options.getStatistics().setStatisticsTimeout(ConfigConstants.STATISTICS_TIMEOUT_DEFAULT);
            log.warn("Config key " + ConfigConstants.CFG_KEY_STATISTICS_TIMEOUT + " was deleted from application configuration option. Setting up default " + ConfigConstants.STATISTICS_TIMEOUT_DEFAULT);
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS + " was deleted from application configuration option. Cache is not updated");
        }
        if (statisticsOptions.containsKey(ConfigConstants.CFG_KEY_STATISTICS_WAITING_CHATS)) {
            log.warn("Mandatory config key " + ConfigConstants.CFG_KEY_STATISTICS_WAITING_CHATS + " was deleted from application configuration option. Cache is not updated");
        }
    }

    public static String getStringValueFromKVCollection(final KeyValueCollection kvCollection, final String key) {
        String result = "";

        if (kvCollection.containsKey(key)) {
            String stringValue = kvCollection.getString(key);
            result = stringValue;
        }

        if (result == null || "".equals(result)) {
            log.warn("Value for key " + key + " is empty");
        }

        return result;
    }

    public static String getStringValueFromKVCollection(final KeyValueCollection kvCollection, final String key, final String defaultValue) {
        String result = defaultValue;

        if (kvCollection.containsKey(key)) {
            String stringValue = kvCollection.getString(key);
            result = stringValue;
        }
        return result;
    }

    public static Integer getIntegerValueFromKVCollection(final KeyValueCollection kvCollection, final String key) {
        Integer result = null;

        if (kvCollection.containsKey(key)) {
            String stringValue = kvCollection.getString(key);

            try {
                result = Integer.valueOf(stringValue);
            } catch (Exception e) {
                log.warn("Cannot convert keyValuePair " + key + " = " + stringValue + " to Integer.");
            }
        }
        return result;
    }

    public static boolean getBooleanValueFromKVCollection(final KeyValueCollection kvCollection, final String key, final boolean defaultValue) {
        boolean result = defaultValue;

        if (kvCollection.containsKey(key)) {
            String stringValue = kvCollection.getString(key);

            if ("true".equalsIgnoreCase(stringValue)) {
                result = true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                result = false;
            } else {
                log.warn("Wrong value configured for boolean option " + key + ". The configured value is " + stringValue + ", defualt value " + defaultValue + " will be used.");
            }
        }
        return result;
    }

    public static Integer getIntegerValueFromKVCollection(final KeyValueCollection kvCollection, final String key, final Integer defaultValue) {
        Integer result = defaultValue;

        if (kvCollection.containsKey(key)) {
            String stringValue = kvCollection.getString(key);

            try {
                result = Integer.valueOf(stringValue);
            } catch (Exception e) {
                log.warn("Cannot convert keyValuePair " + key + " = " + stringValue + " to Integer.");
            }
        }
        return result;
    }
}
