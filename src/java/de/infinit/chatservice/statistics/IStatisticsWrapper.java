/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.statistics;

import de.infinit.chatservice.configuration.beans.ApplicationOptions;
import de.infinit.chatservice.exceptions.BackendUnavailableException;
import de.infinit.chatservice.exceptions.RequestFailedException;

/**
 *
 * @author xxbamar
 */
public interface IStatisticsWrapper {
    int getNumberOfChatsAvailable(ApplicationOptions options) throws RequestFailedException, BackendUnavailableException;
}
