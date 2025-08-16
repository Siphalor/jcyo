package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JcyoCommentRemover implements TokenStream {
	private final TokenStream inner;
	private final TokenBuffer whitespaceBuffer = new TokenBuffer();

	@Override
	public Token nextToken() {
		if (!whitespaceBuffer.isEmpty()) {
			return whitespaceBuffer.nextToken();
		}

		while (true) {
			Token token = inner.nextToken();
			switch (token) {
				case WhitespaceToken _ -> whitespaceBuffer.pushToken(token);
				case JcyoDirectiveStartToken startToken -> {
					if (startToken.commentStyle() == CommentStyle.LINE) {
						whitespaceBuffer.clear();
						chompToEndOfLine();
					} else {
						chompToTokenType(JcyoEndToken.class);
					}
				}
				case JcyoDisabledStartToken startToken -> {
					if (startToken.commentStyle() == CommentStyle.LINE) {
						whitespaceBuffer.clear();
						chompToEndOfLine();
					} else {
						chompToTokenType(JcyoEndToken.class);
					}
				}
				default -> {
					if (!whitespaceBuffer.isEmpty()) {
						whitespaceBuffer.pushToken(token);
						return whitespaceBuffer.nextToken();
					}
					return token;
				}
			}
		}
	}

	private void chompToEndOfLine() {
		while (true) {
			switch (inner.nextToken()) {
				case EofToken _, LineBreakToken _ -> { return; }
				default -> {}
			}
		}
	}

	private void chompToTokenType(Class<? extends Token> tokenType) {
		while (true) {
			Token token = inner.nextToken();
			if (tokenType.isInstance(token)) {
				return;
			} else if (token instanceof EofToken) {
				return;
			}
		}
	}
}
