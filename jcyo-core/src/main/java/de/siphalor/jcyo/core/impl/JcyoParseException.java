package de.siphalor.jcyo.core.impl;

public class JcyoParseException extends RuntimeException {
	public JcyoParseException(String message) {
		super(message);
	}

	public JcyoParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public JcyoParseException(Throwable cause) {
		super(cause);
	}
}
