package de.siphalor.jcyo.core.impl.directive;

import de.siphalor.jcyo.core.impl.expression.JcyoExpression;

public record ElifDirective(JcyoExpression condition) implements JcyoDirective {
	public static final String NAME = "elif";

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
		return true;
	}

	@Override
	public boolean ends(JcyoDirective blockBegin) {
		return blockBegin instanceof IfDirective;
	}
}
