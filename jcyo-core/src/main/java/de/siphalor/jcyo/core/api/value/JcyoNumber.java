package de.siphalor.jcyo.core.api.value;

public record JcyoNumber(double value) implements JcyoValue {
	@Override
	public boolean truthy() {
		return value != 0D;
	}
}
