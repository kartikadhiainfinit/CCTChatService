/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.exceptions;

/**
 *
 * @author xxbamar
 */

public class OpenChatRequestFailedException extends TechnicalException {

    private static final long serialVersionUID = 7635160744106460266L;

    public OpenChatRequestFailedException() {
        super();
    }

    public OpenChatRequestFailedException(final String errorMessage) {
        super(errorMessage);
    }
}