package de.infinit.chatservice.exceptions;

import javax.xml.ws.WebFault;

@WebFault(name = "TechnicalException")
public class TechnicalException extends java.lang.Exception {
	private static final long serialVersionUID = 1871367779072710509L;

	public TechnicalException() {
		super();
	}

	public TechnicalException(final String errorMessage) {
		super(errorMessage);
	}
}