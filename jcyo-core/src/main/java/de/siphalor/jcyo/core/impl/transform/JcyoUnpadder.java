package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;

public class JcyoUnpadder implements TokenStream {
	private final PeekableTokenStream inner;

	private boolean inDisabledFlexComment = false;

	public JcyoUnpadder(TokenStream inner) {
		this.inner = PeekableTokenStream.from(inner);
	}

	@Override
	public Token nextToken() {
		Token token = inner.nextToken();
		return switch (token) {
			case JcyoDisabledStartToken disabledStartToken -> {
				inDisabledFlexComment = true;
				if (inner.peekToken() instanceof WhitespaceToken whitespaceToken
						&& whitespaceToken.codepoint() == ' ') {
					inner.nextToken();
				}
				yield  disabledStartToken;
			}
			case JcyoDirectiveStartToken directiveStartToken -> {
				inDisabledFlexComment = false;
				yield  directiveStartToken;
			}
			case WhitespaceToken whitespaceToken when whitespaceToken.codepoint() == ' ' -> {
				if (inDisabledFlexComment && inner.peekToken() instanceof JcyoEndToken) {
					yield inner.nextToken();
				}
				yield whitespaceToken;
			}
			default -> token;
		};
	}
}
