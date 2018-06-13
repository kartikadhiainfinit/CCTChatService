/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication;

import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.communication.chatservice.CategoriesChangedRequest;
import de.infinit.chatservice.communication.chatservice.ChatStartedRequest;
import de.infinit.chatservice.communication.chatservice.FinishChatRequest;
import de.infinit.chatservice.communication.chatservice.SendMessageRequest;
import de.infinit.chatservice.communication.chatservice.ServiceAvailabilityChangedRequest;
import de.infinit.chatservice.communication.beans.Agent;
import de.infinit.chatservice.communication.beans.ChatMessage;
import de.infinit.chatservice.communication.beans.ErrorResponse;
import de.infinit.chatservice.communication.chatservice.AgentChangedRequest;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author sebesjir
 */
public class AdapterCommunication {
    
    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AdapterCommunication.class);
    
    public static boolean finishConversation(String sessionId, String userId, String text, String source) {
        boolean requestSent = false;
        if (log.isDebugEnabled()) {
            log.debug("[finishConversation] - started. SessionId: " + sessionId + ", source: " + source);
        }
        FinishChatRequest request = new FinishChatRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        request.setUserId(userId);
        request.setSource("External".equalsIgnoreCase(source) ? "System" : source);
        
        if (text != null && !"".equals(text)) {
            ChatMessage message = new ChatMessage();
            message.setText(text);
            message.setSentTimeMillis(new Date().getTime());
            
            request.getChatMessages().add(message);
        }
        
        if (ChatService.isAdapterConnected()) {
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/chat/" + sessionId + "/finish");
                
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("finishConversation [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send finish conversation: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Finish conversation request " + request + " successfully sent to the client side");
                } else {               
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("\"Cannot send finish conversation: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception ex) {
                log.warn("finishConversation failed for sessionId=" + sessionId, ex);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: finishConversation - Output: " + request);
        }
        
        return requestSent;
    }
    
    public static boolean conversationStarted(String sessionId, String userId, String agentName, String agentId) {
        boolean requestSent = false;
        if (log.isDebugEnabled()) {
            log.debug("[conversationStarted] - started. SessionId: " + sessionId + ", agentName: " + agentName);
        }
        
        ChatStartedRequest request = new ChatStartedRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        Agent agent = new Agent();
        agent.setName(agentName);
        agent.setId(agentId);
        
        request.setAgent(agent);
        request.setUserId(userId);
        
        if (ChatService.isAdapterConnected()) {
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/chat/" + sessionId + "/started");
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("conversationStarted [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send conversation started: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Conversation started request " + request + " successfully sent to the client side");
                } else {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send conversation started: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception ex) {
                log.warn("conversationStarted failed for sessionId=" + sessionId, ex);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: conversationStarted - Output: " + request);
        }
        
        return requestSent;
    }
    
    public static boolean notifyAgentChanged(String sessionId, String userId, String agentName, String agentId) {
        boolean requestSent = false;
        AgentChangedRequest request = new AgentChangedRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        Agent agent = new Agent();
        agent.setName(agentName);
        agent.setId(agentId);
        
        request.setAgent(agent);
        request.setUserId(userId);
        
        if (ChatService.isAdapterConnected()) {
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/chat/" + sessionId + "/agentChanged");
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("notifyAgentChanged [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send agent changed: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Agent changed request " + request + " successfully sent to the client side");
                } else {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send agent changed: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception ex) {
                log.warn("notifyAgentChanged failed for sessionId=" + sessionId, ex);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: notifyAgentChanged - Output: " + request);
        }
        
        return requestSent;
    }
    
    public static boolean serviceAvailabilityChanged() {
        boolean requestSent = false;
        ServiceAvailabilityChangedRequest request = new ServiceAvailabilityChangedRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        try {
            request.setServiceAvailabilities(ResponseFactory.getServiceAvailabilities(null));
        } catch (BackendUnavailableException | RequestFailedException ex) {
            log.warn("Cannot get service availability from Response Factory", ex);
        }
        
        if (ChatService.isAdapterConnected()) {
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/serviceAvailability/changed");
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("serviceAvailabilityChanged [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send the availability of chat service: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Service availability notification request " + request + " successfully sent to the client side");
                } else {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send the availability of chat service: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception ex) {
                log.error("Error when sending serviceAvailabilityChanged request to client", ex);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: notifyServiceAvailabilityChanged - Output: " + request);
        }
        
        return requestSent;
    }
    
    public static boolean categoriesChanged() {
        boolean requestSent = false;
        if (log.isDebugEnabled()) {
            log.debug("[categoriesChanged] - started");
        }
        
        CategoriesChangedRequest request = new CategoriesChangedRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        try {
            request.setChatCategories(ResponseFactory.getCategories());
        } catch (Exception ex) {
            log.warn("Cannot get chat categories from Response Factory", ex);
        }
        
        if (ChatService.isAdapterConnected()) {
            if (log.isInfoEnabled()) {
                log.info("Send categoriesChanged request to " + baseServiceUrl + "/category/changed");
            }
            
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/category/changed");
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("categoriesChanged [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send chat categories: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Chat category notification request " + request + " successfully sent to the client side");
                } else {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send chat categories: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception e) {
                log.warn("The categories changed notification failed with exception:" + e.getMessage(), e);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: notifySupportCategoriesChanged - output: " + request);
        }
        
        return requestSent;
    }
    
    public static boolean sendMessageToClient(String sessionId, String userId, String text, String source) {
        boolean requestSent = false;
        if (log.isDebugEnabled()) {
            log.debug("[sendMessageToClient] - started. SessionId: " + sessionId + ", Source: " + source);
        }
        
        SendMessageRequest request = new SendMessageRequest();
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String serviceUser = options.getAdapterEndpoint().getAdapterUser();
        String servicePassword = options.getAdapterEndpoint().getAdapterPassword();
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        request.setUserId(userId);
        
        ChatMessage message = new ChatMessage();
        message.setText(text);
        message.setSource("External".equalsIgnoreCase(source) ? "System" : source);
        message.setSentTimeMillis(new Date().getTime());
        
        request.getChatMessages().add(message);
        
        if (ChatService.isAdapterConnected()) {
            Client client = getClient(serviceUser, servicePassword);
            try {
                WebTarget t = client.target(baseServiceUrl + "/chat/" + sessionId + "/sendMessage");
                Response resp = t.request().accept("application/json; charset=UTF-8").post(Entity.entity(request, "application/json; charset=UTF-8"));
                
                if (log.isInfoEnabled()) {
                    log.info("sendMessageToClient [request=" + request + "] - Response: " + resp);
                }
                
                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send agent message:" + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                    requestSent = true;
                    log.info("Agent message request " + request + " successfully sent to the client side");
                } else {
                    ErrorResponse error = resp.readEntity(ErrorResponse.class);
                    log.warn("Cannot send agent message: " + resp.getStatus() + ", message: " + error.getErrorMessage());
                    //Check accessibility
                    pingAdapter();
                }
            } catch (Exception ex) {
                log.error("Error when sending sendMessageToClient request to client", ex);
            }
        } else {
            log.info("PRIMARY ADAPTER NOT REGISTERED: sendMessageToClient - output: " + request);
        }
        
        return requestSent;
    }
        
    public static void pingAdapter() {
        ApplicationOptions options = ChatService.getApplicationOptions();
        
        String baseServiceUrl = options.getAdapterEndpoint().getAdapterUrl();
        
        Client client = getClient(null, null);
        try {
            WebTarget t = client.target(baseServiceUrl + "/pingFromCct");
            Response resp = t.request().get();
            
            if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                log.warn("[pingAdapter] Client side not available:" + resp.getStatus());
                ChatService.primaryAdapterAvailability(false);
            } else if (resp.getStatus() == Status.OK.getStatusCode()) {
                ChatService.primaryAdapterAvailability(true);
            } else {
                log.warn("[pingAdapter] Fail: " + resp.getStatus());
                ChatService.primaryAdapterAvailability(false);
            }
        } catch (Exception ex) {
            //log.error("[pingAdapter] Error", ex);
        }
    }
    
    private static Client getClient(String serviceUser, String servicePassword) {
        
        Client client = null;
        
        try {
            client = ClientBuilder.newClient(new ClientConfig());
            if (serviceUser != null && servicePassword != null) {
                HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(serviceUser, servicePassword);
                
                if (feature != null) {
                    client.register(feature);
                }
            }
        } catch (Exception e) {
            log.info("Cannot get client. " + e.getMessage());
        }
        
        return client;
    }
}
