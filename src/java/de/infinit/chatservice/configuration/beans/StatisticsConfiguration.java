/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.configuration.beans;

/**
 *
 * @author xxbamar
 */
public class StatisticsConfiguration {
    private Integer concurrentChatThreshold;
    private String groupOfAllChatAgents;
    private String groupOfAllChatQueues;
    private Integer statisticsTimeout;
    private String currentNumberAcceptedChats;
    private String waitingChats;

    public Integer getConcurrentChatThreshold() {
        return concurrentChatThreshold;
    }

    public void setConcurrentChatThreshold(Integer concurrentChatThreshold) {
        this.concurrentChatThreshold = concurrentChatThreshold;
    }

    public String getGroupOfAllChatAgents() {
        return groupOfAllChatAgents;
    }

    public void setGroupOfAllChatAgents(String groupOfAllChatAgents) {
        this.groupOfAllChatAgents = groupOfAllChatAgents;
    }

    public String getGroupOfAllChatQueues() {
        return groupOfAllChatQueues;
    }

    public void setGroupOfAllChatQueues(String groupOfAllChatQueues) {
        this.groupOfAllChatQueues = groupOfAllChatQueues;
    }

    public Integer getStatisticsTimeout() {
        return statisticsTimeout;
    }

    public void setStatisticsTimeout(Integer statisticsTimeout) {
        this.statisticsTimeout = statisticsTimeout;
    }

    public String getCurrentNumberAcceptedChats() {
        return currentNumberAcceptedChats;
    }

    public void setCurrentNumberAcceptedChats(String currentNumberAcceptedChats) {
        this.currentNumberAcceptedChats = currentNumberAcceptedChats;
    }

    public String getWaitingChats() {
        return waitingChats;
    }

    public void setWaitingChats(String waitingChats) {
        this.waitingChats = waitingChats;
    }
    
}
