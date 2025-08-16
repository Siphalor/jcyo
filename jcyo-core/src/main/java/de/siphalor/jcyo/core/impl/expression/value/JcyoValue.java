package de.siphalor.jcyo.core.impl.expression.value;

public sealed interface JcyoValue permits JcyoBoolean, JcyoNumber, JcyoUndefined {
	boolean truthy();
}
