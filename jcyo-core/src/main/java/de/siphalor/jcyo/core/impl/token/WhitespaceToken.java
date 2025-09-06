package de.siphalor.jcyo.core.impl.token;

import lombok.Value;

@Value
public class WhitespaceToken implements RepresentableToken {
	int codepoint;
	String raw;

	public WhitespaceToken(int codepoint) {
		this.codepoint = codepoint;
		this.raw = new String(new int[]{codepoint}, 0, 1);
	}
}
