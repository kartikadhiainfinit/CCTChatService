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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.cometd.websocket.client.WebSocketTransport;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author xxbamar
 */
public class ChatApiConnector {

    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChatApiConnector.class);
    private CountDownLatch requestChatSignal = new CountDownLatch(1);
    private ApplicationOptions options;
    private final String applicationName;
    private DBConnector db;
    private ObjectMapper mapper = new ObjectMapper();
    private BayeuxClient client;

    private String chatChannel;
    private boolean gmsConnected = false;
    private int openChatSyncTimeout_s = 30;
    private final int QUEUING_MESSAGE_SEND_DELAY_MS = 100;

    private ClientStartChatRequest currentOpenChatRequest = null;
    private ChatSessionInfo currentOpenChatSessionInfo = null;
    private Exception currentOpenChatException = null;

    public boolean isGmsConnected() {
        return gmsConnected;
    }

    public ChatApiConnector(String applicationName, ApplicationOptions options, DBConnector db) {
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

            WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

            HttpClient httpClient = new HttpClient();
            httpClient.addBean(webSocketContainer, true);
            httpClient.start();
            
            ClientTransport wsTransport = new WebSocketTransport(null, null, webSocketContainer);
            client = new BayeuxClient(ChatService.getGmsConnector().getChatApiUrl(), wsTransport, new LongPollingTransport(null, httpClient));
            //client.setOption(ClientTransport.MAX_NETWORK_DELAY_OPTION, 2000);

            client.getChannel(Channel.META_HANDSHAKE).addListener(new InitializerListener());
            client.getChannel(Channel.META_CONNECT).addListener(new ConnectionListener());
            client.getChannel(chatChannel).addListener(new ChatListener());
            client.handshake();

            loadSessions();
        } catch (Exception ex) {
            log.error("Cannot initialize ChatApiConnector", ex);
        }
    }

    public void reconnect() {
        try {
            log.info("Reconnect ChatApiConnector");
            gmsConnected = false;
            chatChannel = options.getCommon().getChatChannel();
            if (chatChannel == null || "".equals(chatChannel)) {
                log.error("Chat channel is empty. ChatApiConnector cannot be initialized");
                return;
            }
            if (ChatService.getGmsConnector().getChatApiUrl() == null) {
                log.error("ChatApi Url is null. Check whether GMS Node is running");
                return;
            }
            
            WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

            HttpClient httpClient = new HttpClient();
            httpClient.addBean(webSocketContainer, true);
            httpClient.start();
            
            ClientTransport wsTransport = new WebSocketTransport(null, null, webSocketContainer);
            client = new BayeuxClient(ChatService.getGmsConnector().getChatApiUrl(), wsTransport, new LongPollingTransport(null, httpClient));
            
            client.getChannel(Channel.META_HANDSHAKE).addListener(new InitializerListener());
            client.getChannel(Channel.META_CONNECT).addListener(new ConnectionListener());
            client.getChannel(chatChannel).addListener(new ChatListener());
            client.handshake();
        } catch (Exception ex) {
            log.error("Cannot reconnect ChatApiConnector", ex);
        }
    }

    public synchronized void openChat(String sessionId, ClientStartChatRequest clientRequest) throws OpenChatRequestFailedException, OpenChatTimeoutException, BackendUnavailableException, InvalidArgumentException, JSONException, Exception, JsonProcessingException, InterruptedException {
        log.info("Opening new chat [" + sessionId + "]: " + clientRequest);
        // Reset open chat sync data
        currentOpenChatSessionInfo = null;
        currentOpenChatException = null;
        currentOpenChatRequest = null;

        if (sessionId == null) {
            throw new InvalidArgumentException("SessionId is null");
        }

        if (gmsConnected && client != null) {
            String routingService = CMEApplication.getRoutingServiceForCategory(clientRequest.getCategoryId());
            if (routingService == null) {
                log.info("KPITE504: TechnicalException: InvalidServiceNameException.");
                throw new InvalidArgumentException("Routing service for category id " + clientRequest.getCategoryId() + " not found");
            }
            Map<String, Object> request = createOpenChatData(routingService, clientRequest);

            // Store session Id for synchronization purposes
            clientRequest.setSessionId(sessionId);
            // add sync data
            currentOpenChatRequest = clientRequest;
            log.info("Publishing openChat request and setting mutex. [" + sessionId + "] Request: " + request);

            // Start publishing
            requestChatSignal = new CountDownLatch(1);
//            client.getChannel(chatChannel).publish(request);
            client.getChannel(chatChannel).publish(request, new PublishListener(null, null));

            requestChatSignal.await(openChatSyncTimeout_s, TimeUnit.SECONDS);
            if (currentOpenChatException != null) {
                throw currentOpenChatException;
            } else if (currentOpenChatSessionInfo == null) {
                throw new OpenChatTimeoutException();
            }
            // Reset open chat sync data
            currentOpenChatSessionInfo = null;
            currentOpenChatException = null;
            currentOpenChatRequest = null;
        } else {
            reconnect();
            log.info("KPITE501: TechnicalException: BackendUnavailableException.");
            throw new BackendUnavailableException("Chat could not be opened because ChatApiConnector is not connected to Gms Server");
        }
        log.info("Leaving openChat with sessionId " + sessionId);
    }

    public void sendMessage(String sessionId, ClientSendMessageRequest clientRequest) throws InvalidArgumentException, InvalidSessionIdException, BackendUnavailableException, RequestFailedException, JSONException, JsonProcessingException, InterruptedException {
        log.info("Sending user message [" + sessionId + "]: " + clientRequest);
        if (sessionId == null) {
            log.info("KPITE502: TechnicalException: InvalidArgumentException.");
            throw new InvalidArgumentException("SessionId is null");
        }

        if (gmsConnected && client != null) {
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

//                    client.getChannel(chatChannel).publish(request);
                    CountDownLatch syncSignal = new CountDownLatch(1);
                    Boolean[] publishResponse = new Boolean[1];
                    publishResponse[0] = false;
                    client.getChannel(chatChannel).publish(request, new PublishListener(syncSignal, publishResponse));
                    syncSignal.await(5, TimeUnit.SECONDS);
                    if (publishResponse != null && publishResponse.length > 0 && publishResponse[0]) {
                        log.error("Sending message was not successfull for sessionId " + sessionId);
                        throw new RequestFailedException("Sending message was not successfull for sessionId " + sessionId);
                    }
                }
            }

            // update instances
            if (checkApplicationNameKVP(session)) {
                addApplicationName(session);
            }
        } else {
            reconnect();
            log.info("KPITE501: TechnicalException: BackendUnavailableException.");
            throw new BackendUnavailableException("ChatApiConnector is not connected to Gms Server");
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

        if (gmsConnected && client != null) {
            ChatSessionInfo session = db.getSession(sessionId);
            if (session == null) {
                throw new InvalidSessionIdException("Session was not found [" + sessionId + "]");
            }

            Map<String, Object> request = new HashMap<>();
            request.put("operation", "disconnect");
            request.put("secureKey", session.getSecureKey());

//            client.getChannel(chatChannel).publish(request);
            CountDownLatch syncSignal = new CountDownLatch(1);
            Boolean[] publishResponse = new Boolean[1];
            publishResponse[0] = false;
            client.getChannel(chatChannel).publish(request, new PublishListener(syncSignal, publishResponse));
            syncSignal.await(5, TimeUnit.SECONDS);
            if (publishResponse != null && publishResponse.length > 0 && publishResponse[0]) {
                log.error("Sending message was not successfull for sessionId " + sessionId);
                throw new RequestFailedException("Sending message was not successfull for sessionId " + sessionId);
            }

            if (log.isInfoEnabled()) {
                log.info("Disconnect request sent [" + sessionId + "]");
            }

            // update instances
            if (checkApplicationNameKVP(session)) {
                addApplicationName(session);
            }

        } else {
            reconnect();
            log.info("KPITE501: TechnicalException: BackendUnavailableException.");
            throw new BackendUnavailableException("ChatApiConnector is not connected to Gms Server [" + sessionId + "]");
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

        if (gmsConnected && client != null) {
            ChatSessionInfo session = db.getSession(sessionId);
            if (session == null) {
                throw new InvalidSessionIdException("Session with sessionId " + sessionId + " was not found");
            }

            Map<String, Object> request = new HashMap<>();
            request.put("operation", "updateData");
            request.put("secureKey", session.getSecureKey());

            Map<String, Object> userData = new HashMap<>();

            Set<Map.Entry<String, String>> entrySet = data.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                userData.put(entry.getKey(), entry.getValue());
            }

            request.put("userData", userData);

            client.getChannel(chatChannel).publish(request);
        } else {
            log.info("KPITE501: TechnicalException: BackendUnavailableException.");
            throw new BackendUnavailableException("ChatApiConnector is not connected to Gms Server");
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
                try {
                    log.info("ChatServer message (Client joined) [" + currentOpenChatRequest + "]");

                    session = new ChatSessionInfo();
                    session.addPrimaryChatService(ChatService.getApplicationName());
                    session.setSessionId(currentOpenChatRequest.getSessionId());
                    session.setSecureKey(response.getSecureKey());
                    session.setModifiedTime(new Date());
                    session.setUserId(currentOpenChatRequest.getUserId());
                    session.setStatus(StatusEnum.OPEN);

                    session.setNextPosition(response.getNextPosition());
                    db.storeSession(session);

                    CategoryConfig category = getCategory(options.getCategories(), currentOpenChatRequest.getCategoryId());
                    if (category != null) {
                        if (category.isEnableQueuingMessage()) {
                            log.info("Queuing message is enabled [" + session.getSessionId() + "]");
                            final String sessionId = session.getSessionId();
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

                    // When on, confirm for request thread
                    currentOpenChatSessionInfo = session;
                    log.info("Releasing openChat mutex for sessionId: " + session.getSessionId());
                    requestChatSignal.countDown();
                } catch (Exception ex) {
                    // Exceptions when parsing Client Participant joined
                    currentOpenChatException = ex;
                    requestChatSignal.countDown();
                }

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
    private void connectionEstablished() {
        log.info("system: Connection to Server Opened");
        gmsConnected = true;
    }

    private void connectionClosed() {
        log.warn("system: Connection to Server Closed");
        gmsConnected = false;
        ChatService.getGmsConnector().evaluateGMSNodesAvailability();
    }

    private void connectionBroken() {
        log.warn("system: Connection to Server Broken");
        gmsConnected = false;
        ChatService.getGmsConnector().evaluateGMSNodesAvailability();
    }

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

        client.getChannel(chatChannel).publish(request);
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------
    private class InitializerListener implements ClientSessionChannel.MessageListener {

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (message.isSuccessful()) {
                log.info("ChatApiConnector is ready - initialized");
            } else {
                log.info("ChatApiConnector is NOT ready - NOT initialized");
            }
        }
    }

    private class ConnectionListener implements ClientSessionChannel.MessageListener {

        private boolean wasConnected;
        private boolean connected;

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (client.isDisconnected()) {
                connected = false;
                connectionClosed();
                return;
            }

            wasConnected = connected;
            connected = message.isSuccessful();
            if (!wasConnected && connected) {
                connectionEstablished();
            } else if (wasConnected && !connected) {
                connectionBroken();
            }
        }
    }

    private class PublishListener implements ClientSessionChannel.MessageListener {

        private final CountDownLatch syncSignal;
        private Boolean[] response;

        PublishListener(CountDownLatch syncSignal, Boolean[] response) {
            this.syncSignal = syncSignal;
            this.response = response;
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            log.info("Publish message: " + message);
            Object failure = message.get("failure");
            try {

                if (failure != null) {
                    JSONObject obj = new JSONObject(failure);
                    log.info("Failure while sending request: " + failure.toString());

                    if (obj.has("message") && obj.getJSONObject("message").getString("data").contains("operation=requestChat")) {
                        // Open chat request failed, release mutex
                        log.info("Failure while requestChat - currentOpenChatRequest = " + currentOpenChatRequest + ": " + (String) failure);
                        currentOpenChatException = new OpenChatRequestFailedException("Failure while requestChat:" + (String) failure);
                        requestChatSignal.countDown();
                    }

                    if (response != null && response.length > 0) {
                        response[0] = true;
                    }
                }
            } catch (JSONException ex) {
                log.error("Json exception when parsing message", ex);
                if (response != null && response.length > 0) {
                    response[0] = true;
                }
            }

            if (syncSignal != null) {
                syncSignal.countDown();
            }
        }
    }

    private class ChatListener implements ClientSessionChannel.MessageListener {

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            try {
                log.info("Incoming message: " + message);
                Map<String, Object> data = message.getDataAsMap();
                if (data != null) {
                    ChatResponse response = mapper.convertValue(data, ChatResponse.class);
                    log.info("ChatResponse: " + response.toString());
                    if (response.getStatusCode() == 0 && response.getMessages() != null) {
                        processResponse(response);
                    } else if (response.getErrors() != null) {
                        log.error("GMS errors:");
                        for (Object object : response.getErrors()) {
                            if (object instanceof LinkedHashMap) {
                                LinkedHashMap<String, Object> map = (LinkedHashMap) object;
                                for (Map.Entry<String, Object> entry : map.entrySet()) {
                                    log.error("Error entry: " + entry.getKey() + " = " + entry.getValue());
                                }
                            }
                        }
                        if (currentOpenChatRequest != null) {
                            // Pending open chat. Check whether secureKey does not exist in session cache. If not, then it is requestChat message
                            String secureKey = response.getSecureKey();
                            Map<String, ChatSessionInfo> sessions = db.getSessions();
                            for (Map.Entry<String, ChatSessionInfo> entry : sessions.entrySet()) {
                                if (secureKey.equals(entry.getValue().getSecureKey())) {
                                    //Secure key exists in session cache. Ignore
                                    return;
                                }
                            }
                            // Secure key is new. Release mutex;
                            currentOpenChatException = new OpenChatRequestFailedException("Errors for requestChat. SecureKey: " + secureKey);
                            requestChatSignal.countDown();
                        }
                    } else {
                        log.error("Error ChatChannel message:" + response.getError() + ", referenceId: " + response.getReferenceId());
                    }
                }
            } catch (Exception ex) {
                log.error("Exception when processing Chat message", ex);
            }
        }
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
}
