package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.Token;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public class PeekableTokenStream implements TokenStream {
	private final TokenStream inner;
	private @Nullable Token peek;

	@Override
	public Token nextToken() {
		if (peek != null) {
			Token token = peek;
			peek = null;
			return token;
		}
		return inner.nextToken();
	}

	public Token peekToken() {
		if (peek == null) {
			peek = inner.nextToken();
		}
		return peek;
	}
}
