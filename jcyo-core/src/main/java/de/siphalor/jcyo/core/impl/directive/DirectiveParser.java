package de.siphalor.jcyo.core.impl.directive;

import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.expression.ExpressionParser;
import de.siphalor.jcyo.core.impl.expression.JcyoExpression;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public class DirectiveParser {
	private final PeekableTokenStream tokenStream;
	private final ExpressionParser expressionParser;
	private @Nullable CommentStyle commentStyle;

	public DirectiveParser(PeekableTokenStream tokenStream) {
		this.tokenStream = tokenStream;
		this.expressionParser = new ExpressionParser(tokenStream);
	}

	public JcyoDirective nextDirective() {
		Token token = tokenStream.nextToken();
		if (!(token instanceof JcyoDirectiveStartToken startToken)) {
			throw new IllegalArgumentException("Expected a directive start token but got " + token);
		}
		commentStyle = startToken.commentStyle();
		chompWhitespace();

		token = tokenStream.nextToken();
		if (!isIdentifier(token)) {
			throw new JcyoParseException("Expected a directive identifier token but got " + token);
		}
		String name = token.raw();
		return switch (name) {
			case EndDirective.NAME -> parseEndDirective();
			case GeneratedDirective.NAME -> parseGeneratedDirective();
			case IfDirective.NAME -> parseIfDirective();
			case ElifDirective.NAME -> parseElifDirective();
			case ElseDirective.NAME -> parseElseDirective();
			default -> throw new JcyoParseException("Unknown directive " + name);
		};
	}

	private EndDirective parseEndDirective() {
		chompWhitespace();
		Token token = tokenStream.peekToken();
		if (token instanceof OperatorToken(int codepoint) && codepoint == ':') {
			tokenStream.nextToken();
			chompWhitespace();
			token = tokenStream.nextToken();
			if (!isIdentifier(token)) {
				throw new JcyoParseException("Expected a directive identifier as end directive target but got " + token);
			}
			chompDirectiveEnd();
			return new EndDirective(token.raw());
		}
		chompDirectiveEnd();
		return new EndDirective(null);
	}

	private GeneratedDirective parseGeneratedDirective() {
		chompDirectiveEnd();
		return new GeneratedDirective();
	}

	private IfDirective parseIfDirective() {
		chompWhitespace();
		JcyoExpression condition = expressionParser.nextExpression();
		chompDirectiveEnd();
		return new IfDirective(condition);
	}

	private ElifDirective parseElifDirective() {
		chompWhitespace();
		JcyoExpression condition = expressionParser.nextExpression();
		chompDirectiveEnd();
		return new ElifDirective(condition);
	}

	private ElseDirective parseElseDirective() {
		chompDirectiveEnd();
		return new ElseDirective();
	}

	private boolean isIdentifier(Token token) {
		return token instanceof IdentifierToken || token instanceof JavaKeywordToken;
	}

	private void chompDirectiveEnd() {
		chompWhitespace();
		Token token = tokenStream.nextToken();
		if (commentStyle == CommentStyle.LINE) {
			if (!(token instanceof LineBreakToken)) {
				throw new JcyoParseException("Expected a line break after full line directive");
			}
		} else {
			if (!(token instanceof JcyoEndToken)) {
				throw new JcyoParseException("Expected end of directive");
			}
		}
	}

	private void chompWhitespace() {
		while (true) {
			Token token = tokenStream.peekToken();
			if (token instanceof WhitespaceToken || commentStyle == CommentStyle.FLEX && token instanceof LineBreakToken) {
				tokenStream.nextToken();
			} else {
				break;
			}
		}
	}
}
