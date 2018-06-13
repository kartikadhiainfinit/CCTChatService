package de.infinit.chatservice.exceptions;

import javax.xml.ws.WebFault;

@WebFault
public class BackendUnavailableException extends TechnicalException {
	private static final long serialVersionUID = 7635160744106460266L;

	public BackendUnavailableException() {
		super();
	}

	public BackendUnavailableException(final String errorMessage) {
		super(errorMessage);
	}
}