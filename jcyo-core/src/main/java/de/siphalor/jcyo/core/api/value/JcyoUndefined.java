package de.siphalor.jcyo.core.api.value;

public record JcyoUndefined(String origin) implements JcyoValue {
	@Override
	public boolean truthy() {
		return false;
	}
}
