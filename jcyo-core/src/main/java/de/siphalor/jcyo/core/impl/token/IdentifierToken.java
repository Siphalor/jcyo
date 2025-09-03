package de.siphalor.jcyo.core.impl.token;

public record IdentifierToken(String identifier) implements RepresentableToken {
	@Override
	public String raw() {
		return identifier;
	}
}
