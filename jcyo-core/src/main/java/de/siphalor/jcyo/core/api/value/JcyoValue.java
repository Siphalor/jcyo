package de.siphalor.jcyo.core.api.value;

public sealed interface JcyoValue permits JcyoBoolean, JcyoNumber, JcyoString, JcyoUndefined {
	boolean truthy();
}
