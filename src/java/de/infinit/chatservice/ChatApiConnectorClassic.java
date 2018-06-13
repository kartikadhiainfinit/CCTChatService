package de.infinit.chatservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.infinit.chatservice.beans.ChatSessionInfo;
import de.infinit.chatservice.beans.StatusEnum;
import de.infinit.chatservice.chatApi.beans.*;
import de.infinit.chatservice.communication.AdapterCommunication;
import de.infinit.chatservice.communication.beans.ChatMessage;
import de.infinit.chatservice.communication.client.*;
import de.infinit.chatservice.configuration.CMEApplication;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import de.infinit.chatservice.db.DBConnector;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.OpenChatTimeoutException;
import de.infinit.chatservice.exceptions.InvalidArgumentException;
import de.infinit.chatservice.exceptions.InvalidSessionIdException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.exceptions.OpenChatRequestFailedException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author xxbamar
 */
public class ChatApiConnectorClassic {

    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChatApiConnectorClassic.class);
    private ApplicationOptions options;
    private final String applicationName;
    private DBConnector db;
    private ObjectMapper mapper = new ObjectMapper();

    private String chatChannel;
    private final int QUEUING_MESSAGE_SEND_DELAY_MS = 100;

    public boolean isGmsConnected() {
        return true;
    }

    public ChatApiConnectorClassic(String applicationName, ApplicationOptions options, DBConnector db) {
        this.options = options;
        this.db = db;
        this.applicationName = applicationName;
    }

    public void Init() {
        try {
            log.info("Initializing ChatApiConnector");
            chatChannel = options.getCommon().getChatChannel();
            //openChatSyncTimeout = options.getCommon().getOpenChatSyncTimeout();
            if (chatChannel == null || "".equals(chatChannel)) {
                log.error("Chat channel is empty. ChatApiConnector cannot be initialized");
                return;
            }
            if (ChatService.getGmsConnector().getChatApiUrl() == null) {
                log.error("ChatApi Url is null. Check whether GMS Node is running");
                return;
            }

            loadSessions();
        } catch (Exception ex) {
            log.error("Cannot initialize ChatApiConnector", ex);
        }
    }

    public void reconnect() {
        // Not implemented
    }

    public synchronized void openChat(final String sessionId, ClientStartChatRequest clientRequest) throws OpenChatRequestFailedException, OpenChatTimeoutException, BackendUnavailableException, InvalidArgumentException, JSONException, Exception, JsonProcessingException, InterruptedException {
        log.info("Opening new chat [" + sessionId + "]: " + clientRequest);

        if (sessionId == null) {
            throw new InvalidArgumentException("SessionId is null");
        }

        String routingService = CMEApplication.getRoutingServiceForCategory(clientRequest.getCategoryId());
        if (routingService == null) {
            log.info("KPITE504: TechnicalException: InvalidServiceNameException.");
            throw new InvalidArgumentException("Routing service for category id " + clientRequest.getCategoryId() + " not found");
        }
        clientRequest.setSessionId(sessionId);

        Map<String, Object> request = createOpenChatData(routingService, clientRequest);

        String url = ChatService.getGmsConnector().getChatApiUrl();
        url += "/2/chat/" + chatChannel;

        Client client = getClient();
        WebTarget t = client.target(url);
        log.info("Sending openChat request [" + sessionId + "] to " + url + ".  Request: " + request);
        Response resp = t.request().post(Entity.entity(request, "application/x-www-form-urlencoded; charset=UTF-8"));

        if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            log.warn("[GMS openChat] Not available:" + resp.getStatus());
            throw new RequestFailedException("[GMS openChat] Not available:" + resp.getStatus());
        } else if (resp.getStatus() == Status.OK.getStatusCode()) {
            log.warn("[GMS openChat] OK: " + resp.getStatus() + ", Content: " + resp.toString());

            JSONObject obj = new JSONObject(resp.toString());
            log.info("json: " + obj.toString());

            ChatSessionInfo session = new ChatSessionInfo();
            session.addPrimaryChatService(ChatService.getApplicationName());
            session.setSessionId(sessionId);
            session.setSecureKey(obj.getString("secureKey"));
            session.setGmsChatId(obj.getString("chatId"));
            session.setGmsUserId(obj.getString("userId"));
            session.setGmsAlias(obj.getString("alias"));
            session.setModifiedTime(new Date());
            session.setUserId(clientRequest.getUserId());
            session.setStatus(StatusEnum.OPEN);

            session.setNextPosition(1);
            db.storeSession(session);

            CategoryConfig category = getCategory(options.getCategories(), clientRequest.getCategoryId());
            if (category != null) {
                if (category.isEnableQueuingMessage()) {
                    log.info("Queuing message is enabled [" + session.getSessionId() + "]");
                    final String userId = session.getUserId();
                    final String queuingMessage = category.getQueuingMessage();
                    // Send Queing message to client after specified amount of time
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            log.info("Sending queuing message [" + sessionId + "]");
                            AdapterCommunication.sendMessageToClient(sessionId, userId, queuingMessage, "External");
                        }
                    }, QUEUING_MESSAGE_SEND_DELAY_MS);
                } else {
                    log.info("Queuing message is disabled [" + session.getSessionId() + "]");
                }
            } else {
                log.info("Category for Queuing message was not found. No message will be sent");
            }

        } else {
            log.warn("[GMS openChat] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
            throw new RequestFailedException("[GMS openChat] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
        }

        log.info("Leaving openChat with sessionId " + sessionId);
    }

    public void sendMessage(String sessionId, ClientSendMessageRequest clientRequest) throws InvalidArgumentException, InvalidSessionIdException, BackendUnavailableException, RequestFailedException, JSONException, JsonProcessingException, InterruptedException {
        log.info("Sending user message [" + sessionId + "]: " + clientRequest);
        if (sessionId == null) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("SessionId is null");
        }

        ChatSessionInfo session = db.getSession(sessionId);
        if (session == null) {
            throw new InvalidSessionIdException("Session was not found [" + sessionId + "]");
        }

        for (ChatMessage message : clientRequest.getChatMessages()) {
            if (message.getText() == null) {
                log.warn("Chat message cannot be null, message will be not sent [" + sessionId + "]");
            } else {
                Map<String, Object> request = new HashMap<>();
                request.put("operation", "sendMessage");
                request.put("message", message.getText());
                request.put("messageType", "text");
                request.put("secureKey", session.getSecureKey());
                request.put("userId", session.getGmsUserId());
                request.put("alias", session.getGmsAlias());

                String url = ChatService.getGmsConnector().getChatApiUrl();
                url += "/2/chat/" + chatChannel + "/" + session.getGmsChatId() + "/send";

                Client client = getClient();
                WebTarget t = client.target(url);
                log.info("Sending sendMessage request. [" + sessionId + "] to " + url + ". Request: " + request);
                Response resp = t.request().post(Entity.entity(request, "application/x-www-form-urlencoded; charset=UTF-8"));

                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    log.warn("[GMS sendMessage] Not available:" + resp.getStatus());
                    throw new RequestFailedException("[GMS openChat] Not available:" + resp.getStatus());
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    log.warn("[GMS sendMessage] OK: " + resp.getStatus() + ", Content: " + resp.toString());

                    JSONObject obj = new JSONObject(resp.toString());
                    log.info("json: " + obj.toString());

                    session.setNextPosition(Integer.parseInt(obj.getString("nextPosition")));
                    db.storeSession(session);

                } else {
                    log.warn("[GMS sendMessage] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
                    throw new RequestFailedException("[GMS sendMessage] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
                }
            }
        }

        // update instances
        if (checkApplicationNameKVP(session)) {
            addApplicationName(session);
        }
    }

    public void disconnect(final String sessionId) throws Exception, InvalidArgumentException, BackendUnavailableException, InvalidSessionIdException, RequestFailedException, JsonProcessingException, InterruptedException {
        if (log.isInfoEnabled()) {
            log.info("Disconnecting from chat [" + sessionId + "]");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("SessionId is null or empty");
        }

        ChatSessionInfo session = db.getSession(sessionId);
        if (session == null) {
            throw new InvalidSessionIdException("Session was not found [" + sessionId + "]");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("operation", "disconnect");
        request.put("secureKey", session.getSecureKey());
        request.put("userId", session.getGmsUserId());
        request.put("alias", session.getGmsAlias());

        String url = ChatService.getGmsConnector().getChatApiUrl();
        url += "/2/chat/" + chatChannel + "/" + session.getGmsChatId() + "/disconnect";

        Client client = getClient();
        WebTarget t = client.target(url);
        log.info("Sending disconnect request. [" + sessionId + "] to " + url + ". Request: " + request);
        Response resp = t.request().post(Entity.entity(request, "application/x-www-form-urlencoded; charset=UTF-8"));

        if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            log.warn("[GMS disconnect] Not available:" + resp.getStatus());
            throw new RequestFailedException("[GMS openChat] Not available:" + resp.getStatus());
        } else if (resp.getStatus() == Status.OK.getStatusCode()) {
            log.warn("[GMS disconnect] OK: " + resp.getStatus() + ", Content: " + resp.toString());

            JSONObject obj = new JSONObject(resp.toString());
            log.info("json: " + obj.toString());

            session.setNextPosition(Integer.parseInt(obj.getString("nextPosition")));
            db.storeSession(session);

        } else {
            log.warn("[GMS disconnect] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
            throw new RequestFailedException("[GMS disconnect] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
        }

        // update instances
        if (checkApplicationNameKVP(session)) {
            addApplicationName(session);
        }
    }

    public void updateUserData(String sessionId, Map<String, String> data) throws InvalidArgumentException, InvalidSessionIdException, BackendUnavailableException, RequestFailedException, JSONException, JsonProcessingException, InterruptedException {
        log.info("updateUserData " + data);
        if (sessionId == null) {
            throw new InvalidArgumentException("SessionId is null");
        }
        if (data == null) {
            throw new InvalidArgumentException("Userdata to update is null");
        }

        ChatSessionInfo session = db.getSession(sessionId);
        if (session == null) {
            throw new InvalidSessionIdException("Session with sessionId " + sessionId + " was not found");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("operation", "updateData");
        request.put("secureKey", session.getSecureKey());
        request.put("userId", session.getGmsUserId());
        request.put("alias", session.getGmsAlias());

        Map<String, Object> userData = new HashMap<>();

        Set<Map.Entry<String, String>> entrySet = data.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            userData.put(entry.getKey(), entry.getValue());
        }

        request.put("userData", userData);

        String url = ChatService.getGmsConnector().getChatApiUrl();
        url += "/2/chat/" + chatChannel + "/" + session.getGmsChatId() + "/updateData";

        Client client = getClient();
        WebTarget t = client.target(url);
        log.info("Sending sendMessage request. [" + sessionId + "] to " + url + ". Request: " + request);
        Response resp = t.request().post(Entity.entity(request, "application/x-www-form-urlencoded; charset=UTF-8"));

        if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            log.warn("[GMS sendMessage] Not available:" + resp.getStatus());
            throw new RequestFailedException("[GMS openChat] Not available:" + resp.getStatus());
        } else if (resp.getStatus() == Status.OK.getStatusCode()) {
            log.warn("[GMS sendMessage] OK: " + resp.getStatus() + ", Content: " + resp.toString());

            JSONObject obj = new JSONObject(resp.toString());
            log.info("json: " + obj.toString());

            session.setNextPosition(Integer.parseInt(obj.getString("nextPosition")));
            db.storeSession(session);

        } else {
            log.warn("[GMS sendMessage] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
            throw new RequestFailedException("[GMS sendMessage] Fail: " + resp.getStatus() + ", Content: " + resp.toString());
        }

    }

    public void cleanupIdleChats() throws BackendUnavailableException, RequestFailedException {
        log.info("Cleanup idle chats");
        Map<String, ChatSessionInfo> sessions = db.getSessions();
        long idleTimeout = options.getCommon().getIdleTimeoutMin();
        if (idleTimeout < 1) {
            log.warn("Idle timeout is not set. No cleanup");
            return;
        }
        //Convert mins to ms
        idleTimeout *= 1000L * 60;

        for (Iterator<Map.Entry<String, ChatSessionInfo>> it = sessions.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChatSessionInfo> entry = it.next();
            ChatSessionInfo session = entry.getValue();
            long idleTime = new Date().getTime() - session.getModifiedTime().getTime();
            if (idleTime > idleTimeout) {
                log.info("Removing session " + session + ". Idle for " + (idleTime / 60 / 1000L) + "mins");
                AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), "IDLE CHAT CLEANUP", "Extern");
                db.deleteSession(session);
                it.remove();
            }
        }
    }

    public void claimSessions(String chatServiceName) {
        db.restoreSessions(chatServiceName);
        db.clearSessionListFor(chatServiceName);
    }

    public void chatSessionClaimed(String sessionId) {
        db.removeSessionFromCurrentChatService(sessionId);
    }

    // Adapter disconnected. Turned on automatically on first request
    public void reset() {
        db.clearSessionCache();
        reconnect();
    }

    private void processResponse(ChatResponse response) throws BackendUnavailableException, RequestFailedException {
        ChatSessionInfo session = null;
        for (ChatResponseMessage message : response.getMessages()) {
            if ("ParticipantJoined".equals(message.getType()) && "Client".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Client joined)");
            } else if ("ParticipantJoined".equals(message.getType()) && "Agent".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Agent joined) - Start conversation.");
                session = findSessionBySecureKey(response.getSecureKey());

                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }

                if (StatusEnum.TRANSFER == session.getStatus()) {
                    log.info("Transfer [" + session.getSessionId() + "]");
                    session.setStatus(StatusEnum.OPEN);
//                    AdapterCommunication.conversationStarted(session.getSessionId(), session.getUserId(), message.getFrom().getNickname(), Integer.toString(message.getFrom().getParticipantId()));
                    boolean connectionIsOk = AdapterCommunication.notifyAgentChanged(session.getSessionId(), session.getUserId(), message.getFrom().getNickname(), Integer.toString(message.getFrom().getParticipantId()));
                    if (!connectionIsOk) {
                        // break when connection broken
                        return;
                    }
                } else {
                    boolean connectionIsOk = AdapterCommunication.conversationStarted(session.getSessionId(), session.getUserId(), message.getFrom().getNickname(), Integer.toString(message.getFrom().getParticipantId()));
                    if (!connectionIsOk) {
                        // break when connection broken
                        return;
                    }
                }
            } else if ("ParticipantJoined".equals(message.getType())) {
                log.info("ChatServer message (External participant joined)");
            } else if ("Message".equals(message.getType()) && "Client".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Client Message)");
                if (options.getCommon().isSendBackClientMessages()) {
                    session = findSessionBySecureKey(response.getSecureKey());
                    if (session == null) {
                        log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                        return;
                    }
                    boolean connectionIsOk = AdapterCommunication.sendMessageToClient(session.getSessionId(), session.getUserId(), message.getText(), message.getFrom().getType());
                    if (!connectionIsOk) {
                        // break when connection broken
                        return;
                    }
                } else {
                    log.info("Client message and sending back client messages is disabled");
                }
            } else if ("Message".equals(message.getType()) && "Agent".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Agent Message)");
                session = findSessionBySecureKey(response.getSecureKey());
                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }
                if (Constants.MESSAGE_TRANSFERED.equals(message.getText())) {
                    session.setStatus(StatusEnum.TRANSFER);
                }
                boolean connectionIsOk = AdapterCommunication.sendMessageToClient(session.getSessionId(), session.getUserId(), message.getText(), message.getFrom().getType());
                if (!connectionIsOk) {
                    // break when connection broken
                    return;
                }
            } else if ("Message".equals(message.getType())) {
                log.info("ChatServer message (External participant Message)");
                session = findSessionBySecureKey(response.getSecureKey());
                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }

                String userNickname = message.getFrom().getNickname();
                String externalNickname = options.getCommon().getExternalNickname();
                if (externalNickname != null && userNickname != null
                        && externalNickname.equalsIgnoreCase(userNickname)) {
                    final String text = message.getText();

                    if (GmsDataConstants.DISCONNECT_OVERFLOW.equalsIgnoreCase(text)
                            || GmsDataConstants.DISCONNECT_OVERLOAD.equalsIgnoreCase(text)) {
                        session.setDisconnectReason(text);
                        log.info("KPIBE501: BusinessException: AllAgentBusyException.");
//                        boolean connectionIsOk = AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), null, "External");
//                        if (!connectionIsOk) {
//                            // break when connection broken
//                            return;
//                        }
                        AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), null, "External");
                        return;
                    } else if (GmsDataConstants.DISCONNECT_OUTOFOFFICEHOURS.equalsIgnoreCase(text)) {
                        session.setDisconnectReason(text);
                        log.info("KPIBE504: BusinessException: ServiceClosedException.");
//                        boolean connectionIsOk = AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), null, "External");
//                        if (!connectionIsOk) {
//                            // break when connection broken
//                            return;
//                        }
                        AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), null, "External");
                        return;
                    }
                }

                boolean connectionIsOk = AdapterCommunication.sendMessageToClient(session.getSessionId(), session.getUserId(), message.getText(), message.getFrom().getType());
                if (!connectionIsOk) {
                    // break when connection broken
                    return;
                }
            } else if ("ParticipantLeft".equals(message.getType()) && "Client".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Client left)");
                session = findSessionBySecureKey(response.getSecureKey());
                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }
                db.deleteSession(session);
            } else if ("ParticipantLeft".equals(message.getType()) && "Agent".equals(message.getFrom().getType())) {
                log.info("ChatServer message (Agent left)");
                session = findSessionBySecureKey(response.getSecureKey());
                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }

                if (StatusEnum.TRANSFER == session.getStatus()) {
//                    session.setStatus(StatusEnum.OPEN);

                } else {
                    session.setStatus(StatusEnum.CLOSED);
                    boolean connectionIsOk = AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), message.getText(), message.getFrom().getType());
                    if (!connectionIsOk) {
                        // break when connection broken
                        return;
                    }
                }
            } else if ("ParticipantLeft".equals(message.getType())) {
                log.info("ChatServer message (External participant left)");
                session = findSessionBySecureKey(response.getSecureKey());
                if (session == null) {
                    log.error("Session not found in cache for secureKey " + response.getSecureKey() + ". Ignoring message");
                    return;
                }
                boolean connectionIsOk = AdapterCommunication.finishConversation(session.getSessionId(), session.getUserId(), message.getText(), message.getFrom().getType());
                if (!connectionIsOk) {
                    // break when connection broken
                    return;
                }
            } else if ("PushUrl".equals(message.getType())) {
                log.info("ChatServer message (PushUrl)");
            } else if ("FileUploaded".equals(message.getType())) {
                log.info("ChatServer message (FileUploaded)");
            } else if ("IdleAlert".equals(message.getType())) {
                log.info("ChatServer message (IdleAlert)");
            } else if ("IdleClose".equals(message.getType())) {
                log.info("ChatServer message (IdleClose)");
            }
        }

        if (session != null) {
            session.setNextPosition(response.getNextPosition());
            db.storeSession(session);
        }
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------
    private Map<String, Object> createOpenChatData(String serviceName, ClientStartChatRequest data) throws InvalidArgumentException, JSONException {
        Map<String, Object> request = new HashMap<>();
        if (data.getMsisdn() == null || data.getMsisdn().trim().length() == 0) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("The nickname, topic and subtopic are required.", "nickname");
        }
        if (data.getNickname() == null || data.getNickname().trim().length() == 0) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("The nickname, topic and subtopic are required.", "nickname");
        }

        if (serviceName == null || serviceName.trim().length() == 0) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("The service name is required.", "serviceName");
        }

        CategoryConfig category = getCategory(options.getCategories(), data.getCategoryId());

        request.put("operation", "requestChat");
        request.put("subject", category.getDisplayName());
        request.put("nickname", data.getNickname());
        request.put("firstName", data.getFirstName());
        request.put("lastName", data.getLastName());

        Map<String, Object> userData = new HashMap<>();

        userData.put(GmsDataConstants.KV_CP_NICKNAME, data.getNickname());
        userData.put(GmsDataConstants.KV_CP_SERVICE_NAME, serviceName);
        userData.put(GmsDataConstants.KV_CP_TOPIC_SUBTOPIC, category.getDisplayName());
        userData.put(GmsDataConstants.KV_CP_MSISDN, data.getMsisdn());

        String name = null;
        if (data.getFirstName() != null && !data.getFirstName().isEmpty()) {
            name = data.getFirstName();
            if (data.getLastName() != null && !data.getLastName().isEmpty()) {
                name = name.concat(" ");
            }
        }
        if (data.getLastName() != null && !data.getLastName().isEmpty()) {
            if (name == null) {
                name = data.getLastName();
            } else {
                name = name.concat(data.getLastName());
            }
        }

//        final String name = data.getFirstName().concat(" " + data.getLastName());
        if (name != null) {
            userData.put(GmsDataConstants.KV_CP_CUST_NAME, name);
        }

        final String salutation = data.getSalutation();
        if (salutation != null) {
            userData.put(GmsDataConstants.KV_CP_SALUTATION, salutation);
        }

        final String email = data.getEmail();
        if (email != null) {
            userData.put(GmsDataConstants.KV_CP_EMAIL, email);
        }

        final String pkkCheck = Boolean.toString(data.getPkkCheck());
        userData.put(GmsDataConstants.KV_CP_PKK_CHECK, pkkCheck);

        request.put("userData", userData);

        return request;
    }

    private ChatSessionInfo findSessionBySecureKey(String secureKey) {
        for (Map.Entry<String, ChatSessionInfo> entry : db.getSessions().entrySet()) {
            ChatSessionInfo session = entry.getValue();
            if (session != null) {
                if (secureKey.equals(session.getSecureKey())) {
                    return session;
                }
            }
        }

        return null;
    }

    private CategoryConfig getCategory(Map<String, CategoryConfig> categories, String categoryId) {
        CategoryConfig requiredCategory = null;
        for (Map.Entry<String, CategoryConfig> entry : categories.entrySet()) {
            CategoryConfig cat = entry.getValue();
            if (cat.getId().equals(categoryId)) {
                requiredCategory = cat;
            }
        }

        return requiredCategory;
    }

    private void loadSessions() {
        log.info("Loading sessions");
        db.clearSessionCache();
        db.restoreSessions(ChatService.getApplicationName());
    }

    public void requestNotifications(ChatSessionInfo session) {
        log.info("Requesting notifications for session: " + session);
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "requestNotifications");
        request.put("secureKey", session.getSecureKey());
        request.put("transcriptPosition", session.getNextPosition());

        // TODO Classic ChatApi Refresh Chat
    }


    private boolean checkApplicationNameKVP(ChatSessionInfo chatSessionInfo) {
        boolean addAppName = false;
        LinkedList<String> webApiWrappers = chatSessionInfo.getChatServiceInstances();
        if (webApiWrappers == null || webApiWrappers.isEmpty()) {
            addAppName = true;
        } else {
            String lastWebApiWrapper = webApiWrappers.get(webApiWrappers.size() - 1);
            if (!applicationName.equals(lastWebApiWrapper)) {
                addAppName = true;
            }
        }
        return addAppName;
    }

    private void addApplicationName(ChatSessionInfo chatSessionInfo) throws RequestFailedException, BackendUnavailableException, InvalidArgumentException {
        if (chatSessionInfo.getChatServiceInstances() == null) {
            chatSessionInfo.setChatServiceInstances(new LinkedList<String>());
            chatSessionInfo.getChatServiceInstances().add(applicationName);
        }

        Map<String, String> kvc = new HashMap<>();

        if (chatSessionInfo.getChatServiceInstances() != null) {
            StringBuilder sbWawInstances = new StringBuilder();
            for (String instance : chatSessionInfo.getChatServiceInstances()) {
                sbWawInstances.append(instance).append(",");
            }
            if (sbWawInstances.length() > 0) {
                kvc.put(GmsDataConstants.KV_INSTANCE_NAME, sbWawInstances.toString());
            }
        }

        try {
            updateUserData(chatSessionInfo.getSessionId(), kvc);
        } catch (JSONException | JsonProcessingException | InterruptedException ex) {
            log.error("Json Exception catched when updating instances userdata", ex);
            throw new RequestFailedException("Json Exception catched when updating instances userdata");
        }

        // update cassandra
        db.storeSession(chatSessionInfo);

        if (log.isDebugEnabled()) {
            log.debug("Add value: " + applicationName + " to userData for chatId: " + chatSessionInfo.getSessionId());
        }
    }

    private Client getClient() {
        Client client = null;

        try {
            client = ClientBuilder.newClient(new ClientConfig());
        } catch (Exception e) {
            log.info("Cannot get client. " + e.getMessage());
        }

        return client;
    }
}
