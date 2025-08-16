package de.siphalor.jcyo.core.api;

public class JcyoProcessingException extends Exception {
	public JcyoProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public JcyoProcessingException(String message) {
		super(message);
	}
}
