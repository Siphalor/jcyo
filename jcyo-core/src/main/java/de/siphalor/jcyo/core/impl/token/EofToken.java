package de.siphalor.jcyo.core.impl.token;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EofToken implements Token {
	private static final EofToken INSTANCE = new EofToken();

	public static EofToken instance() {
		return INSTANCE;
	}
}
