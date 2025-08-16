package de.siphalor.jcyo.core.impl.directive;

public record ElseDirective() implements JcyoDirective {
	public static final String NAME = "else";

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
