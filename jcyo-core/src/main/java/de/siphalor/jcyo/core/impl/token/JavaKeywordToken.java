package de.siphalor.jcyo.core.impl.token;

public record JavaKeywordToken(JavaKeyword keyword) implements Token {
	@Override
	public String raw() {
		return keyword.text();
	}
}
