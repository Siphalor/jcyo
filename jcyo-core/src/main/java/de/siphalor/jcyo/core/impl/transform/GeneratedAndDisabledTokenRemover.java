package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.JcyoHelper;
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
	private final JcyoHelper jcyoHelper;
	private final TokenBuffer buffer = new TokenBuffer();
	private boolean inDisabledSection = false;

	public GeneratedAndDisabledTokenRemover(TokenStream inner, JcyoOptions options) {
		this.inner = PeekableTokenStream.from(inner);
		this.jcyoHelper = new JcyoHelper(options);
	}

	@Override
	public Token nextToken() {
		while (true) {
			if (!buffer.isEmpty()) {
				return buffer.nextToken();
			}

			Token token = inner.peekToken();
			switch (token) {
				case JcyoDirectiveStartToken _ -> {
					DirectiveParser parser = new DirectiveParser(PeekableTokenStream.from(buffer.copying(inner)));
					JcyoDirective directive = parser.nextDirective();
					if (!(directive instanceof GeneratedDirective generatedDirective)) {
						continue;
					}
					chompToDirectiveEnd(generatedDirective);
					buffer.clear();
				}
				case JcyoDisabledStartToken _ -> {
					inner.nextToken();
					inDisabledSection = true;
				}
				case JcyoEndToken _ when inDisabledSection -> {
					inner.nextToken();
					inDisabledSection = false;
				}
				case PlainJavaCommentToken commentToken -> {
					inner.nextToken();
					return processPlainJavaComment(commentToken);
				}
				default -> {
					return inner.nextToken();
				}
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

	private PlainJavaCommentToken processPlainJavaComment(PlainJavaCommentToken token) {
		String disabledTokenForLineNoWhitespace = jcyoHelper.disabledForLineNoWhitespace();
		String rawComment = token.raw();
		int disabledTokenMatch = rawComment.indexOf(disabledTokenForLineNoWhitespace);
		if (disabledTokenMatch == -1) {
			return token;
		}

		StringBuilder plainCommentBuilder = new StringBuilder(rawComment.length());
		plainCommentBuilder.append(rawComment, 0, disabledTokenMatch);

		int currentPos = disabledTokenMatch;
		String disabledTokenForLine = jcyoHelper.disabledForLine();
		while (true) {
			if (rawComment.startsWith(disabledTokenForLine, currentPos)) {
				currentPos += disabledTokenForLine.length();
			} else {
				currentPos += disabledTokenForLineNoWhitespace.length();
			}
			disabledTokenMatch = rawComment.indexOf(disabledTokenForLineNoWhitespace, currentPos);
			if (disabledTokenMatch == -1) {
				plainCommentBuilder.append(rawComment, currentPos, rawComment.length());
				break;
			} else {
				plainCommentBuilder.append(rawComment, currentPos, disabledTokenMatch);
				currentPos = disabledTokenMatch;
			}
		}

		return new PlainJavaCommentToken(plainCommentBuilder.toString(), token.commentStyle(), token.javadoc());
	}
}
