package de.infinit.chatservice.exceptions;

import javax.xml.ws.WebFault;

@WebFault
public class RequestFailedException extends TechnicalException {
	private static final long serialVersionUID = 7635160744106460266L;

	public RequestFailedException(final String errorMessage) {
		super(errorMessage);
	}
}