package de.infinit.chatservice.exceptions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.WebFault;

@WebFault
@XmlRootElement
public class InvalidArgumentException extends TechnicalException {
	private static final long serialVersionUID = -9038179754103610847L;

	@XmlElement(required = true, nillable = false)
	private String argumentName;

	public InvalidArgumentException(final String errorMessage) {
		super(errorMessage);
	}

	public InvalidArgumentException(final String errorMessage, final String argumentName) {
		super(errorMessage);

		this.argumentName = argumentName;
	}

	/**
	 * @return the argumentName
	 */
	@XmlElement(required = true, nillable = false)
	public String getArgumentName() {
		return argumentName;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("InvalidArgumentException [argumentName=");
		builder.append(argumentName);
		builder.append(", toString()=");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @param argumentName
	 *          the argumentName to set
	 */
	@XmlElement(required = true, nillable = false)
	public void setArgumentName(final String argumentName) {
		this.argumentName = argumentName;
	}
}
