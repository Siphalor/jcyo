package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.Token;
import org.jspecify.annotations.Nullable;

public interface PeekableTokenStream extends TokenStream {
	static PeekableTokenStream from(TokenStream tokenStream) {
		return new PeekableTokenStreamImpl(tokenStream);
	}

	@Override
	Token nextToken();

	Token peekToken();

	class PeekableTokenStreamImpl implements PeekableTokenStream {
		private final TokenStream inner;
		private @Nullable Token peek;

		private PeekableTokenStreamImpl(TokenStream inner) {
			this.inner = inner;
		}

		@Override
		public Token nextToken() {
			if (peek != null) {
				Token token = peek;
				peek = null;
				return token;
			}
			return inner.nextToken();
		}

		@Override
		public Token peekToken() {
			if (peek == null) {
				peek = inner.nextToken();
			}
			return peek;
		}
	}
}
