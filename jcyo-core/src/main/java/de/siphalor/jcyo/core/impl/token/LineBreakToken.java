package de.siphalor.jcyo.core.impl.token;

public record LineBreakToken(String raw) implements RepresentableToken {
	private static final LineBreakToken DEFAULT = new LineBreakToken("\n");

	public static LineBreakToken defaultInstance() {
		return DEFAULT;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(20 + raw.length() + raw.length());
		sb.append("LineBreakToken[raw=");
		raw.codePoints().forEach(cp -> {
			switch (cp) {
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				default:
					sb.appendCodePoint(cp);
					break;
			}
		});
		sb.append("]");
		return sb.toString();
	}
}
