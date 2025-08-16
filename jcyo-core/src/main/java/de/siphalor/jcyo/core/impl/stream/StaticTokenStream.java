package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.EofToken;
import de.siphalor.jcyo.core.impl.token.Token;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Queue;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StaticTokenStream implements TokenStream {
	private final Queue<Token> tokens;

	@Override
	public Token nextToken() {
		if (tokens.isEmpty()) {
			return EofToken.instance();
		}
		return tokens.remove();
	}
}
