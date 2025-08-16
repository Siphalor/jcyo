package de.siphalor.jcyo.core.impl.directive;

public record GeneratedDirective() implements JcyoDirective {
	public static final String NAME = "generated";

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
