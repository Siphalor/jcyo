package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.directive.DirectiveParser;
import de.siphalor.jcyo.core.impl.directive.GeneratedDirective;
import de.siphalor.jcyo.core.impl.directive.JcyoDirective;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;

public class GeneratedAndDisabledTokenRemover implements TokenStream {
	private final PeekableTokenStream inner;
	private final TokenBuffer buffer = new TokenBuffer();

	public GeneratedAndDisabledTokenRemover(TokenStream inner) {
		this.inner = new PeekableTokenStream(inner);
	}

	@Override
	public Token nextToken() {
		while (true) {
			if (!buffer.isEmpty()) {
				return buffer.nextToken();
			}

			Token token = inner.peekToken();
			if (token instanceof JcyoDirectiveStartToken) {
				DirectiveParser parser = new DirectiveParser(new PeekableTokenStream(buffer.copying(inner)));
				JcyoDirective directive = parser.nextDirective();
				if (!(directive instanceof GeneratedDirective generatedDirective)) {
					continue;
				}
				chompToDirectiveEnd(generatedDirective);
				buffer.clear();
			} else if (token instanceof JcyoDisabledStartToken || token instanceof JcyoDirectiveEndToken) {
				inner.nextToken();
				chompWhitespace();
			} else {
				return inner.nextToken();
			}
		}
	}

	private void chompToDirectiveEnd(GeneratedDirective startDirective) {
		while (true) {
			Token token = inner.peekToken();
			if (token instanceof EofToken) {
				return;
			}
			if (token instanceof JcyoDirectiveStartToken) {
				JcyoDirective directive = new DirectiveParser(inner).nextDirective();
				if (!directive.ends(startDirective)) {
					throw new JcyoParseException("Nesting directives is not allowed inside generated code");
				}
				return;
			}
			inner.nextToken();
		}
	}

	private void chompWhitespace() {
		while (true) {
			Token token = inner.peekToken();
			if (token instanceof WhitespaceToken) {
				inner.nextToken();
			} else {
				break;
			}
		}
	}
}
