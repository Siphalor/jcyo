package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoProcessingException;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.JcyoHelper;
import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.directive.DirectiveParser;
import de.siphalor.jcyo.core.impl.directive.ElseDirective;
import de.siphalor.jcyo.core.impl.directive.IfDirective;
import de.siphalor.jcyo.core.impl.directive.JcyoDirective;
import de.siphalor.jcyo.core.impl.expression.JcyoExpression;
import de.siphalor.jcyo.core.impl.expression.JcyoExpressionEvaluator;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.Data;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class JcyoDirectiveApplier {
	private final JcyoHelper helper;
	private final JcyoVariables variables;
	private final Deque<StackEntry> stack = new ArrayDeque<>();

	public JcyoDirectiveApplier(JcyoOptions options, JcyoVariables variables) {
		this.helper = new JcyoHelper(options);
		this.variables = variables;
	}

	public TokenStream apply(TokenStream inner) throws JcyoProcessingException {
		var stream = new PeekableTokenStream(inner);

		TokenBuffer result = new TokenBuffer();
		var directiveParser = new DirectiveParser(new PeekableTokenStream(result.copying(stream)));
		var evaluator = new JcyoExpressionEvaluator(variables);

		loop:
		while (true) {
			switch (stream.peekToken()) {
				case EofToken _:
					result.pushToken(stream.nextToken());
					break loop;
				case JcyoDirectiveStartToken startToken:
					currentStackEntry()
							.filter(stackEntry -> stackEntry.disabledActive() && stackEntry.commentStyle() == CommentStyle.FLEX)
							.ifPresent(stackEntry -> {
								result.pushToken(new JcyoEndToken(helper.disabledForFlexEnd()));
								stackEntry.disabledActive(false);
							});

					JcyoDirective directive = directiveParser.nextDirective();
					switch (directive) {
						case IfDirective(JcyoExpression condition) -> {
							boolean disabled = currentStackEntry().map(StackEntry::disabled).orElse(false)
									|| !evaluator.evaluate(condition).truthy();
							stack.push(new StackEntry(directive, disabled, startToken.commentStyle()));
						}
						case ElseDirective _ -> {
							StackEntry entry = stack.pop();
							validateEndDirective(startToken, directive, entry);
							stack.push(new StackEntry(directive, !entry.disabled(), startToken.commentStyle()));
						}
						default -> {
							if (directive.isBlockEnd()) {
								StackEntry entry = stack.pop();
								validateEndDirective(startToken, directive, entry);
							}
							if (directive.isBlockBegin()) {
								currentStackEntry().ifPresentOrElse(
										stackEntry -> stack.push(new StackEntry(
												directive,
												stackEntry.disabled(),
												stackEntry.commentStyle()
										)),
										() -> stack.push(new StackEntry(directive, false, startToken.commentStyle()))
								);
							}
						}
					}
					break;
				case LineBreakToken _:
					currentStackEntry()
							.filter(stackEntry -> stackEntry.commentStyle == CommentStyle.LINE)
							.ifPresent(stackEntry -> stackEntry.disabledActive(false));
					result.pushToken(stream.nextToken());
					break;
				case WhitespaceToken _:
					result.pushToken(stream.nextToken());
					break;
				case Token _:
					currentStackEntry()
							.filter(stackEntry -> stackEntry.disabled() && !stackEntry.disabledActive())
							.ifPresent(stackEntry -> {
								result.pushToken(createDisabledToken(stackEntry.commentStyle()));
								stackEntry.disabledActive(true);
							});
					result.pushToken(stream.nextToken());
					break;
			}
		}

		return result;
	}

	private void validateEndDirective(JcyoDirectiveStartToken startToken, JcyoDirective directive, StackEntry entry) {
		if (!directive.ends(entry.startDirective())) {
			throw new JcyoParseException(
					"Incorrect end directive for "
							+ entry.startDirective()
							+ ": "
							+ directive
			);
		}
		if (entry.commentStyle() != startToken.commentStyle()) {
			throw new JcyoParseException(
					"You must not mix comment styles for start and end of block directives"
			);
		}
	}

	private Optional<StackEntry> currentStackEntry() {
		return Optional.ofNullable(stack.peek());
	}

	private JcyoDisabledStartToken createDisabledToken(CommentStyle commentStyle) {
		return switch (commentStyle) {
			case LINE -> new JcyoDisabledStartToken(helper.disabledForLine(), CommentStyle.LINE);
			case FLEX -> new JcyoDisabledStartToken(helper.disabledForFlexStart(), CommentStyle.FLEX);
		};
	}

	@Data
	private static final class StackEntry {
		private final JcyoDirective startDirective;
		private final boolean disabled;
		private final CommentStyle commentStyle;
		private boolean disabledActive;
	}
}
