package de.siphalor.jcyo.core.impl.directive;

import org.jspecify.annotations.Nullable;

public record EndDirective(@Nullable String target) implements JcyoDirective {
	public static final String NAME = "end";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean isBlockBegin() {
		return false;
	}

	@Override
	public boolean isBlockEnd() {
		return true;
	}

	@Override
	public boolean ends(JcyoDirective blockBegin) {
		return target == null || blockBegin.name().equals(target);
	}
}
