package de.infinit.chatservice;

import com.genesyslab.platform.commons.protocol.ProtocolException;
import de.infinit.chatservice.configuration.CMEApplication;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.configuration.beans.ConnectionServerInfo;
import de.infinit.mslogger.MSLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author xxbamar
 */
public class GmsConnector {
    
     public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GmsConnector.class);

    private String gmsNodeURL = null;
    private String primaryGmsNodeURL = null;
    private final List<String> backupGmsNodesURL = new ArrayList<>();

    // Reconnection to primary node
    private Timer gmsNodeReconnectTimer;
    private boolean gmsNodeReconnectTimerStarted = false;

    private final ApplicationOptions options;
    private final MSLogger msLogger;

    public GmsConnector(CMEApplication cme) {
        this.options = ChatService.getApplicationOptions();
        this.msLogger = ChatService.getMsLogger();
    }

    public String getGmsNodeUrl() {
        return gmsNodeURL;
    }

    public String getPrimaryGmsNodeURL() {
        return primaryGmsNodeURL;
    }

    public List<String> getBackupGmsNodesURL() {
        return backupGmsNodesURL;
    }

    public Timer getGmsNodeReconnectTimer() {
        return gmsNodeReconnectTimer;
    }

    public boolean isGmsNodeReconnectTimerStarted() {
        return gmsNodeReconnectTimerStarted;
    }

    public void setGmsNodeReconnectTimer(Timer gmsNodeReconnectTimer) {
        this.gmsNodeReconnectTimer = gmsNodeReconnectTimer;
    }

    public String getBaseStatisticUrl() {
        String gmsNodeUrl = getGmsNodeUrl();
        if (gmsNodeUrl == null || gmsNodeUrl.isEmpty()) {
            return null;
        }
        return gmsNodeUrl + "/1/statistic";
    }

    public String getBaseDBUrl() {
        String gmsNodeUrl = getGmsNodeUrl();
        if (gmsNodeUrl == null || gmsNodeUrl.isEmpty()) {
            return null;
        }
        return gmsNodeUrl + "/1/storage";
    }

    public String getChatApiUrl() {
        String gmsNodeUrl = getGmsNodeUrl();
        if (gmsNodeUrl == null || gmsNodeUrl.isEmpty()) {
            return null;
        }
        return gmsNodeUrl + "/cometd";
    }

    /**
     * Read GMS nodes from Connection tab of the application
     */
    public void readGMSNodesConnection() {
        if (log.isInfoEnabled()) {
            log.info("readGMSNodesConnection() - started...");
        }

        String primaryGmsNodeName = options.getGms().getGmsPrimaryNodeName();
        log.info("Primary GMS node is set to " + primaryGmsNodeName);
        try {
            List<ConnectionServerInfo> gmsNodes = ChatService.getCme().getGmsNodes(ChatService.getApplicationName());
            for (ConnectionServerInfo gmsNodeInfo : gmsNodes) {
                log.info("Callback Service is connected to GMS Node:" + gmsNodeInfo.getName() + " [" + gmsNodeInfo.getHost() + ":" + gmsNodeInfo.getPort() + "]");

                if (primaryGmsNodeName != null && primaryGmsNodeName.equals(gmsNodeInfo.getName())) {
                    primaryGmsNodeURL = "http://" + gmsNodeInfo.getHost() + ":" + gmsNodeInfo.getPort() + "/genesys";
                } else {
                    backupGmsNodesURL.add("http://" + gmsNodeInfo.getHost() + ":" + gmsNodeInfo.getPort() + "/genesys");
                }
            }
        } catch (ProtocolException e) {
            log.error("ProtocolException catched during reading of GMS nodes connections", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException catched during reading of GMS nodes connections", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("readGMSNodesConnection() - ...leaving");
        }
    }

    /**
     * Evaluate GMS Nodes availability, start reconnection thread and set appropriate alerts to message server if needed
     */
    public void evaluateGMSNodesAvailability() {
        if (log.isInfoEnabled()) {
            log.info("evaluateGMSNodesAvailability() - started...");
        }

        String result = null;

        if (options != null) {
            String gmsUsername = options.getGms().getGmsUser();
            String gmsPassword = options.getGms().getGmsPassword();

            if (isGmsNodeOnline(primaryGmsNodeURL, gmsUsername, gmsPassword)) {
                if (log.isInfoEnabled()) {
                    log.info("Primary GMS Node is AVAILABLE");
                }

                result = primaryGmsNodeURL;

                if (msLogger != null) {
                    msLogger.alert(options.getAlerts().getGmsNodesReconnect(), "GMS Node [" + result + "] is ONLINE");
                    msLogger.alert(options.getAlerts().getPrimaryGmsReconnect(), "Primary GMS Node [" + options.getGms().getGmsPrimaryNodeName() + "] is ONLINE");
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Primary GMS Node is NOT AVAILABLE");
                }

                boolean reconnectOnlyPrimary = true;
                result = getAvailableGMSNode();
                if (result != null) {
                    if (log.isInfoEnabled()) {
                        log.info("Backup GMS Node is AVAILABLE");
                    }
                    if (msLogger != null) {
                        msLogger.alert(options.getAlerts().getGmsNodesReconnect(), "GMS Node [" + result + "] is ONLINE");
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("All GMS nodes are OFFLINE");
                    }
                    if (msLogger != null) {
                        msLogger.alert(options.getAlerts().getGmsNodesNotAvailable(), "All GMS nodes are OFFLINE");
                    }
                    reconnectOnlyPrimary = false;
                }

                // start reconnect timer
                gmsNodeReconnectTimerStarted = false;
                startGmsReconnectionThread(reconnectOnlyPrimary);
            }
        } else {
            log.warn("Application options are not loaded, cannot initiate GMS nodes connection");
        }

        gmsNodeURL = result;

        if (result != null && ChatService.getChatApi() != null) {
            ChatService.getChatApi().reconnect();
        }

        if (log.isInfoEnabled()) {
            log.info("GMS Node with URL " + gmsNodeURL + " will be used in Chat Service " + ChatService.getApplicationName());
        }
    }

    /**
     * Get available GMS Node URL
     *
     * @return - URL of the GMS Node which is in ONLINE state
     */
    public String getAvailableGMSNode() {

        String gmsUsername = options.getGms().getGmsUser();
        String gmsPassword = options.getGms().getGmsPassword();

        if (isGmsNodeOnline(primaryGmsNodeURL, gmsUsername, gmsPassword)) {
            return primaryGmsNodeURL;
        } else {
            for (String backupUrl : backupGmsNodesURL) {
                if (isGmsNodeOnline(backupUrl, gmsUsername, gmsPassword)) {
                    if (msLogger != null) {
                        msLogger.alert(options.getAlerts().getPrimaryGmsNotAvailable(), "Primary GMS Node [" + options.getGms().getGmsPrimaryNodeName() + "] is OFFLINE");
                    }

                    return backupUrl;
                }
            }
        }

        return null;
    }

    /**
     * Task for primary GMS node reconnection
     *
     * @author sebesjir
     */
    public class ReconnectPrimaryGmsNodeTask extends TimerTask {

        @Override
        public void run() {
            String gmsUsername = options.getGms().getGmsUser();
            String gmsPassword = options.getGms().getGmsPassword();

            if (isGmsNodeOnline(primaryGmsNodeURL, gmsUsername, gmsPassword)) {
                gmsNodeReconnectTimerStarted = false;
                gmsNodeURL = primaryGmsNodeURL;

                if (log.isInfoEnabled()) {
                    log.info("Primary GMS Node is AVAILABLE");
                }

                if (msLogger != null) {
                    msLogger.alert(options.getAlerts().getPrimaryGmsReconnect(), "Primary GMS Node [" + options.getGms().getGmsPrimaryNodeName() + "] reconnected");
                }

                if (gmsNodeURL != null && ChatService.getChatApi() != null) {
                    ChatService.getChatApi().reconnect();
                }
            } else {
                gmsNodeReconnectTimerStarted = false;
                startGmsReconnectionThread(true);
            }
        }
    }

    /**
     * Task for any GMS node reconnection
     *
     * @author sebesjir
     */
    public class ReconnectAnyGmsNodeTask extends TimerTask {

        @Override
        public void run() {
            evaluateGMSNodesAvailability();
        }
    }

    /**
     * Start GMS Node reconnection
     *
     * @param onlyPrimary - true: Reconnection only for primary GMS Node, - false: Reconnection to any GMS Node
     */
    private void startGmsReconnectionThread(boolean onlyPrimary) {
        if (log.isInfoEnabled()) {
            log.info("startGmsReconnectionThread(onlyPrimary=" + onlyPrimary + ") entered...");
        }

        if (!gmsNodeReconnectTimerStarted) {
            stopGmsReconnectionThread();
            gmsNodeReconnectTimerStarted = true;

            String timerName = "CW_GMS_RECONNECT_TIMER";
            if (log.isDebugEnabled()) {
                log.debug("initialize GMS reconnect timer with name: " + timerName);
            }

            gmsNodeReconnectTimer = new Timer(timerName);

            TimerTask task;
            if (onlyPrimary) {
                task = new ReconnectPrimaryGmsNodeTask();
            } else {
                task = new ReconnectAnyGmsNodeTask();
            }

            gmsNodeReconnectTimer.schedule(task, Constants.GMS_RECONNECTION_TIMEOUT_SEC * 1000);
        }
        if (log.isDebugEnabled()) {
            log.info("Leaving startGmsReconnectionThread()");
        }
    }

    /**
     * Stop GMS Node reconnection thread
     */
    private void stopGmsReconnectionThread() {
        if (log.isDebugEnabled()) {
            log.debug("stopGmsReconnectionThread() entered...");
        }

        if (gmsNodeReconnectTimer != null) {
            gmsNodeReconnectTimerStarted = false;
            gmsNodeReconnectTimer.cancel();
        }

        if (log.isDebugEnabled()) {
            log.debug("leaving stopGmsReconnectionThread()");
        }
    }

    /**
     * Use Node API to get Node availability (online/offline)
     *
     * @param nodeUrl - url of the GMS node
     * @param username - username for GMS node authorization
     * @param password - password for GMS node authorization
     * @return true if the status of the GMS node is ONLINE
     */
    public boolean isGmsNodeOnline(final String nodeUrl, final String username, final String password) {
        try {
            HttpAuthenticationFeature feature = null;
            if (username != null && password != null) {
                feature = HttpAuthenticationFeature.basic(username, password);
            }

            Client client = ClientBuilder.newClient(new ClientConfig());
            if (feature != null) {
                client.register(feature);
            }

            WebTarget t = client.target(nodeUrl + "/1/admin/node/status");
            Response resp = t.request().get();
            log.debug("CWR: Response: " + resp);

            if (resp.getStatus() == Response.Status.OK.getStatusCode()) {
                String responseEntity = resp.readEntity(String.class);
                if ("ONLINE".equalsIgnoreCase(responseEntity)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.warn("GMS Node " + nodeUrl + " is OFFLINE " + ex.getMessage());
            return false;
        }

        log.warn("GMS Node " + nodeUrl + " is OFFLINE");
        return false;
    }
}
