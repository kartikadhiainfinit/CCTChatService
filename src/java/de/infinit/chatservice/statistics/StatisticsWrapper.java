/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import de.infinit.chatservice.ChatService;
import de.infinit.chatservice.GmsDataConstants;
import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.RequestFailedException;
import de.infinit.chatservice.utils.JsonHelper;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import javax.ws.rs.ProcessingException;
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

/**
 *
 * @author xxbamar
 */
public class StatisticsWrapper {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(StatisticsWrapper.class);
    private static final String STATISTICS_FILTER = "CHAT_MEDIA";

    private ApplicationOptions options = null;
    private final String tenant;
    
    public StatisticsWrapper(final ApplicationOptions options, final String tenant) {
        if (log.isInfoEnabled()) {
            log.info("[StatisticsWrapper] Input: tenant=" + tenant);
        }

        this.options = options;
        this.tenant = tenant;
    }

    /**
     * Get Number of available chats (Equation: concurrentChatThreshold - currentNumberOfOpenChats - numberOfChatsWaiting)
     *
     * @return returns number of available chats
     * @throws de.infinit.chatservice.exceptions.RequestFailedException
     * @throws de.infinit.chatservice.exceptions.BackendUnavailableException
     */
    public int getNumberOfChatsAvailable() throws RequestFailedException, BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[getNumberOfChatsAvailable] Input: options=[not logged]");
        }

        if (ChatService.getGmsConnector().getBaseStatisticUrl() == null) {
            throw new BackendUnavailableException("Connection to GMS node is not established.");
        }

        int concurrentChatThreshold;
        try {
            if (log.isDebugEnabled()) {
                log.debug("get concurrentChatThreshold from CME configuration..");
            }
            concurrentChatThreshold = options.getStatistics().getConcurrentChatThreshold();
            if (log.isDebugEnabled()) {
                log.debug("concurrentChatThreshold =" + concurrentChatThreshold);
            }
        } catch (final NumberFormatException e) {
            log.error("Configuration contains incorrect value for concurrentChatThreshold (" + options.getStatistics().getConcurrentChatThreshold() + ")", e);
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.warn("Configuration contains incorrect value for concurrentChatThreshold (" + options.getStatistics().getConcurrentChatThreshold() + ")");
            throw new RequestFailedException("Configuration contains incorrect value for concurrentChatThreshold (" + options.getStatistics().getConcurrentChatThreshold() + ")");
        }

        try {
            final int numberOfWaitingChats = getNumberOfChatsWaitingForVQ(options.getStatistics().getGroupOfAllChatQueues(), options);
            final int currentNumberOfOpenChats = getCurrentNumberOfOpenChatsForVQ(options.getStatistics().getGroupOfAllChatAgents(), options);

            if (log.isDebugEnabled()) {
                log.debug("[getNumberOfChatsAvailable] sub-results: numberOfWaitingChats:" + numberOfWaitingChats + " currentNumberOfOpenChats:" + currentNumberOfOpenChats);
            }

            int numberOfAvailableChats = countNumberOfChatsAvailable(concurrentChatThreshold, currentNumberOfOpenChats, numberOfWaitingChats);
            if (log.isDebugEnabled()) {
                log.debug("[getNumberOfChatsAvailable] Output: " + numberOfAvailableChats);
            }
            return numberOfAvailableChats;
        } catch (final RequestFailedException e) {
            log.warn("RequestFailedException occured in getNumberOfChatsAvailable():" + e.getMessage());
            throw e;
        } catch (final BackendUnavailableException e) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.warn("BackendUnavailableException occured in getNumberOfChatsAvailable():" + e.getMessage());
            throw e;
        } catch (final Exception e) {
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.warn("Exception occured in getNumberOfChatsAvailable():" + e.getMessage());
            throw new RequestFailedException("Unexpected");
        }
    }

    /**
     * count Number Of Chats available (Equation: concurrentChatThreshold - currentNumberOfOpenChats - numberOfChatsWaiting)
     *
     * @param concurrentChatThreshold
     * @param currentNumberOfOpenChats
     * @param numberOfChatsWaiting
     * @return
     */
    private int countNumberOfChatsAvailable(final int concurrentChatThreshold, final int currentNumberOfOpenChats, final int numberOfChatsWaiting) {
        final int response = concurrentChatThreshold - currentNumberOfOpenChats - numberOfChatsWaiting;
        if (response < 0) {
            return 0;
        }
        return response;
    }

    public int getNumberOfChatsWaitingForVQ(final String vqName, final ApplicationOptions options) throws RequestFailedException, BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[getNumberOfChatsWaitingForVQ] Input: vqName=" + vqName + ", conf=[not logged]");
        }

        final String statName = options.getStatistics().getWaitingChats();
        final String queueName = vqName;
        final String objectType = "GroupQueues";

        return peekStatistic(statName, objectType, queueName, this.tenant, StatisticsWrapper.STATISTICS_FILTER, options.getStatistics().getStatisticsTimeout());
    }

    public int getCurrentNumberOfOpenChatsForVQ(final String vqName, final ApplicationOptions options) throws RequestFailedException, BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[getCurrentNumberOfOpenChatsForVQ] Input: vqName=" + vqName + ", conf=[not logged]");
        }

        final String statName = options.getStatistics().getCurrentNumberAcceptedChats();
        final String queueName = vqName;
        final String objectType = "GroupAgents";

        return peekStatistic(statName, objectType, queueName, this.tenant, StatisticsWrapper.STATISTICS_FILTER, options.getStatistics().getStatisticsTimeout());
    }

    private Integer peekStatistic(final String metric, final String objectType, final String objectId, final String tenant, final String filter, final int requestTimeout)
            throws RequestFailedException, BackendUnavailableException {
        if (log.isDebugEnabled()) {
            log.debug("[peekStatistic] Input: metric=" + metric + ", objectType=" + objectType + ", objectId=" + objectId + ", tenant=" + tenant + ", filetr=" + filter);
        }

        if (ChatService.getGmsConnector().getBaseStatisticUrl() == null) {
            throw new BackendUnavailableException("Connection to GMS node is not established.");
        }

        HttpAuthenticationFeature feature = null;
        if (options != null && options.getGms().getGmsUser() != null && options.getGms().getGmsPassword() != null) {
            feature = HttpAuthenticationFeature.basic(options.getGms().getGmsUser(), options.getGms().getGmsPassword());
        }

        ClientConfig configuration = new ClientConfig();
        if (requestTimeout > 0) {
            configuration.property(ClientProperties.CONNECT_TIMEOUT, requestTimeout * 1000);
            configuration.property(ClientProperties.READ_TIMEOUT, requestTimeout * 1000);
        }

        Client client = ClientBuilder.newClient(configuration);
        if (feature != null) {
            client.register(feature);
        }

        WebTarget t = client.target(ChatService.getGmsConnector().getBaseStatisticUrl());

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add(GmsDataConstants.KEY_OBJECT_TYPE, objectType);
        formData.add(GmsDataConstants.KEY_OBJECT_ID, objectId);
        formData.add(GmsDataConstants.KEY_TENANT, tenant);
        formData.add(GmsDataConstants.KEY_METRIC, metric);

        if (filter != null) {
            formData.add(GmsDataConstants.KEY_FILTER, filter);
        }

        try {
            Response resp = t.request().post(Entity.form(formData));

            JsonNode responseRootNode;
            responseRootNode = JsonHelper.getJsonRootNode(resp.readEntity(String.class));

            // Respond depending on status
            if (resp.getStatus() == Status.OK.getStatusCode()) {

                // Get callback request id
                if (responseRootNode != null) {
                    IntNode valueNode = (IntNode) responseRootNode.get(GmsDataConstants.KEY_VALUE);
                    if (valueNode != null) {
                        int statValue = valueNode.asInt();

                        if (log.isDebugEnabled()) {
                            log.debug("[peekStatistic] Output: " + statValue);
                        }
                        return statValue;
                    }
                }

                // statvalue not find in the response
                log.error("Unexpected message from StatServer: " + resp);
                log.info("KPITE505: TechnicalException: RequestFailedException.");
                log.warn("Error. Unexpected message from StatServer: " + responseRootNode);
                throw new RequestFailedException("Error. Unexpected message from StatServer: " + responseRootNode);
            } else {
                log.error("Unexpected HTTP response from StatServer: " + resp);
                log.info("KPITE505: TechnicalException: RequestFailedException.");
                log.warn("peekStatistic - RequestFailedException: Unexpected HTTP response from StatServer [HTTP status " + resp.getStatus() + "].");
                throw new RequestFailedException("Unexpected HTTP response from StatServer [HTTP status " + resp.getStatus() + "].");
            }
        } catch (ProcessingException ex) {
            //log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.warn("peekStatistic - ProcessingException: " + ex.getMessage(), ex);
            throw new BackendUnavailableException("ProcessingException: " + ex.getMessage());
        } catch (ConnectException ex) {
            log.info("KPITE505: TechnicalException: BackendUnavailableException.");
            log.warn("peekStatistic - ConnectException: " + ex.getMessage(), ex);
            throw new BackendUnavailableException("ConnectException: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("Statistics request sending error", ex);
            log.info("KPITE505: TechnicalException: RequestFailedException.");
            log.warn("peekStatistic - IOException: " + ex.getMessage());
            throw new RequestFailedException("IOException: " + ex.getMessage());
        }
    }

    private String logPassword(final String password) {
        if (password == null) {
            return "null";
        } else if (password.isEmpty()) {
            return "empty";
        }
        return "[output suppressed]";
    }
}
