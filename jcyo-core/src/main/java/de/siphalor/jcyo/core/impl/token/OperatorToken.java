package de.siphalor.jcyo.core.impl.token;

public record OperatorToken(int codepoint) implements RepresentableToken {
	@Override
	public String raw() {
		return new String(new int[] { codepoint }, 0, 1);
	}

	@Override
	public String toString() {
		return "OperatorToken[" + raw() + "]";
	}
}
