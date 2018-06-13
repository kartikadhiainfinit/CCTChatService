/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.configuration;

import com.genesyslab.platform.applicationblocks.com.ConfigException;
import com.genesyslab.platform.applicationblocks.com.objects.CfgStatDay;
import com.genesyslab.platform.applicationblocks.com.objects.CfgTransaction;
import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.collections.KeyValuePair;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.configuration.protocol.confserver.events.EventObjectUpdated;
import com.genesyslab.platform.configuration.protocol.obj.ConfIntegerCollection;
import com.genesyslab.platform.configuration.protocol.obj.ConfObject;
import com.genesyslab.platform.configuration.protocol.obj.ConfObjectDelta;
import com.genesyslab.platform.configuration.protocol.types.CfgObjectState;
import com.genesyslab.platform.configuration.protocol.types.CfgObjectType;
import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.beans.Tuple;
import de.infinit.chatservice.beans.ChatServiceDef;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import de.infinit.chatservice.configuration.beans.ServiceConfig;
import de.infinit.chatservice.db.DBConnector;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.InvalidArgumentException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.utils.OpeningHoursHelper;
import de.infinit.configuration.CMEConnector;
import de.infinit.configuration.ICMEApplication;
import de.infinit.configuration.ICMEConnector;
import de.infinit.configuration.beans.CMEConnectionParameters;
import de.infinit.configuration.beans.ConnectionServerInfo;
import de.infinit.configuration.beans.TimeTableConfig;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author sebesjir, xxbamar
 */
public class CMEApplication implements ICMEApplication {

    public final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CMEApplication.class);

    private ICMEConnector cmeConnector;
    private ChatService chatService;
    private ApplicationOptions options = null;

    public ICMEConnector getCmeConnector() {
        return cmeConnector;
    }

    public boolean isConnectionOpen() {
        return cmeConnector.isConnectionOpen();
    }

    public void closeConnection() {
        cmeConnector.closeConnection();
    }

    public CMEApplication(CMEConnectionParameters configuration) throws ConfigException {
        cmeConnector = initCMEServerConnector(configuration);
    }

    public List<ConnectionServerInfo> getGmsNodes(String applicationName) throws ProtocolException, InterruptedException {
        return cmeConnector.getGmsNodes(applicationName);
    }

    public List<ConnectionServerInfo> getEndpoints(String applicationName) throws ProtocolException, InterruptedException {
        return cmeConnector.getEndpoints(applicationName);
    }

    public ApplicationOptions loadApplicationOptions(String applicationName) {
        options = ChatService.getApplicationOptions();

        CMEConnectionParameters cmeParams = ConfigFilesHelper.loadConfigServerConfiguration(ChatService.getApplicationPath(), ChatService.isDebug());
        // init CME connection
        try {
            if (cmeConnector == null) {
                cmeConnector = initCMEServerConnector(cmeParams);
            }
        } catch (Exception ex) {
            log.error("Cannot open connection to Config server", ex);
            //chatService.shutdownServer();
        }

        if (log.isDebugEnabled()) {
            log.debug("getApplicationOptions(applicationName=" + applicationName + ") - entered...");
        }
        if (cmeConnector.isConnectionOpen()) {
            try {
                KeyValueCollection optionList = cmeConnector.getOptionList();
                options = CmeHelper.evaluateApplicationOptions(applicationName, optionList);
            } catch (ConfigException | ProtocolException | InterruptedException ex) {
                log.error("Not able to read application configuration from CME for application \"" + applicationName + "\"", ex);
            }
        }

        // load service availability LO
        loadServices();

        // load categories LO
        loadCategories();

        if (log.isDebugEnabled()) {
            log.debug("Leaving getApplicationOptions with result=" + options);
        }

        ChatService.getGmsConnector().readGMSNodesConnection();
        ChatService.getGmsConnector().evaluateGMSNodesAvailability();

        return options;
    }

    public void registerServices(LinkedList<ChatServiceDef> chatServicesForRegistreation) throws InvalidArgumentException {
        for (ChatServiceDef chatServiceForRegistration : chatServicesForRegistreation) {
            boolean serviceFound = false;
            for (Map.Entry<String, ServiceConfig> kvp : options.getServices().entrySet()) {
                ServiceConfig service = kvp.getValue();
                if (service.getBrandId() == chatServiceForRegistration.getBrandId() && service.getChannel().equals(chatServiceForRegistration.getChannel())) {
                    options.getRegisteredServices().add(service);
                    serviceFound = true;
                }
            }

            if (!serviceFound) {
                log.warn("Chat service for channel: " + chatServiceForRegistration.getChannel() + ", brandId:" + chatServiceForRegistration.getBrandId() + " not exists.");
                throw new InvalidArgumentException("Chat service for channel: " + chatServiceForRegistration.getChannel() + ", brandId:" + chatServiceForRegistration.getBrandId() + " not exists.");
            }
        }
    }

    private void loadCategories() {
        try {
            if (options.getCommon().getChatCategoriesLOName() != null && !"".equals(options.getCommon().getChatCategoriesLOName())) {
                CfgTransaction tr = cmeConnector.getListObject(options.getCommon().getChatCategoriesLOName());
                KeyValueCollection categories = tr.getUserProperties();
                options.setCategoriesDBId(tr.getDBID());

                for (Iterator it = categories.iterator(); it.hasNext();) {
                    KeyValuePair kvp = (KeyValuePair) it.next();
                    String name = (String) kvp.getStringKey();
                    KeyValueCollection val = kvp.getTKVValue();

                    CmeHelper.loadCategory(name, val);
                }
            } else {
                log.warn("No category LO name configured");
            }
        } catch (ConfigException | ProtocolException | InterruptedException ex) {
            log.error("Error while reading categories", ex);
        }

        // fill timetables
        Map<String, CategoryConfig> categories = options.getCategories();
        Collection<CategoryConfig> categoriesvalues = categories.values();

        for (CategoryConfig categoryConfig : categoriesvalues) {
            String openingTimetable = categoryConfig.getOpeningTimetable();
            if (openingTimetable != null && !openingTimetable.isEmpty()) {
                TimeTableConfig timetable = cmeConnector.getTimetable(openingTimetable, true);
                categoryConfig.setOpeningHours(timetable.getStatDays());
                categoryConfig.setOpeningTimetableDBID(timetable.getDbid());
            }
        }
    }

    private void loadServices() {
        if (log.isDebugEnabled()) {
            log.debug("[loadServices] Started");
        }
        try {
            String serviceLOName = options.getCommon().getServiceAvailabilityLOName();
            if (serviceLOName != null && !serviceLOName.trim().isEmpty()) {
                if (log.isInfoEnabled()) {
                    log.info("Load services from " + serviceLOName + " list object");
                }

                CfgTransaction tr = cmeConnector.getListObject(serviceLOName);
                if (tr != null) {
                    KeyValueCollection services = tr.getUserProperties();
                    options.setServicesDBId(tr.getDBID());

                    for (Iterator it = services.iterator(); it.hasNext();) {
                        KeyValuePair kvp = (KeyValuePair) it.next();
                        String name = (String) kvp.getStringKey();
                        KeyValueCollection val = kvp.getTKVValue();

                        ServiceConfig service = CmeHelper.loadService(name, val).x;
                    }
                } else {
                    log.warn("Cannot read service availability list object [" + serviceLOName + "]");
                }
            } else {
                log.warn("No services availability LO name configured");
            }
        } catch (ConfigException | ProtocolException | InterruptedException ex) {
            log.error("Error while reading services LO", ex);
        }
    }

    @Override
    public void onCMEChannelOpened() {
        if (options != null) {
            ChatService.getMsLogger().alert(options.getAlerts().getCmeReconnect(), "Configuration server conection re-opened");
        }

        // get Config. server connection parameters
        CMEConnectionParameters cmeParams = ConfigFilesHelper.loadConfigServerConfiguration(ChatService.getApplicationPath(), ChatService.isDebug());
        // start Config server connection
        if (cmeParams != null) {
            if (options == null && ChatService.getApplicationName() != null) {

                loadApplicationOptions(ChatService.getApplicationName());
                // start message server

                chatService.finishInitialization();
            } else if (ChatService.getApplicationName() != null) {
                loadApplicationOptions(ChatService.getApplicationName());
            }
        }
    }

    /**
     * Initialize connection to Config. server
     *
     * @param configuration
     * @return
     * @throws ConfigException
     */
    private ICMEConnector initCMEServerConnector(CMEConnectionParameters configuration) throws ConfigException {
        if (configuration.getHost() == null || configuration.getPort() == null) {
            throw new ConfigException("Missing configuration for primary CME server.");
        }
        if (configuration.getHostBackup() == null || configuration.getPortBackup() == null || configuration.getHostBackup().isEmpty() || configuration.getPortBackup().isEmpty()) {
            log.info("Missing host and port for backup CME server.");
        }
        return new CMEConnector(this, configuration);
    }

    @Override
    public void onMessageRetrieved(Message message) {
        if (message instanceof EventObjectUpdated) {

            final EventObjectUpdated eventObjectUpdated = (EventObjectUpdated) message;

            KeyValueCollection updatedKVC = null;
            KeyValueCollection deletedKVC = null;
            KeyValueCollection addedKVC = null;

            if (eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGApplication.asInteger().intValue()) {
                ConfObjectDelta updateDelta = eventObjectUpdated.getObjectDelta();

                Object changedOptions = updateDelta.getPropertyValue("changedOptions");
                if (changedOptions != null) {
                    updatedKVC = (KeyValueCollection) changedOptions;
                }

                Object deletedOptions = updateDelta.getPropertyValue("deletedOptions");
                if (deletedOptions != null) {
                    deletedKVC = (KeyValueCollection) deletedOptions;
                }

                Object deltaApplication = updateDelta.getPropertyValue("deltaApplication");
                if (deltaApplication != null) {
                    if (deltaApplication instanceof ConfObject) {
                        ConfObject delta = (ConfObject) deltaApplication;
                        Object propertyValue = delta.getPropertyValue("options");
                        addedKVC = (KeyValueCollection) propertyValue;
                    }
                }

                updateApplicationOptions(updatedKVC, deletedKVC, addedKVC);
            }
            if (eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGTransaction.asInteger().intValue()) {
                // LO updated
                ConfObjectDelta updateDelta = eventObjectUpdated.getObjectDelta();

                if (updateDelta.getObjectDbid() == options.getServicesDBId()) {
                    updatedKVC = null;
                    deletedKVC = null;
                    addedKVC = null;

                    Object changedOptions = updateDelta.getPropertyValue("changedUserProperties");
                    if (changedOptions != null) {
                        updatedKVC = (KeyValueCollection) changedOptions;
                    }

                    Object deletedOptions = updateDelta.getPropertyValue("deletedUserProperties");
                    if (deletedOptions != null) {
                        deletedKVC = (KeyValueCollection) deletedOptions;
                    }

                    Object deltaTransaction = updateDelta.getPropertyValue("deltaTransaction");
                    if (deltaTransaction != null) {
                        if (deltaTransaction instanceof ConfObject) {
                            ConfObject delta = (ConfObject) deltaTransaction;
                            Object propertyValue = delta.getPropertyValue("userProperties");
                            addedKVC = (KeyValueCollection) propertyValue;
                        }
                    }
                    final Integer eventId = eventObjectUpdated.getUnsolisitedEventNumber();
                    updateServicesLO(eventId, updatedKVC, deletedKVC, addedKVC);
                }
                if (updateDelta.getObjectDbid() == options.getCategoriesDBId()) {
                    updatedKVC = null;
                    deletedKVC = null;
                    addedKVC = null;

                    Object changedOptions = updateDelta.getPropertyValue("changedUserProperties");
                    if (changedOptions != null) {
                        updatedKVC = (KeyValueCollection) changedOptions;
                    }

                    Object deletedOptions = updateDelta.getPropertyValue("deletedUserProperties");
                    if (deletedOptions != null) {
                        deletedKVC = (KeyValueCollection) deletedOptions;
                    }

                    Object deltaTransaction = updateDelta.getPropertyValue("deltaTransaction");
                    if (deltaTransaction != null) {
                        if (deltaTransaction instanceof ConfObject) {
                            ConfObject delta = (ConfObject) deltaTransaction;
                            Object propertyValue = delta.getPropertyValue("userProperties");
                            addedKVC = (KeyValueCollection) propertyValue;
                        }
                    }
                    updateCategoriesLO(updatedKVC, deletedKVC, addedKVC);
                }
            }

            if (eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGStatTable.asInteger().intValue()
                    || eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGStatDay.asInteger().intValue()) {
                processTimetablesUpdate(eventObjectUpdated);
            }
        }
    }

    private void processTimetablesUpdate(final EventObjectUpdated eventObjectUpdated) {
        final Integer eventId = eventObjectUpdated.getUnsolisitedEventNumber();
        boolean sendNotification = false;

        if (eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGStatTable.asInteger().intValue()) {

            ConfObjectDelta updateDelta = eventObjectUpdated.getObjectDelta();
            Integer dbId = updateDelta.getObjectDbid();

            if (log.isInfoEnabled()) {
                log.info("Update for CFGStatTable [" + dbId + "] received:" + updateDelta);
            }

            ConfObject updatedStatTable = null;
            ConfIntegerCollection deletedStatTable = null;

            Object changedOptions = updateDelta.getPropertyValue("deltaStatTable");
            if (changedOptions != null) {
                updatedStatTable = (ConfObject) changedOptions;
            }

            Object deletedOptions = updateDelta.getPropertyValue("deletedStatDayDBIDs");
            if (deletedOptions != null) {
                deletedStatTable = (ConfIntegerCollection) deletedOptions;
            }
            sendNotification = updateStatTable(dbId, updatedStatTable, deletedStatTable);
        }

        if (eventObjectUpdated.getObjectType().intValue() == CfgObjectType.CFGStatDay.asInteger().intValue()) {

            ConfObjectDelta updateDelta = eventObjectUpdated.getObjectDelta();
            Integer dbId = updateDelta.getObjectDbid();
            if (log.isInfoEnabled()) {
                log.info("Update for CFGStatDay [" + dbId + "] received:" + updateDelta);
            }

            Object changedOptions = updateDelta.getPropertyValue("deltaStatDay");
            if (changedOptions != null) {
                ConfObject updatedStatDay = (ConfObject) changedOptions;
                sendNotification = updateStatDay(updatedStatDay);
            }
        }

        if (sendNotification) {
            // postpone
            if (options.getCommon().getMessagePostponeMs() > 0) {
                Timer postponeTimer = new Timer();
                postponeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendCategoryNotification(eventId);
                    }
                }, options.getCommon().getMessagePostponeMs());

            } else {
                sendCategoryNotification(eventId);
            }
        }
    }

    private void sendCategoryNotification(final Integer eventId) {
        DBConnector db = ChatService.getDb();

        try {
            String processed = db.getEventProcessedInfo(String.valueOf(eventId));
            if (processed == null) {
                // sendNotification
                db.createEventHandeledEntry(String.valueOf(eventId), "false");
                ChatService.notifyCategoriesChanged();
                db.createEventHandeledEntry(String.valueOf(eventId), "true");
            }
        } catch (BackendUnavailableException | RequestFailedException ex) {
            log.warn("Error when sending a category notification", ex);
        }
    }

    private boolean updateStatTable(final Integer dbid, final ConfObject updated, final ConfIntegerCollection deleted) {
        boolean categoryNotificationRequired = false;

        if (dbid != null) {
            Calendar currentCalendar = Calendar.getInstance();

            // TODO: add reaction on disable/enable timetable
            if (updated.getPropertyValue("statDayDBIDs") != null) {
                Collection<CategoryConfig> values = options.getCategories().values();
                for (CategoryConfig value : values) {

                    if (value.getOpeningTimetableDBID() != null && value.getOpeningTimetableDBID().intValue() == dbid) {
                        log.debug("category [" + value.getDisplayName() + "] timetable " + value.getOpeningTimetable() + "[" + value.getOpeningTimetableDBID()
                                + "], was changed");
                        
                        boolean wasOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);

                        ConfIntegerCollection newStatDays = ((ConfIntegerCollection) updated.getPropertyValue("statDayDBIDs"));
                        for (Integer newStatDay : newStatDays) {
                            CfgStatDay newDay = cmeConnector.getStatDay(newStatDay, true);
                            if (newDay != null) {
                                log.debug("Add statDay to timetable " + value.getOpeningTimetable() + "[" + value.getOpeningTimetableDBID() + "] - " + newDay.getName());
                                
                                value.getOpeningHours().add(newDay);
                            }
                        }

                        boolean isOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);

                        if (wasOpen != isOpen) {
                            categoryNotificationRequired = true;
                        }
                    }
                }
            }

            if (deleted != null) {
                Collection<CategoryConfig> values = options.getCategories().values();
                for (CategoryConfig value : values) {
                    if (value.getOpeningTimetableDBID() != null && value.getOpeningTimetableDBID().intValue() == dbid.intValue()) {
                        log.debug("category [" + value.getDisplayName() + "] timetable " + value.getOpeningTimetable() + "[" + value.getOpeningTimetableDBID()
                                + "], was changed");

                        boolean wasOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                        List<CfgStatDay> openingHours = value.getOpeningHours();

                        for (Integer newStatDay : deleted) {
                            CfgStatDay remove = null;
                            for (CfgStatDay openingHour : openingHours) {
                                if (openingHour.getDBID().intValue() == newStatDay.intValue()) {
                                    remove = openingHour;
                                    break;
                                }
                            }
                            if (remove != null) {
                                log.debug("Remove statDay from timetable " + value.getOpeningTimetable() + "[" + value.getOpeningTimetableDBID() + "] - " + remove.getName());
                                openingHours.remove(remove);
                            }
                        }

                        boolean isOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                        if (wasOpen != isOpen) {
                            categoryNotificationRequired = true;
                        }
                    }
                }
            }
        }

        return categoryNotificationRequired;
    }

    private boolean updateStatDay(final ConfObject updated) {

        boolean serviceNotificationRequired = false;
        try {
            Integer dbid = updated.getObjectDbid();
            if (dbid != null) {
                Calendar currentCalendar = Calendar.getInstance();

                if (updated.getPropertyValue("startTime") != null) {
                    Collection<CategoryConfig> values = options.getCategories().values();
                    for (CategoryConfig value : values) {
                        List<CfgStatDay> openingHours = value.getOpeningHours();
                        for (CfgStatDay openingHour : openingHours) {
                            if (openingHour.getDBID().intValue() == dbid) {

                                ChatService.getInstance().startOpeningHourTimer();

                                boolean wasOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                                openingHour.setStartTime((Integer) updated.getPropertyValue("startTime"));
                                boolean isOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);

                                if (wasOpen != isOpen) {
                                    serviceNotificationRequired = true;
                                }
                            }
                        }
                    }
                } else if (updated.getPropertyValue("endTime") != null) {
                    Collection<CategoryConfig> values = options.getCategories().values();
                    for (CategoryConfig value : values) {
                        List<CfgStatDay> openingHours = value.getOpeningHours();
                        for (CfgStatDay openingHour : openingHours) {
                            if (openingHour.getDBID().intValue() == dbid) {
                                ChatService.getInstance().startOpeningHourTimer();

                                boolean wasOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                                openingHour.setEndTime((Integer) updated.getPropertyValue("endTime"));
                                boolean isOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                                if (wasOpen != isOpen) {
                                    serviceNotificationRequired = true;
                                }
                            }
                        }
                    }
                } else if (updated.getPropertyValue("state") != null) {
                    Collection<CategoryConfig> values = options.getCategories().values();
                    for (CategoryConfig value : values) {
                        List<CfgStatDay> openingHours = value.getOpeningHours();
                        for (CfgStatDay openingHour : openingHours) {
                            if (openingHour.getDBID().intValue() == dbid) {
                                ChatService.getInstance().startOpeningHourTimer();

                                boolean wasOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                                openingHour.setState(CfgObjectState.valueOf((Integer) updated.getPropertyValue("state")));
                                boolean isOpen = OpeningHoursHelper.isCategoryOpen(value, currentCalendar);
                                if (wasOpen != isOpen) {
                                    serviceNotificationRequired = true;
                                }
                            }
                        }
                    }
                }

//            if (serviceNotificationRequired) {
//                ChatService.notifyCategoriesChanged();
//            }
            }
        } catch (Exception e) {
            log.warn("Error when processing the opening hours update", e);
        }

        return serviceNotificationRequired;
    }

    private void updateServicesLO(final Integer eventId, final KeyValueCollection updated, final KeyValueCollection deleted, final KeyValueCollection added) {
        if (log.isInfoEnabled()) {
            log.info("Update for service LO retrieved (eventId=" + eventId + ")");
        }
        boolean serviceNotificationRequired = false;
        //merge updated and added - same logic
        KeyValueCollection updatedOrAdded = new KeyValueCollection();
        if (updated != null) {
            updatedOrAdded.addAll(updated);
        }
        if (added != null) {
            updatedOrAdded.addAll(added);
        }

        for (Iterator it = updatedOrAdded.iterator(); it.hasNext();) {
            KeyValuePair kvp = (KeyValuePair) it.next();
            String name = kvp.getStringKey();
            KeyValueCollection config = kvp.getTKVValue();
            Tuple<ServiceConfig, Boolean> result = CmeHelper.loadService(name, config);
            Collection<ServiceConfig> services = options.getRegisteredServices().isEmpty() ? options.getServices().values() : options.getRegisteredServices();
            for (ServiceConfig service : services) {
                if (result.x == service) {
                    serviceNotificationRequired = result.y;
                    break;
                }
            }
        }

        if (deleted != null) {
            for (Iterator it = deleted.iterator(); it.hasNext();) {
                KeyValuePair kvp = (KeyValuePair) it.next();
                String name = kvp.getStringKey();
                KeyValueCollection config = kvp.getTKVValue();

                if (config == null || config.length() == 0) { // deleted whole service
                    ServiceConfig service = findServiceByName(name);
                    if (service != null) {
                        if (options.getRegisteredServices().contains(service)) {
                            options.getRegisteredServices().remove(service);
                        }
                        options.getServices().remove(service.getSectionName());
                        log.info("Deleting service " + name);
                    }
                } else {
                    Collection<ServiceConfig> services = options.getRegisteredServices().isEmpty() ? options.getServices().values() : options.getRegisteredServices();
                    // delete just param
                    Tuple<ServiceConfig, Boolean> result = CmeHelper.deleteServiceConfig(name, config);
                    for (ServiceConfig service : services) {
                        if (result != null && result.x == service) {
                            serviceNotificationRequired = serviceNotificationRequired | result.y;
                        }
                    }
                }
            }
        }

        if (serviceNotificationRequired) {
            if (options.getCommon().getMessagePostponeMs() > 0) {
                Timer postponeTimer = new Timer();
                postponeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendServiceAvailabilityNotification(eventId);
                    }
                }, options.getCommon().getMessagePostponeMs());
            } else {
                sendServiceAvailabilityNotification(eventId);
            }
        }
    }

    private ServiceConfig findServiceByName(String serviceName) {
        for (ServiceConfig service : options.getServices().values()) {
            if (service.getSectionName().equals(serviceName)) {
                return service;
            }
        }

        return null;
    }

    private void sendServiceAvailabilityNotification(final Integer eventId) {
        if (log.isInfoEnabled()) {
            log.info("[sendServiceAvailabilityNotification] Input: eventId=" + eventId);
        }

        DBConnector db = ChatService.getDb();
        try {
            String processed = db.getEventProcessedInfo(String.valueOf(eventId));
            if (processed == null) {
                db.createEventHandeledEntry(String.valueOf(eventId), "false");
                ChatService.notifyServiceAvailabilityChanged();
                db.createEventHandeledEntry(String.valueOf(eventId), "true");
            }
        } catch (BackendUnavailableException | RequestFailedException ex) {
            log.warn("Error when sending a service availability notification", ex);
        }
    }

    private void updateCategoriesLO(final KeyValueCollection updated, final KeyValueCollection deleted, final KeyValueCollection added) {
        boolean categoriesNotificationRequired = false;
        //merge updated and added - same logic
        KeyValueCollection updatedOrAdded = new KeyValueCollection();
        if (updated != null) {
            updatedOrAdded.addAll(updated);
        }
        if (added != null) {
            updatedOrAdded.addAll(added);
        }

        for (Iterator it = updatedOrAdded.iterator(); it.hasNext();) {
            KeyValuePair kvp = (KeyValuePair) it.next();
            String name = kvp.getStringKey();
            KeyValueCollection config = kvp.getTKVValue();
            categoriesNotificationRequired = CmeHelper.loadCategory(name, config);
        }

        if (deleted != null) {
            for (Iterator it = deleted.iterator(); it.hasNext();) {
                KeyValuePair kvp = (KeyValuePair) it.next();
                String name = kvp.getStringKey();
                KeyValueCollection config = kvp.getTKVValue();

                if (config == null || config.length() == 0) { // deleted whole service
                    if (options.getCategories().containsKey(name)) {
                        options.getCategories().remove(name);
                        log.info("Deleting category " + name);
                        categoriesNotificationRequired = true;
                    }
                } else {
                    // delete just param
                    categoriesNotificationRequired = categoriesNotificationRequired | CmeHelper.deleteCategoryConfig(name, config);
                }
            }
        }

        if (categoriesNotificationRequired) {
            ChatService.notifyCategoriesChanged();
        }
    }

    /**
     * Update changed application options
     *
     * @param updated - updated keys
     * @param deleted - deleted keys
     * @param added - added keys
     */
    private void updateApplicationOptions(final KeyValueCollection updated, final KeyValueCollection deleted, final KeyValueCollection added) {
        try {
            // alerts section - DONE            
            CmeHelper.updateApplicationOptions(updated, deleted, added);

            // gms section - DONE
            boolean updateGmsAvailability = CmeHelper.updateGmsApplicationOptions(updated, deleted, added);
            if (updateGmsAvailability) {
                ChatService.getGmsConnector().evaluateGMSNodesAvailability();
            }
        } catch (Exception e) {
            log.error("Options update failed:", e);
        }
    }

    public static String getRoutingServiceForCategory(String categoryId) {
        ApplicationOptions options = ChatService.getApplicationOptions();
        for (Map.Entry<String, CategoryConfig> entry : options.getCategories().entrySet()) {
            CategoryConfig category = entry.getValue();
            if (category.getId().equals(categoryId)) {
                return category.getRoutingService();
            }
        }

        return null;
    }

    @Override
    public void onCMEChannelError() {
    }

    @Override
    public void onCMEChannelClosed() {
        ChatService.getMsLogger().alert(options.getAlerts().getCmeNotAvailable(), "Configuration server connection not available");
    }

    public String getApplicationTenantName() throws ConfigException, ProtocolException, InterruptedException {
        return cmeConnector.getApplicationTenantName(); //To change body of generated methods, choose Tools | Templates.
    }
}
