package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.token.JcyoDisabledStartToken;
import de.siphalor.jcyo.core.impl.token.JcyoEndToken;
import lombok.Getter;

public class JcyoHelper {
	private static final JcyoEndToken DISABLED_FOR_FLEX_END = new JcyoEndToken(" */");
	private static final JcyoEndToken DISABLED_FOR_FLEX_END_NO_WHITESPACE = new JcyoEndToken("*/");

	@Getter
	private final JcyoDisabledStartToken disabledForLine;
	@Getter
	private final JcyoDisabledStartToken disabledForFlexStart;
	@Getter
	private final JcyoDisabledStartToken disabledForFlexStartNoWhitespace;

	public JcyoHelper(JcyoOptions options) {
		var sb = new StringBuilder(5);
		sb.append("//").appendCodePoint(options.disabledPrefix()).append(' ');
		this.disabledForLine = new JcyoDisabledStartToken(sb.toString(), CommentStyle.LINE);
		sb.setLength(0);
		sb.append("/*").appendCodePoint(options.disabledPrefix()).append(' ');
		this.disabledForFlexStart = new JcyoDisabledStartToken(sb.toString(), CommentStyle.FLEX);
		this.disabledForFlexStartNoWhitespace = new JcyoDisabledStartToken(sb.substring(0, 3), CommentStyle.FLEX);
	}

	public JcyoEndToken disabledForFlexEnd() {
		return DISABLED_FOR_FLEX_END;
	}

	public JcyoEndToken disabledForFlexEndNoWhitespace() {
		return DISABLED_FOR_FLEX_END_NO_WHITESPACE;
	}
}
