package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import lombok.Getter;

public class JcyoHelper {
	private final JcyoOptions options;
	@Getter
	private final String disabledForLine;
	@Getter
	private final String disabledForFlexStart;
	@Getter
	private final String disabledForFlexEnd;

	public JcyoHelper(JcyoOptions options) {
		this.options = options;
		this.disabledForLine = new StringBuilder("//").appendCodePoint(options.disabledPrefix()).append(' ').toString();
		this.disabledForFlexStart = new StringBuilder("/*").appendCodePoint(options.disabledPrefix()).append(' ').toString();
		this.disabledForFlexEnd = " */";
	}
}
