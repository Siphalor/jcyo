package de.siphalor.jcyo.core.api.value;

public record JcyoBoolean(boolean value) implements JcyoValue {
	@Override
	public boolean truthy() {
		return value;
	}
}
