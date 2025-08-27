package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.EofToken;
import de.siphalor.jcyo.core.impl.token.Token;

import java.util.ArrayDeque;
import java.util.Queue;

public class TokenBuffer implements TokenStream {
	private final Queue<Token> buffer = new ArrayDeque<>();
	private boolean eofPushed;
	private boolean eofReached;

	public void pushToken(Token token) {
		if (eofPushed) {
			throw new IllegalStateException("EOF token already pushed, but got: " + token);
		}
		if (token instanceof EofToken) {
			eofPushed = true;
		}
		buffer.add(token);
	}

	public void clear() {
		buffer.clear();
	}

	@Override
	public Token nextToken() {
		if (eofReached) {
			return EofToken.instance();
		}
		Token token = buffer.remove();
		if (token instanceof EofToken) {
			eofReached = true;
		}
		return token;
	}

	public boolean isEmpty() {
		return buffer.isEmpty();
	}

	public TokenStream copying(TokenStream other) {
		return () -> {
			Token token = other.nextToken();
			if (!(eofPushed && token instanceof EofToken)) {
				pushToken(token);
			}
			return token;
		};
	}
}
