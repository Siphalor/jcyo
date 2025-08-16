package de.siphalor.jcyo.core.api.value;

public sealed interface JcyoValue permits JcyoBoolean, JcyoNumber, JcyoString, JcyoUndefined {
	static JcyoValue of(Object value) {
		return switch (value) {
			case Boolean b -> new JcyoBoolean(b);
			case Number n -> new JcyoNumber(n.doubleValue());
			case String s -> new JcyoString(s);
			default -> throw new IllegalArgumentException(
					"Cannot convert " + value + " of type " + value.getClass().getName() + " to a JcyoValue"
			);
		};
	}

	boolean truthy();
}
