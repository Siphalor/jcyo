package de.siphalor.jcyo.core.impl.token;

public record JcyoEndToken(String raw) implements RepresentableToken {
	private static final JcyoEndToken IMPLICIT = new JcyoEndToken("");
	public static JcyoEndToken implicit() {
		return IMPLICIT;
	}
}
