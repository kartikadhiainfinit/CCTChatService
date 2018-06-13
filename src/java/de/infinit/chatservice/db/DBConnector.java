package de.infinit.chatservice.db;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.InvalidSessionIdException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.beans.ChatSessionInfo;
import de.infinit.chatservice.beans.StatusEnum;
import de.infinit.chatservice.communication.ChatServiceCommunication;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.utils.JsonHelper;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

public class DBConnector {

    private static final Logger log = Logger.getLogger(DBConnector.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private ConcurrentHashMap<String, ChatSessionInfo> sessionCache;

    private final ApplicationOptions options;

    /**
     * DBConnector - uses GMS storage API to store the chat interaction related
     * data
     *
     * @param options
     * @param dbURL
     * @param gmsUser
     * @param gmsPassword
     */
    public DBConnector(final ApplicationOptions options) {
        if (log.isInfoEnabled()) {
            log.info("[DBConnector] Input: configuration=[not logged]");
        }

        this.options = options;
        this.sessionCache = new ConcurrentHashMap<>();
    }

    /**
     * // * Create chat Info in the GMS Cassandra using storage API // * //
     *
     *
     * @param session
     * @throws de.infinit.chatservice.exceptions.BackendUnavailableException
     */
    public void storeSession(final ChatSessionInfo session) throws BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[storeSession] Input: session=" + session);
        }

        if (session == null) {
            log.warn("Cannot create chat session info. Session object is null.");
            throw new IllegalStateException("Session can't be a null");
        }

        session.setModifiedTime(new Date());

        if (ChatService.getGmsConnector().getBaseDBUrl() == null) {
            throw new BackendUnavailableException("GMS DB Url is null");
        }

        Client client = getClient();
        WebTarget t = client.target(ChatService.getGmsConnector().getBaseDBUrl() + "/session" + session.getSessionId() + "/" + options.getDatabase().getTtl());

        if (log.isDebugEnabled()) {
            log.debug("GMS Storage API insert for " + session.getSessionId() + " secureKey=" + session.getSecureKey());
        }

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        if (session.getUserId() != null) {
            formData.add(DBConstants.COLUMN_USER_ID, session.getUserId());
        }
        formData.add(DBConstants.COLUMN_SESSION_ID, String.valueOf(session.getSessionId()));
        if (session.getSecureKey() != null) {
            formData.add(DBConstants.COLUMN_SECURE_KEY, session.getSecureKey());
        }
        formData.add(DBConstants.COLUMN_NEXT_POSITION, String.valueOf(session.getNextPosition()));
        formData.add(DBConstants.COLUMN_LAST_ACCESS, String.valueOf(session.getModifiedTime().getTime()));
        if (session.getStatus() != null) {
            formData.add(DBConstants.COLUMN_STATUS, session.getStatus().toString());
        }
        if (session.getDisconnectReason() != null) {
            formData.add(DBConstants.COLUMN_DISCONNECT_REASON, session.getDisconnectReason());
        }
        if (session.getChatServiceInstances() != null) {
            StringBuilder sbWawInstances = new StringBuilder();
            for (String instance : session.getChatServiceInstances()) {
                sbWawInstances.append(instance).append(",");
            }

            if (sbWawInstances.length() > 0) {
                formData.add(DBConstants.COLUMN_CHATSERVICE_INSTANCES, sbWawInstances.toString());
            }
        }

        Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).post(Entity.form(formData));

        try {

            // Respond depending on status
            if (resp.getStatus() == Status.OK.getStatusCode()) {
                if (log.isDebugEnabled()) {
                    log.debug("Store or update data in cassandra is successful. Session: " + session.toString());
                    sessionCache.put(session.getSessionId(), session);
                    // store updated list to Cassandra
                    storeSessionListToCassandra();
                }
            } else {
                // Error
                log.warn("Store or update data in cassandra was not successful. Response HTTP Status is " + resp.getStatus() + " and response is " + resp);

                // TODO: Maybe we have to distinguish based on status to throw backend unavailable or request failed exception
                throw new BackendUnavailableException("Store or update data in cassandra was not successful.");
            }
        } catch (Exception ex) {
            log.warn("Store or update data in cassandra failed. " + ex.getMessage());
            throw new BackendUnavailableException("Store or update data in cassandra was not successful.");
        }

        if (log.isDebugEnabled()) {
            log.debug("[storeSession] Output: void");
        }
    }

    /**
     * Get chat session info from GMS Cassandra
     *
     * @param sessionId
     * @return
     * @throws InvalidSessionIdException
     * @throws BackendUnavailableException
     * @throws RequestFailedException
     */
    public ChatSessionInfo getSession(final String sessionId) throws InvalidSessionIdException, BackendUnavailableException, RequestFailedException {
        if (log.isDebugEnabled()) {
            log.debug("[getSession] Input: sessionId=" + sessionId);
        }

        if (sessionId == null || sessionId.trim().length() == 0) {
            if (log.isInfoEnabled()) {
                log.info("The sessionId can't be empty!");
                log.info("KPITE503: TechnicalException: InvalidChatSessionIdException.");
            }
            throw new InvalidSessionIdException("The session can't be empty");
        }

        if (sessionCache.containsKey(sessionId)) {
            ChatSessionInfo result = sessionCache.get(sessionId);
            return result;
        }
        if (log.isInfoEnabled()) {
            log.info("Session id " + sessionId + " not found in local cache. Retrieve data from Cassandra DB.");
        }

        ChatSessionInfo session = loadSessionFromCassandra(sessionId);
        if (session != null) {
            // Claim session from other chat services
            ChatServiceCommunication.claimSessionAsPrimary(sessionId);

            // Set primary
            session.addPrimaryChatService(ChatService.getApplicationName());
            sessionCache.put(sessionId, session);

            // Store session with updated primary chat service to Cassandra
            storeSession(session);

            // Store updated session list to Cassandra
            storeSessionListToCassandra();

            // register for notificatons
            ChatService.getChatApi().requestNotifications(session);
        }

        if (log.isDebugEnabled()) {
            log.debug("[getSession] Output: " + session);
        }
        return session;
    }

    public void deleteSession(final ChatSessionInfo session) throws BackendUnavailableException, RequestFailedException {
        if (log.isDebugEnabled()) {
            log.debug("[deleteSession] Input: Session=" + session);
        }

        if (session == null) {
            log.warn("Not able to delete data from local cache, Cassandra. Session is null.");
            throw new IllegalStateException("Session can't be a null");
        }

        if (sessionCache.containsKey(session.getSessionId())) {
            sessionCache.remove(session.getSessionId());
            if (log.isInfoEnabled()) {
                log.info("Chat session deleted (" + session + ") from cache.");
            }
        }

        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        if (dbURL == null) {
            log.info("Delete data from cassandra is not successful. ");
            return;
        }

        Client client = getClient();

        WebTarget t = client.target(dbURL + "/session" + session.getSessionId());

        log.info("[Cassandra] Delete for " + session.getSessionId());
        try {
            Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).delete();

            if (resp.getStatus() == Status.OK.getStatusCode()) {
                log.debug("[" + session.getSessionId() + "] Deletion of the chat data from GMS cassandra was successfull.");
            } else {
                log.warn("[" + session.getSessionId() + "] Delete data from GMS cassandra was not successful. [" + resp.getStatus() + "]");
            }
        } catch (Exception ex) {
            log.warn("[" + session.getSessionId() + "] Delete data from GMS cassandra was not successful. " + ex.getMessage());
        }

        // store updated session list to Cassandra
        storeSessionListToCassandra();

        if (log.isInfoEnabled()) {
            log.info("Chat session deleted (" + session + ") from Cassandra.");
        }
    }

    public void clearSessionCache() {
        log.info("Clearing session cache");
        sessionCache.clear();
    }

    public void restoreSessions(String chatServiceName) {
        if (log.isDebugEnabled()) {
            log.debug("[restoreSessionsForCurrentChatService]");
        }

        Client client = getClient();
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        WebTarget t = client.target(dbURL + "/chatService-" + chatServiceName);

        try {
            Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).get();

            if (resp.getStatus() == Status.OK.getStatusCode()) {

                JsonNode responseRootNode = JsonHelper.getJsonRootNode(resp.readEntity(String.class));

                JsonNode sessionIdNode = responseRootNode.get(DBConstants.CHAT_SERVICE_SESSION_LIST);
                if (sessionIdNode == null) {
                    log.debug("Cannot load chat service list from JsonRootNode");
                } else {

                    log.debug("Session list loading was successfull");
                    String sessionIdList = sessionIdNode.textValue();

                    String[] sessionIds = sessionIdList.split(",");
                    // Load sessions from Cassandra. But only primary ones
                    for (String sessionId : sessionIds) {
                        if (!"".equals(sessionId)) {
                            ChatSessionInfo session = loadSessionFromCassandra(sessionId);
                            if (session != null && !session.isChatServiceInstancesListEmpty()) {
                                // add to local cache
                                sessionCache.put(sessionId, session);

                                // register for notificatons
                                ChatService.getChatApi().requestNotifications(session);

                                // Claim if not mine
                                if (!session.getPrimaryChatService().equals(ChatService.getApplicationName())) {
                                    // Claim session from another chat service
                                    session.addPrimaryChatService(chatServiceName);
                                    storeSession(session);
                                }
                            }
                        }
                    }

                    // Storing updated sessionList to Cassandra
                    storeSessionListToCassandra();
                }
            } else {
                log.warn("Session list loading failed");
            }
        } catch (Exception ex) {
            log.error("Session list loading exception", ex);
        }
    }

    public void removeSessionFromCurrentChatService(String sessionId) {
        if (sessionId == null || "".equals(sessionId)) {
            log.warn("sessionId is empty. Cannot remove from current ChatService");
        }

        if (sessionCache.containsKey(sessionId)) {
            log.info("[removeSessionFromCurrentChatService] removing session " + sessionId + " from cache and storing updated session list");
            sessionCache.remove(sessionId);
            storeSessionListToCassandra();
        }
    }

    public ConcurrentHashMap<String, ChatSessionInfo> getSessions() {
        return sessionCache;
    }

    // Methods for event map
    public void createEventHandeledEntry(final String eventId, final String processed) throws BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[createEventHandeledEntry] Input: eventId=" + eventId);
        }

        if (eventId == null) {
            log.warn("Cannot create chat event handeled entry. EventId object is null.");
            throw new IllegalStateException("EventId can't be a null");
        }
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        if (ChatService.getGmsConnector().getBaseDBUrl() == null) {
            throw new BackendUnavailableException("GMS DB Url is null");
        }

        Client client = getClient();
        WebTarget t = client.target(dbURL + "/event" + eventId + "/" + DBConstants.EVENT_CACHE_TIMEOUT);

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(DBConstants.EVENT_CACHE_PROCESSED, processed);
        Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).post(Entity.form(formData));

        try {
            if (resp.getStatus() == Status.OK.getStatusCode()) {
                if (log.isDebugEnabled()) {
                    log.debug("Store or update data in cassandra is successful. EventId: " + eventId);
                }
            } else {
                log.warn("Store or update data in cassandra was not successful. Response HTTP Status is " + resp.getStatus() + " and response is " + resp);
                throw new BackendUnavailableException("Store or update data in cassandra was not successful.");
            }
        } catch (Exception ex) {
            log.warn("Store or update data in cassandra failed. " + ex.getMessage());
            throw new BackendUnavailableException("Store or update data in cassandra was not successful.");
        }

        if (log.isDebugEnabled()) {
            log.debug("[createEventHandeledEntry] Output: void");
        }
    }

    public String getEventProcessedInfo(final String eventId) throws BackendUnavailableException, RequestFailedException {
        if (log.isDebugEnabled()) {
            log.debug("[getEventProcessedInfo] Input: eventId=" + eventId);
        }

        if (eventId == null) {
            if (log.isInfoEnabled()) {
                log.info("The eventId can't be empty!");
            }
            return null;
        }
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        if (ChatService.getGmsConnector().getBaseDBUrl() == null) {
            log.warn("Read data from cassandra is not successful (init).");
            throw new BackendUnavailableException("Read data from cassandra is not successful (init).");
        }

        Client client = getClient();
        WebTarget t = client.target(dbURL + "/event" + eventId);
        Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).get();

        String processed = null;

        JsonNode responseRootNode;
        try {
            responseRootNode = JsonHelper.getJsonRootNode(resp.readEntity(String.class));

            if (resp.getStatus() == Status.OK.getStatusCode()) {
                JsonNode processedNode = responseRootNode.get(DBConstants.EVENT_CACHE_PROCESSED);
                if (processedNode != null) {
                    processed = processedNode.textValue();
                }

                if (log.isInfoEnabled()) {
                    log.info("Gets data from Cassandra. EventId: " + eventId + ":" + processed);
                }
            } else {
                log.warn("Requesting of session was not successfull. Response HTTP Status is " + resp.getStatus() + " and response is " + resp);
                if (resp.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    throw new BackendUnavailableException("It was not possible to retrieve data from GMS Cassandra. HTTP Status " + resp.getStatus());
                } else {
                    return null;
                }
            }
        } catch (IOException ex) {
            log.warn("Requestiong data from cassandra was not successful. " + ex.getMessage());
            throw new BackendUnavailableException("Requestiong data from cassandra was not successful.");
        }

        if (log.isDebugEnabled()) {
            log.debug("[getEventProcessedInfo] Output: " + processed);
        }
        return processed;
    }

    public void clearSessionListFor(String chatServiceName) {
        if (log.isDebugEnabled()) {
            log.debug("[clearSessionListFor]");
        }

        Client client = getClient();
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        WebTarget t = client.target(dbURL + "/chatService-" + ChatService.getApplicationName() + "/" + DBConstants.CHAT_SERVICE_CACHE_TIMEOUT);

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(DBConstants.CHAT_SERVICE_SESSION_LIST, "");

        try {
            Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).post(Entity.form(formData));

            if (resp.getStatus() == Status.OK.getStatusCode()) {
                log.debug("Session list clearing was successfull");
            } else {
                log.warn("Session list clearing failed");
            }
        } catch (Exception ex) {
            log.error("Session list clearing exception", ex);
        }
    }

    private void storeSessionListToCassandra() {
        if (log.isDebugEnabled()) {
            log.debug("[storeSessionList]");
        }

        Client client = getClient();
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        WebTarget t = client.target(dbURL + "/chatService-" + ChatService.getApplicationName() + "/" + DBConstants.CHAT_SERVICE_CACHE_TIMEOUT);

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        String sessionList = getSessionList();
        formData.add(DBConstants.CHAT_SERVICE_SESSION_LIST, sessionList);

        try {
            Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).post(Entity.form(formData));

            if (resp.getStatus() == Status.OK.getStatusCode()) {
                log.debug("Session list storing was successfull");
            } else {
                log.warn("Session list storing failed");
            }
        } catch (Exception ex) {
            log.error("Session list storing exception", ex);
        }
    }

    private ChatSessionInfo loadSessionFromCassandra(String sessionId) throws InvalidSessionIdException, BackendUnavailableException, RequestFailedException {
        if (log.isDebugEnabled()) {
            log.debug("[loadSessionFromCassandra] Input=" + sessionId);
        }

        if (sessionId == null || sessionId.trim().length() == 0) {
            if (log.isInfoEnabled()) {
                log.info("The sessionId can't be empty!");
                log.info("KPITE503: TechnicalException: InvalidChatSessionIdException.");
            }
            throw new InvalidSessionIdException("The session can't be empty");
        }
        String dbURL = ChatService.getGmsConnector().getBaseDBUrl();
        if (dbURL == null) {
            log.warn("Read data from cassandra is not successful (init).");
            throw new BackendUnavailableException("Read data from cassandra is not successful (init).");
        }

        Client client = getClient();

        WebTarget t = client.target(dbURL + "/session" + sessionId);
        Response resp = t.request().header(DBConstants.GMS_USER_HEADER, DBConstants.GMS_CUSTOM_ID).get();

        final ChatSessionInfo session = new ChatSessionInfo();
        session.setSessionId(sessionId);

        JsonNode responseRootNode;
        try {
            responseRootNode = JsonHelper.getJsonRootNode(resp.readEntity(String.class));

            // Respond depending on status
            if (resp.getStatus() == Status.OK.getStatusCode()) {

                JsonNode sessionIdNode = responseRootNode.get(DBConstants.COLUMN_SESSION_ID);
                if (sessionIdNode != null) {
                    session.setSessionId(sessionIdNode.textValue());
                }

                JsonNode userIdNode = responseRootNode.get(DBConstants.COLUMN_USER_ID);
                if (userIdNode != null) {
                    session.setUserId(userIdNode.textValue());
                }

                JsonNode secureKeyNode = responseRootNode.get(DBConstants.COLUMN_SECURE_KEY);
                if (secureKeyNode != null) {
                    session.setSecureKey(secureKeyNode.textValue());
                }

                JsonNode lastIndexNode = responseRootNode.get(DBConstants.COLUMN_NEXT_POSITION);
                if (lastIndexNode != null) {
                    session.setNextPosition(Integer.parseInt(lastIndexNode.textValue()));
                }

                JsonNode disconnectReasonNode = responseRootNode.get(DBConstants.COLUMN_DISCONNECT_REASON);
                if (disconnectReasonNode != null) {
                    session.setDisconnectReason(disconnectReasonNode.textValue());
                }

                JsonNode statusNode = responseRootNode.get(DBConstants.COLUMN_STATUS);
                if (statusNode != null) {
                    String status = statusNode.textValue();
                    if (status != null) {
                        session.setStatus(StatusEnum.valueOf(status));
                    } else {
                        session.setStatus(StatusEnum.CLOSED);
                    }
                } else {
                    session.setStatus(StatusEnum.CLOSED);
                }

                JsonNode wawNode = responseRootNode.get(DBConstants.COLUMN_CHATSERVICE_INSTANCES);
                if (wawNode != null) {
                    String sChatServiceInstances = wawNode.textValue();
                    LinkedList<String> ChatServiceInstances = new LinkedList<>();
                    if (sChatServiceInstances != null) {
                        String[] aChatServiceInstances = sChatServiceInstances.split(",");
//                        for (int i = 0; i < aChatServiceInstances.length; i++) {
//                            ChatServiceInstances.add(aChatServiceInstances[i]);
//                        }
                        ChatServiceInstances.addAll(Arrays.asList(aChatServiceInstances));
                        session.setChatServiceInstances(ChatServiceInstances);
                    }
                }

                JsonNode lastAccessNode = responseRootNode.get(DBConstants.COLUMN_LAST_ACCESS);
                if (lastAccessNode != null) {
                    String sLastAccess = lastAccessNode.textValue();
                    try {
                        Long lastAccess = Long.parseLong(sLastAccess);
                        session.setModifiedTime(new Date(lastAccess));
                    } catch (NumberFormatException e) {
                        log.warn("Last access " + sLastAccess + "cannot be parsed to long value. Last access will not be retrieved fromthe GMS Cassandra");
                    }

                }

                if (log.isInfoEnabled()) {
                    log.info("Gets data from Cassandra. Session: " + session.toString());
                }
            } else {
                log.warn("Requesting of session was not successfull. Response HTTP Status is " + resp.getStatus() + " and response is " + resp);

                if (resp.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    throw new BackendUnavailableException("It was not possible to retrieve data from GMS Cassandra. HTTP Status " + resp.getStatus());
                } else {
                    throw new RequestFailedException("It was not possible to retrieve data from GMS Cassandra. HTTP Status " + resp.getStatus());
                }
            }
        } catch (IOException ex) {
            log.warn("Requestiong data from cassandra was not successful. " + ex.getMessage());
            throw new BackendUnavailableException("Requestiong data from cassandra was not successful.");
        }

        return session;
    }

    private Client getClient() {
        int requestTimeout = options.getDatabase().getCassandraTimeout();
        ClientConfig clientConfig = new ClientConfig();
        if (requestTimeout > 0) {
            clientConfig.property(ClientProperties.CONNECT_TIMEOUT, requestTimeout * 1000);
            clientConfig.property(ClientProperties.READ_TIMEOUT, requestTimeout * 1000);
        }

        Client client = ClientBuilder.newClient(clientConfig);

        HttpAuthenticationFeature feature = null;
        if (options != null && options.getGms().getGmsUser() != null && options.getGms().getGmsPassword() != null) {
            feature = HttpAuthenticationFeature.basic(options.getGms().getGmsUser(), options.getGms().getGmsPassword());
        }

        if (feature != null) {
            client.register(feature);
        }

        return client;
    }

    private String getSessionList() {
        StringBuilder list = new StringBuilder();
        for (Map.Entry<String, ChatSessionInfo> kvp : sessionCache.entrySet()) {
            list.append(kvp.getValue().getSessionId()).append(",");
        }

        return list.toString();
    }
}
