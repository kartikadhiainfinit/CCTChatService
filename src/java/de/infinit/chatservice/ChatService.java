package de.infinit.chatservice;

import com.genesyslab.platform.applicationblocks.com.ConfigException;
import java.util.Date;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.log4j.PropertyConfigurator;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.management.protocol.ApplicationStatus;
import de.infinit.chatservice.communication.AdapterCommunication;
import de.infinit.chatservice.communication.ResponseFactory;
import de.infinit.chatservice.communication.beans.ChatCategory;
import de.infinit.chatservice.beans.ChatServiceDef;
import de.infinit.chatservice.communication.ChatServiceCommunication;
import de.infinit.chatservice.communication.beans.ChatMessage;
import de.infinit.chatservice.communication.beans.ErrorResponse;
import de.infinit.chatservice.communication.client.*;
import de.infinit.chatservice.configuration.CMEApplication;
import de.infinit.chatservice.configuration.ConfigFilesHelper;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import de.infinit.chatservice.configuration.beans.ServiceConfig;
import de.infinit.chatservice.db.DBConnector;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.OpenChatTimeoutException;
import de.infinit.chatservice.exceptions.InvalidArgumentException;
import de.infinit.chatservice.exceptions.InvalidSessionIdException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.exceptions.OpenChatRequestFailedException;
import de.infinit.configuration.ICMEConnector;
import de.infinit.configuration.beans.CMEConnectionParameters;
import de.infinit.configuration.beans.ConnectionServerInfo;
import de.infinit.configuration.beans.ServerInfo;
import de.infinit.lca.ILCAApplication;
import de.infinit.lca.ILCAConnector;
import de.infinit.lca.LCAConnector;
import de.infinit.mslogger.MSLogger;
import de.infinit.chatservice.statistics.StatisticsWrapper;
import de.infinit.chatservice.utils.DateHelper;
import de.infinit.chatservice.utils.OpeningHoursHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;

/**
 * CCT Chat Service (REST Web Service)
 *
 * @author sebesjir
 */
@Path("/")
public class ChatService implements ServletContextListener, ILCAApplication {

    @Context
    private HttpServletRequest httpRequest;

    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChatService.class);
    private static final org.apache.log4j.Logger logVer = org.apache.log4j.Logger.getLogger("ChatServiceVersion");

    protected static CMEApplication cme;
    // Connectors
    private static ILCAConnector lca;
    private static GmsConnector gmsConnector;
    private static MSLogger msLogger;
    private static ChatApiConnector chatApi;
    private static DBConnector db;
    private static StatisticsWrapper stats;

    private static ChatService instance;

    // Application info
    private static ApplicationOptions options;

    private static String applicationName;
    private static String applicationPath;
    private static List<ConnectionServerInfo> otherChatServices;
    private static String tenantName = Constants.TENANT_NAME_DEFAULT;
    private static boolean adapterConnected = false;
    private static boolean isDebug;
    private static boolean initialized = false;
    private static boolean handshake = false;

    // Timers
    private Timer openingHourTimer;
    private static Timer pingAdapterTimer;
    private static Timer pingChatServicesTimer;
    private Timer cleanupTimer;

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------
    public static String getApplicationName() {
        return applicationName;
    }

    public static ChatService getInstance() {
        return instance;
    }

    public static List<ConnectionServerInfo> getOtherChatServices() {
        return otherChatServices;
    }

    public static MSLogger getMsLogger() {
        return msLogger;
    }

    public static CMEApplication getCme() {
        return cme;
    }

    public static GmsConnector getGmsConnector() {
        return gmsConnector;
    }

    public static StatisticsWrapper getStats() {
        return stats;
    }

    public static DBConnector getDb() {
        return db;
    }

    public static ChatApiConnector getChatApi() {
        return chatApi;
    }

    public static ApplicationOptions getApplicationOptions() {
        return options;
    }

    public static String getApplicationPath() {
        return applicationPath;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static boolean isAdapterConnected() {
        return adapterConnected;
    }

    // -------------------------------------------------------------------------
    // CW Service initialization
    // - initialize logger
    // - get application options from config server
    // - initialize LCA connection
    // -------------------------------------------------------------------------
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("contextInitialized");

        ChatService.applicationPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("*** Context destroyed call - shutdown the server ***");
        shutdownServer();
    }

    public void debugInit(String applicationPath) {
        ChatService.applicationPath = applicationPath;
        ChatService.isDebug = true;

        this.initialize();
    }

    private void initialize() {
        instance = this;

        // get log4j configuration
        initializeLogger();

        // log start info
        logInfoToBothAppenders("**************************************************************************");
        logInfoToBothAppenders("*            starting CCT Chat Service " + Constants.VERSION);
        logInfoToBothAppenders("**************************************************************************");

        // get Config. server connection parameters
        final CMEConnectionParameters cmeParams = ConfigFilesHelper.loadConfigServerConfiguration(ChatService.applicationPath, ChatService.isDebug);

        if (logVer.isInfoEnabled()) {
            logVer.info("Config. server configuration:\n" + cmeParams);
        }

        options = new ApplicationOptions();
        // start Config server connection
        if (cmeParams != null) {
            applicationName = cmeParams.getApplicationName();

            // init CME connection
            try {
                if (cme == null) {
                    cme = new CMEApplication(cmeParams);
                }
            } catch (Exception ex) {
                log.error("Cannot open connection to Config server", ex);
                return;
            }
            if (cme.isConnectionOpen()) {
                finishInitialization();
            } else {
                log.error("Config server connection is not available. Waiting for connection...");
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("initialize() - ...leaving");
        }
    }

    public void finishInitialization() {
        try {
            initialized = false;
            

            // connect to LCA and Message Server
            initializeMessaging();
            
            gmsConnector = new GmsConnector(cme);

            if (lca != null) {
                lca.changeLcaStatus(ApplicationStatus.StartPending);
            }

            cme.loadApplicationOptions(applicationName);
            if (options.getCommon().getBrandId() != 0 && options.getCommon().getChannel() != null && !"".equals(options.getCommon().getChannel())) {
                LinkedList<ChatServiceDef> chatServices = new LinkedList<>();
                chatServices.add(new ChatServiceDef(options.getCommon().getChannel(), options.getCommon().getBrandId()));
                try {
                    cme.registerServices(chatServices);
                } catch (InvalidArgumentException ex) {
                    log.warn("Registering services from CME failed in finishInitialization - InvalidArgumentException", ex);
                }
            }
            try {
                tenantName = cme.getApplicationTenantName();
            } catch (ConfigException | ProtocolException | InterruptedException ex) {
                log.warn("Cannot get tenant name, default tenant name will be used.");
            }
            otherChatServices = cme.getEndpoints(applicationName);
            db = new DBConnector(options);
            chatApi = new ChatApiConnector(applicationName, options, db);
            chatApi.Init();
            stats = new StatisticsWrapper(options, tenantName);

            initialized = true;

            // starting timers
            startCleanupTimer();
            startPingAdapterTimer();
            startPingChatServicesTimer();

            if (lca != null) {
                lca.changeLcaStatus(ApplicationStatus.Running);
            }
        } catch (ProtocolException | InterruptedException ex) {
            log.error("Error while finishInitialization", ex);
        }
    }

    /**
     * Initialize connection to LCA and MS
     */
    private void initializeMessaging() {
        if (log.isInfoEnabled()) {
            log.info("initializeMessaging() - started...");
        }

        if (cme != null && cme.isConnectionOpen()) {

            // get Application info
            ServerInfo applicationServerInfo = getApplicationServerInfo(cme.getCmeConnector(), applicationName);

            String lcaHost = Constants.LCA_HOST_DEFAULT;
            if (applicationServerInfo != null && !applicationServerInfo.getLcaPort().isEmpty()) {
                int lcaPort = Constants.LCA_PORT_DEFAULT;

                try {
                    lcaPort = Integer.parseInt(applicationServerInfo.getLcaPort());
                } catch (NumberFormatException e) {
                    log.warn("Configured LCA port [" + applicationServerInfo.getLcaPort() + "] is not a number. Default LCA port [" + Constants.LCA_PORT_DEFAULT + "] will be used.");
                }

                // initialize LCA connection
                try {
                    lca = new LCAConnector(applicationName, lcaHost, lcaPort, this);
                    if (lca != null) {
                        lca.changeLcaStatus(ApplicationStatus.Initializing);
                    }
                } catch (Exception e) {
                    log.warn("Unable to connect to LCA." + e.getMessage());
                }

                msLogger = initMsLoggerConnector(cme.getCmeConnector(), applicationName, applicationServerInfo);
            }
        } else {
            log.error("Cannot initialize LCA connection! Config. server connection is not open");
        }

        if (log.isDebugEnabled()) {
            log.debug("initializeMessaging() - ...leaving");
        }
    }

    /**
     * Initialize connection to message server
     *
     * @param cmeController
     * @param applicationName
     * @param serverInfo
     * @return
     */
    private MSLogger initMsLoggerConnector(final ICMEConnector cmeController, final String applicationName, final ServerInfo serverInfo) {
        if (log.isDebugEnabled()) {
            log.debug("Start initMsLoggerConnector...");
        }
        ConnectionServerInfo messageServerInfo = null;
        try {
            messageServerInfo = cmeController.getMessageServerInfo(applicationName);
        } catch (ProtocolException ex) {
            log.error("getStatServerInfo " + applicationName + " throws ProtocolException! \n" + ex);
        } catch (InterruptedException ex) {
            log.error("getStatServerInfo " + applicationName + " throws InterruptedException! \n" + ex);
        }
        if (messageServerInfo == null || serverInfo == null) {
            log.error("MSLogger or application " + applicationName + " not found in CME");
            return null;
        }

        return new MSLogger(applicationName, serverInfo.getDBID(), serverInfo.getHost(), messageServerInfo.getHost(), messageServerInfo.getPort(), messageServerInfo.getBackUpHost(), messageServerInfo.getBackUpPort(),
                messageServerInfo.getConnProtocol(), messageServerInfo.getTimeoutLocal(), messageServerInfo.getTimeoutRemote(), messageServerInfo.getMode(), messageServerInfo.getRedundancyType(),
                messageServerInfo.getAttempts(), messageServerInfo.getConnTimeout());
    }

    /**
     * Server shutdown. Closes all connections.
     */
    // @PreDestroy
    public void shutdownServer() {
        logInfoToBothAppenders("Starting Shutdown of Chat Service...");

        if (pingAdapterTimer != null) {
            pingAdapterTimer.cancel();
        }

        if (pingChatServicesTimer != null) {
            pingChatServicesTimer.cancel();
        }
        
        if (openingHourTimer != null) {
            openingHourTimer.cancel();
        }
        
        if (cleanupTimer != null) {
            cleanupTimer.cancel();
        }

        if (lca != null) {
            log.debug("Change LCA status to STOP PENDING");
            lca.changeLcaStatus(ApplicationStatus.StopPending);
        }

        // stop GMS reconnection thread
        if (gmsConnector != null && gmsConnector.getGmsNodeReconnectTimer() != null) {
            log.debug("Close GMS node reconnection timer");
            try {
                gmsConnector.getGmsNodeReconnectTimer().cancel();
                gmsConnector.setGmsNodeReconnectTimer(null);
            } catch (Exception e) {
                log.error("Exxception catched during shutdown of the server: " + e.getMessage());
            }
        }

        if (lca != null) {
            log.debug("Set LCA status to stopped and close LCA server connection");
            lca.changeLcaStatus(ApplicationStatus.StopPending);
        }

        // close cme connection
        if (cme != null) {
            log.debug("Close CME connection");
            try {
                cme.closeConnection();
            } catch (Exception e) {
                log.error("Exception catched during shutdown of the server: " + e.getMessage());
            }
        }

        // close message server connection
        if (msLogger != null) {
            log.debug("Close message server connection");
            try {
                msLogger.closeConnection();
            } catch (Exception e) {
                log.error("Exception catched during shutdown of the server: " + e.getMessage());
            }
        }

        // Set application status to Stopped in LCA, and close LCA connection
        if (lca != null) {
            log.debug("Set LCA status to stopped and close LCA server connection");
            lca.changeLcaStatus(ApplicationStatus.Stopped);
            lca.stopLcaConnector();
        }

        logInfoToBothAppenders("****************************************************************");
        logInfoToBothAppenders("* Shutdown of the CCT Chat Service Service completed");
        logInfoToBothAppenders("***************************************************************");
    }

    private ServerInfo getApplicationServerInfo(final ICMEConnector cmeController, final String applicationName) {
        ServerInfo serverInfo = null;
        try {
            serverInfo = cmeController.getServerInfo(applicationName);
        } catch (ProtocolException ex) {
            log.error("getApplicationServerInfo " + applicationName + " throws ProtocolException! \n" + ex);
        } catch (InterruptedException ex) {
            log.error("getApplicationServerInfo " + applicationName + " throws InterruptedException! \n" + ex);
        }
        if (serverInfo == null) {
            log.error("Application " + applicationName + " not found in CME");
        }
        return serverInfo;

    }

    /**
     * Initialize log4j - read configuration from log4j-gms.properties file
     */
    private void initializeLogger() {
        String configPath = null;
        try {
            configPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (configPath.contains("SourceCode")) {
                configPath = configPath.substring(0, configPath.lastIndexOf("SourceCode"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("Configuration/log4j-chat-service.properties");
            } else if (configPath.contains("wtpwebapps")) {
                configPath = configPath.substring(0, configPath.lastIndexOf("wtpwebapps"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("conf/log4j-chat-service.properties");
            } else {
                configPath = configPath.substring(0, configPath.lastIndexOf("webapps"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("conf/log4j-chat-service.properties");
            }

            System.out.println("Log4j Config Path:" + configPath);
            PropertyConfigurator.configure(configPath);
        } catch (Exception e) {
            System.err.println("Log4j Config error:" + e);
        }

        log.info("Log4j configured from file:" + configPath);
    }

    private void logInfoToBothAppenders(String message) {
        if (log.isInfoEnabled()) {
            log.info(message);
        }
        if (logVer.isInfoEnabled()) {
            logVer.info(message);
        }
    }

    public static void primaryAdapterAvailability(boolean availability) {
        if (adapterConnected && !availability) {
            if (chatApi != null) {
                log.info("Primary adapter is no longer available");
                chatApi.reset();
            }
        }
        if (!adapterConnected && availability) {
            log.info("Primary adapter is now available");
        }
        adapterConnected = availability;
    }

    public static void claimSessions(String serviceName) {
        getChatApi().claimSessions(serviceName);
    }

    public void startCleanupTimer() {
        if (cleanupTimer != null) {
            cleanupTimer.cancel();
        }

        if (options.getCommon().getIdleChatCleanupTime() != null && !"".equals(options.getCommon().getIdleChatCleanupTime())) {
            log.info("Set startCleanupTimer");
            String idleChatCleanupTime = options.getCommon().getIdleChatCleanupTime();
            try {
                SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date now = new Date();
                String today = dateOnly.format(now);
                Date cleanupTime = fullFormat.parse(today + " " + idleChatCleanupTime);
                if (cleanupTime.before(now)) {
                    cleanupTime = DateHelper.addDays(cleanupTime, 1);
                }
                long timeSpan = cleanupTime.getTime() - now.getTime();
                log.info("Setting cleanup timer to: " + fullFormat.format(cleanupTime));

                cleanupTimer = new Timer("cleanupTimer");
                cleanupTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            chatApi.cleanupIdleChats();
                        } catch (BackendUnavailableException | RequestFailedException ex) {
                            log.error("Error while cleanupIdleChats", ex);
                        }
                        startCleanupTimer();
                    }
                }, timeSpan);

            } catch (ParseException ex) {
                log.error("Error while parsing cleanupTime: " + idleChatCleanupTime, ex);
            } catch (Exception ex) {
                log.error("Error while setting idle cleanup", ex);
            }
        }
    }

    public void startPingAdapterTimer() {
        if (pingAdapterTimer != null) {
            pingAdapterTimer.cancel();
        }
        if (options.getCommon().getPingClientIntervalMs() > 0) {
            pingAdapterTimer = new Timer("pingAdapterTimer");
            pingAdapterTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    AdapterCommunication.pingAdapter();
                    startPingAdapterTimer();
                }
            }, options.getCommon().getPingClientIntervalMs());
        }
    }

    public void startPingChatServicesTimer() {
        final int pingChatServicesEvery_s = 30;
        if (pingChatServicesTimer != null) {
            pingChatServicesTimer.cancel();
        }

        
        Date roundedUpTime = DateHelper.roundUp(new Date(), pingChatServicesEvery_s);
        long extraSecond = 1000L; // Adding extra second just to be sure that ping will not be missed by some delay
        long pingTime = roundedUpTime.getTime() + extraSecond + options.getCommon().getMessagePostponeMs();
        
        pingChatServicesTimer = new Timer("pingChatServicesTimer");
        pingChatServicesTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ChatServiceCommunication.pingChatServices();
                startPingChatServicesTimer();
            }
        }, pingTime);
    }

    public void startOpeningHourTimer() {
        if (openingHourTimer != null) {
            openingHourTimer.cancel();
        }

        Map<String, CategoryConfig> configuredCategories = options.getCategories();
        Date nextOHChangeTime = OpeningHoursHelper.getNextCategoryChangeTime(configuredCategories);

        final Long oohChangeTime = nextOHChangeTime.getTime();

        if (nextOHChangeTime.getTime() > -1) {
            openingHourTimer = new Timer("openingHourTimer");
            openingHourTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    log.info("openingHourTimer run");

                    try {

                        String eventId = Constants.OOH_EVENT_PREFIX + oohChangeTime;
                        String processed = db.getEventProcessedInfo(eventId);
                        if (processed == null) {
                            // sendNotification
                            db.createEventHandeledEntry(eventId, "false");
                            notifyCategoriesChanged();
                            db.createEventHandeledEntry(eventId, "true");
                        }
                        //                if (!isMessageProcessed) {
                        //                    notifyCategoriesChanged();
                        //                }
                    } catch (BackendUnavailableException | RequestFailedException ex) {
                        log.warn("Error when sending a opening hours notification", ex);
                        notifyCategoriesChanged();
                    }
                    startOpeningHourTimer();
                }
            },
                    new Date(nextOHChangeTime.getTime() + options.getCommon().getMessagePostponeMs()));
        }
    }

    public static void notifyCategoriesChanged() {
        AdapterCommunication.categoriesChanged();
    }

    public static void notifyServiceAvailabilityChanged() {
        AdapterCommunication.serviceAvailabilityChanged();
    }

    // --------------------------------------------------------------------------
    // Ping
    // --------------------------------------------------------------------------
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping() {
        if (log.isInfoEnabled()) {
            log.info("ChatService: Received 'ping' request at " + new Date().toString());
        }

        if (options != null && options.getCommon().isEnabled() && initialized && chatApi.isGmsConnected()) {
            return Response.ok().build();
        } else {
            log.warn("Ping response suppressed, see gms/disable_ping application option");
            return Response.ok().status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    // --------------------------------------------------------------------------
    // Ping from ChatService
    // --------------------------------------------------------------------------
    @GET
    @Path("/pingFromChatService")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pingFromChatService() {
        if (log.isInfoEnabled()) {
            log.info("ChatService: Received 'pingFromChatService' request at " + new Date().toString());
        }

        // Always ok without handshake
        if (!handshake || (options != null && options.getCommon().isEnabled() && adapterConnected)) {
            return Response.ok().build();
        } else {
            log.warn("Ping response suppressed, see gms/disable_ping application option");
            return Response.ok().status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    // --------------------------------------------------------------------------
    // registerAdapter
    // --------------------------------------------------------------------------
    @POST
    @Path("/handshake")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handshake(ClientHandshakeRequest request) {
        try {
            cme.registerServices(request.getChatServices());
            handshake = true;
        } catch (InvalidArgumentException ex) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidArgumentException - service not found: " + ex.getMessage());
            return Response.status(500).entity(errorResponse).build();
        } catch (Exception ex) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }

        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // claimSessionAsPrimary
    // --------------------------------------------------------------------------
    @GET
    @Path("/claimSessionAsPrimary/{sessionId}")
    public Response claimSessionAsPrimary(@PathParam("sessionId") String sessionId) {
        log.info("[requestSessionAsPrimary] - SessionId: " + sessionId);
        getChatApi().chatSessionClaimed(sessionId);
        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // test
    // --------------------------------------------------------------------------
    @GET
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() {
        try {
            ChatServiceCommunication.claimSessionAsPrimary("123456");
        } catch (Exception ex) {
            Response.serverError().build();
        }

        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // finishConversation
    // --------------------------------------------------------------------------
    @POST
    @Path("/chat/{sessionId}/finish")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response finishConversation(@PathParam("sessionId") String sessionId) {
        long startTime = logRequest("finishConversation", sessionId);
        if (!handshake) {
            log.error("[finishConversation] Handshake required");
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Handshake required");
            return Response.status(560).entity(errorResponse).build();
        }

        try {
            Map<String, String> update = new HashMap<>();
            update.put(GmsDataConstants.KV_CP_CLOSED, GmsDataConstants.KV_CP_CLOSED_VALUE);
            chatApi.updateUserData(sessionId, update);
        } catch (Exception ex) {
            log.error("[" + sessionId + "] " + GmsDataConstants.KV_CP_CLOSED + " userdata update failed", ex);
        }

        int responseStatus = 200;
        try {
            chatApi.disconnect(sessionId);
        } catch (InvalidSessionIdException ex) {
            log.error("[finishConversation] InvalidSessionIdException catched. HTTP Status 553 will be returned", ex);
            responseStatus = 553;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidSessionIdException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (InvalidArgumentException ex) {
            log.error("[finishConversation] InvalidArgumentException catched. HTTP Status 554 will be returned", ex);
            responseStatus = 554;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidArgumentException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (BackendUnavailableException ex) {
            log.error("[finishConversation] BackendUnavailableException catched. HTTP Status 500 will be returned", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("BackendUnavailableException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (Exception ex) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.error("[finishConversation] Exception catched. HTTP Status 500 will be returned", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } finally {
            logResponse("finishConversation", startTime, null, responseStatus);
        }

        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // getServiceAvailability
    // --------------------------------------------------------------------------
    @POST
    @Path("/serviceAvailability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceAvailability(ClientServiceAvailabilityRequest request) {
        long startTime = logRequest("getServiceAvailability", null, "request: " + request);
        if (!handshake) {
            log.error("[getServiceAvailability] Handshake required");
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Handshake required");
            return Response.status(560).entity(errorResponse).build();
        }

        ClientGetServiceAvailabilityResponse response = new ClientGetServiceAvailabilityResponse();
        int responseStatus = 200;
        try {
            response.setServiceAvailabilities(ResponseFactory.getServiceAvailabilities(request.getChatServices()));
        } catch (BackendUnavailableException ex) {
            log.error("BackendUnavailableException catched during getServiceAvailability", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("BackendUnavailableException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (Exception ex) {
            // including RequestFailedException from Statistics
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.error("Exception catched during getServiceAvailability", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } finally {
            logResponse("getServiceAvailability", startTime, response, responseStatus);
        }

        return Response.ok(response).build();
    }

    // --------------------------------------------------------------------------
    // getSupportCategories
    // --------------------------------------------------------------------------
    @GET
    @Path("/category")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSupportCategories() {
        long startTime = logRequest("getSupportCategories", null, null);
        if (!handshake) {
            log.error("[getSupportCategories] Handshake required");
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Handshake required");
            return Response.status(560).entity(errorResponse).build();
        }

        ClientGetCategoriesResponse response = new ClientGetCategoriesResponse();
        int responseStatus = 200;
        try {
            Map<String, CategoryConfig> configuredCategories = options.getCategories();
            LinkedList<ChatCategory> categories = OpeningHoursHelper.evaluateCategoryAvailability(configuredCategories);
            if (openingHourTimer == null) {
                startOpeningHourTimer();
            }
            response.setChatCategories(categories);
        } catch (Exception ex) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.error("[getSupportCategories] exception catched.", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } finally {
            logResponse("getSupportCategories", startTime, response, responseStatus);
        }
        return Response.ok(response).build();
    }

    // --------------------------------------------------------------------------
    // sendMessage
    // --------------------------------------------------------------------------
    @POST
    @Path("/chat/{sessionId}/sendMessage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessage(@PathParam("sessionId") String sessionId, ClientSendMessageRequest request) {
        long startTime = logRequest("sendMessage", sessionId, request);
        if (!handshake) {
            log.error("[sendMessage] Handshake required");
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Handshake required");
            return Response.status(560).entity(errorResponse).build();
        }

        int responseStatus = 200;
        try {
            int maxMessageLength = options.getCommon().getMaxClientMessageLength();
            for (ChatMessage message : request.getChatMessages()) {
                if (message.getText().length() > maxMessageLength) {
                    log.warn("Message length exceeded limit. It was truncated to " + maxMessageLength + " chars. Orig message was: " + message.getText());
                    message.setText(message.getText().substring(0, maxMessageLength));
                }
            }
            getChatApi().sendMessage(sessionId, request);
        } catch (InvalidSessionIdException ex) {
            log.error("[sendMessage] InvalidSessionIdException catched", ex);
            responseStatus = 553;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidSessionIdException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (InvalidArgumentException ex) {
            log.error("[sendMessage] InvalidArgumentException catched", ex);
            responseStatus = 554;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidArgumentException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (BackendUnavailableException ex) {
            log.error("[sendMessage] BackendUnavailableException catched", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("BackendUnavailableException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (Exception ex) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.error("[sendMessage] Exception catched", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } finally {
            logResponse("sendMessage", startTime, null, responseStatus);
        }

        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // requestStartConversation
    // --------------------------------------------------------------------------
    @POST
    @Path("/chat/{sessionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestStartConversation(@PathParam("sessionId") String sessionId, ClientStartChatRequest request) {
        long startTime = logRequest("requestStartConversation", sessionId, request);
        if (log.isInfoEnabled()) {
            log.info("KPICM101: Chat Message: openChat");
            log.info("[requestStartConversation] Input: sessionId=" + sessionId + ", request=" + request);
        }
        if (!handshake) {
            log.error("[getSupportCategories] Handshake required");
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Handshake required");
            return Response.status(560).entity(errorResponse).build();
        }

        int responseStatus = 200;
        try {
            // Chat available
            boolean serviceFound = false;
            Collection<ServiceConfig> services = options.getRegisteredServices().isEmpty() ? options.getServices().values() : options.getRegisteredServices();
            for (ServiceConfig service : services) {
                if (service.getBrandId() == request.getBrandId() && service.getChannel().equalsIgnoreCase(request.getChannel())) {
                    serviceFound = true;
                    if (!service.isAvailable()) {
                        log.warn("Service is not available");
                        responseStatus = 551;
                        ErrorResponse errorResponse = new ErrorResponse();
                        errorResponse.setErrorMessage("Service is not available");
                        return Response.status(responseStatus).entity(errorResponse).build();
                    }
                }
            }
            if (!serviceFound) {
                log.warn("Service is not found");
                responseStatus = 500;
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setErrorMessage("Service is not found");
                return Response.status(responseStatus).entity(errorResponse).build();
            }

            int numberOfChatsAvailable = stats.getNumberOfChatsAvailable();
            if (numberOfChatsAvailable < 1) {
                // No chat slot available
                log.warn("Cannot create chat. All chat slots are used");
                responseStatus = 551;
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setErrorMessage("Cannot create chat. All chat slots are used");
                return Response.status(responseStatus).entity(errorResponse).build();
            }

            // Category available
            if (request.getCategoryId() == null || "".equals(request.getCategoryId())) {
                log.error("CategoryId is null");
                responseStatus = 554;
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setErrorMessage("CategoryId is null");
                return Response.status(responseStatus).entity(errorResponse).build();
            }
            Map<String, CategoryConfig> configuredCategories = options.getCategories();
            ChatCategory category = OpeningHoursHelper.evaluateCategoryAvailability(configuredCategories, request.getCategoryId());

            if (category == null) {
                log.error("Required category not found");
                log.info("KPITE502: TechnicalException: InvalidArgumentException.");
                responseStatus = 554;
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setErrorMessage("Required category not found");
                return Response.status(responseStatus).entity(errorResponse).build();
            } else if (!category.isAvailable()) {
                log.warn("Category " + category.getName() + "(" + category.getId() + ") is not available");
                responseStatus = 552;
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setErrorMessage("Category " + category.getName() + "(" + category.getId() + ") is not available");
                return Response.status(responseStatus).entity(errorResponse).build();
            }

            // Open chat
            getChatApi().openChat(sessionId, request);

        } catch (OpenChatRequestFailedException ex) {
            log.error("[requestStartConversation] OpenChatRequestFailedException catched", ex);
            responseStatus = 554;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("OpenChatRequestFailedException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (OpenChatTimeoutException ex) {
            log.error("[requestStartConversation] OpenChatTimeoutException catched", ex);
            responseStatus = 554;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("OpenChatTimeoutException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (InvalidArgumentException ex) {
            log.error("[requestStartConversation] InvalidArgumentException catched", ex);
            responseStatus = 554;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("InvalidArgumentException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (BackendUnavailableException ex) {
            log.error("[requestStartConversation] BackendUnavailableException catched", ex);
            // start reconnection 
            gmsConnector.evaluateGMSNodesAvailability();
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("BackendUnavailableException: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } catch (Exception ex) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.error("[requestStartConversation] Exception catched", ex);
            responseStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Exception: " + ex.getMessage());
            return Response.status(responseStatus).entity(errorResponse).build();
        } finally {
            logResponse("requestStartConversation", startTime, null, responseStatus);
        }

        return Response.ok().build();
    }

    // --------------------------------------------------------------------------
    // Private
    // --------------------------------------------------------------------------
    private long logRequest(final String methodName, final String sessionId, final Object request) {
        long startTime = 0L;
        if (log.isInfoEnabled()) {
            log.info("[" + methodName + "] Input: sessionId=" + (sessionId != null ? sessionId : "") + ", Request=" + (request != null ? request : "NULL"));
            startTime = Calendar.getInstance().getTimeInMillis();
        }

        return startTime;
    }

    private long logRequest(final String methodName, final String sessionId) {
        long startTime = 0L;
        if (log.isInfoEnabled()) {
            log.info("[" + methodName + "] Input: sessionId=" + (sessionId != null ? sessionId : ""));
            startTime = Calendar.getInstance().getTimeInMillis();
        }
        return startTime;
    }

    private void logResponse(String methodName, long startTime, Object response, Integer status) {
        if (log.isInfoEnabled()) {
            final long processTime = Calendar.getInstance().getTimeInMillis() - startTime;
            log.info("[" + methodName + "] Output:" + response + ",HTTP Status=" + status + ", Duration= " + processTime + " ms");
        }
    }

    // --------------------------------------------------------------------------
    // Handlers
    // --------------------------------------------------------------------------
    @Override
    public void exitApplication() {
        shutdownServer();
    }

    @Override
    public void onLCAChannelOpened() {
        lca.changeLcaStatus(ApplicationStatus.Running);
    }

}
