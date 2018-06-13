/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.communication;

import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.configuration.beans.ConnectionServerInfo;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 *
 * @author xxbamar
 */
public class ChatServiceCommunication {

    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ChatServiceCommunication.class);

    public static void claimSessionAsPrimary(String sessionId) {
        if (log.isDebugEnabled()) {
            log.debug("[claimSessionAsPrimary] - started. SessionId: " + sessionId);
        }

        for (ConnectionServerInfo chatService : ChatService.getOtherChatServices()) {
            Client client = getClient();
            try {
                String url = chatService.getHost() + ":" + chatService.getPort() + "/claimSessionAsPrimary/" + sessionId;
                WebTarget t = client.target(url);
                log.info("Sending claimSessionAsPrimary to " + url);
                t.request().async().get();
            } catch (Exception ex) {
                log.error("[claimSessionAsPrimary] Error", ex);
            }
        }
    }

    public static void pingChatServices() {
        if (log.isDebugEnabled()) {
            log.debug("[pingChatServices] - started.");
        }
        for (ConnectionServerInfo chatService : ChatService.getOtherChatServices()) {
            Client client = getClient();

            try {
                String url = chatService.getHost() + ":" + chatService.getPort() + "/pingFromChatService/";
                WebTarget t = client.target(url);
                log.info("Sending pingChatServices to " + url);
                Response resp = t.request().get();

                if (resp.getStatus() == Status.SERVICE_UNAVAILABLE.getStatusCode()) {
                    log.warn("[pingChatServices] ChatService not available:" + resp.getStatus());
                    ChatService.claimSessions(chatService.getName());
                } else if (resp.getStatus() == Status.OK.getStatusCode()) {

                } else {
                    log.warn("[pingChatServices] Fail: " + resp.getStatus());
                    ChatService.claimSessions(chatService.getName());
                }
            } catch (Exception ex) {
                //log.error("[pingChatServices] Error", ex);
            }
        }
    }

    private static Client getClient() {
        Client client = null;

        try {
            client = ClientBuilder.newClient(new ClientConfig());
        } catch (Exception e) {
            log.info("Cannot get client. " + e.getMessage());
        }

        return client;
    }
}
