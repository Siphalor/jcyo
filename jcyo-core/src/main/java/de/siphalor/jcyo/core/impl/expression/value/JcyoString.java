package de.siphalor.jcyo.core.impl.expression.value;

public record JcyoString(String value) implements JcyoValue {
	@Override
	public boolean truthy() {
		return !value.isEmpty();
	}
}
