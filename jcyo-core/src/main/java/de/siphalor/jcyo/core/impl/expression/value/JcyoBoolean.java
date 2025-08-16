package de.siphalor.jcyo.core.impl.expression.value;

public record JcyoBoolean(boolean value) implements JcyoValue {
	@Override
	public boolean truthy() {
		return value;
	}
}
