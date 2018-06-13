package de.infinit.chatservice.exceptions;

import javax.xml.ws.WebFault;

@WebFault
public class InvalidSessionIdException extends InvalidArgumentException {
	private static final long serialVersionUID = -9038179754103610847L;

	public static final String ARGUMENT_NAME = "chatId";

	public InvalidSessionIdException() {
		super(null, ARGUMENT_NAME);
	}

	public InvalidSessionIdException(final String errorMessage) {
		super(errorMessage, ARGUMENT_NAME);
	}
}
