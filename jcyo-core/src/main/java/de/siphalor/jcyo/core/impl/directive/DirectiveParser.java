package de.siphalor.jcyo.core.impl.directive;

import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.expression.ExpressionParser;
import de.siphalor.jcyo.core.impl.expression.JcyoExpression;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

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

		Token nameToken = tokenStream.nextToken();
		String name = getIdentifier(nameToken).orElseThrow(() ->
				new JcyoParseException("Expected a directive identifier token but got " + nameToken)
		);
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
			Token nameToken = tokenStream.nextToken();
			String name = getIdentifier(nameToken).orElseThrow(() -> new JcyoParseException(
					"Expected a directive identifier as end directive target but got " + nameToken)
			);
			chompDirectiveEnd();
			return new EndDirective(name);
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

	private Optional<String> getIdentifier(Token token) {
		return switch (token) {
			case IdentifierToken (String identifier) -> Optional.of(identifier);
			case JavaKeywordToken (JavaKeyword keyword) -> Optional.of(keyword.text());
			default -> Optional.empty();
		};
	}

	private void chompDirectiveEnd() {
		chompWhitespace();
		Token token = tokenStream.nextToken();
		if (commentStyle == CommentStyle.LINE) {
			if (!(token instanceof LineBreakToken)) {
				throw new JcyoParseException("Expected a line break after full line directive, but got: " + token);
			}
		} else {
			if (!(token instanceof JcyoEndToken)) {
				throw new JcyoParseException("Expected end of directive, but got: " + token);
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
