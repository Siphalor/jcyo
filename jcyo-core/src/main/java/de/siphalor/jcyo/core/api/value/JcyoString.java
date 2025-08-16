package de.siphalor.jcyo.core.api.value;

public record JcyoString(String value) implements JcyoValue {
	@Override
	public boolean truthy() {
		return true;
	}
}
