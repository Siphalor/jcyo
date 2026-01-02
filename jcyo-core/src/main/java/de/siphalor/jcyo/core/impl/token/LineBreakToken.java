package de.siphalor.jcyo.core.impl.token;

public record LineBreakToken(String raw) implements RepresentableToken {
	private static final LineBreakToken DEFAULT = new LineBreakToken("\n");

	public static LineBreakToken defaultInstance() {
		return DEFAULT;
	}
}
