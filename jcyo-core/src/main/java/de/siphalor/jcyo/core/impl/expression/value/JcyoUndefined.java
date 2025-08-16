package de.siphalor.jcyo.core.impl.expression.value;

public record JcyoUndefined(String origin) implements JcyoValue {
	@Override
	public boolean truthy() {
		return false;
	}
}
