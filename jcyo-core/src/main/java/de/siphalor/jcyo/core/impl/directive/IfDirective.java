package de.siphalor.jcyo.core.impl.directive;

import de.siphalor.jcyo.core.impl.expression.JcyoExpression;

public record IfDirective(JcyoExpression condition) implements JcyoDirective {
	public static final String NAME = "if";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean isBlockBegin() {
		return true;
	}

	@Override
	public boolean isBlockEnd() {
		return false;
	}

	@Override
	public boolean ends(JcyoDirective blockBegin) {
		return false;
	}
}
