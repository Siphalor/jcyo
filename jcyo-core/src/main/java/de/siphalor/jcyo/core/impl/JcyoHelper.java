package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import lombok.Getter;

public class JcyoHelper {
	private static final String DISABLED_FOR_FLEX_END = " */";
	private static final String DISABLED_FOR_FLEX_END_NO_WHITESPACE = "*/";

	@Getter
	private final String disabledForLine;
	@Getter
	private final String disabledForLineNoWhitespace;
	@Getter
	private final String disabledForFlexStart;
	@Getter
	private final String disabledForFlexStartNoWhitespace;

	public JcyoHelper(JcyoOptions options) {
		var sb = new StringBuilder(5);
		sb.append("//").appendCodePoint(options.disabledPrefix());
		this.disabledForLineNoWhitespace = sb.toString();
		sb.append(' ');
		this.disabledForLine = sb.toString();

		sb.setLength(0);
		sb.append("/*").appendCodePoint(options.disabledPrefix());
		this.disabledForFlexStartNoWhitespace = sb.toString();
		sb.append(' ');
		this.disabledForFlexStart = sb.toString();
	}

	public String disabledForFlexEnd() {
		return DISABLED_FOR_FLEX_END;
	}

	public String disabledForFlexEndNoWhitespace() {
		return DISABLED_FOR_FLEX_END_NO_WHITESPACE;
	}
}
