package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.directive.*;
import de.siphalor.jcyo.core.impl.expression.JcyoExpression;
import de.siphalor.jcyo.core.impl.expression.JcyoExpressionEvaluationException;
import de.siphalor.jcyo.core.impl.expression.JcyoExpressionEvaluator;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.*;

public class JcyoDirectiveApplier {
	private final JcyoExpressionEvaluator expressionEvaluator;

	public JcyoDirectiveApplier(JcyoVariables variables) {
		this.expressionEvaluator = new JcyoExpressionEvaluator(variables);
	}

	public TokenStream apply(TokenStream stream) {
		return new StreamTransformer(PeekableTokenStream.from(stream));
	}

	@RequiredArgsConstructor
	private class StreamTransformer implements TokenStream {
		private final PeekableTokenStream input;
		private final TokenBuffer buffer = new TokenBuffer();
		private final Deque<StackEntry> stack = new ArrayDeque<>();
		private int indentLength;
		private final List<String> indentBuilder = new ArrayList<>();

		@Override
		public Token nextToken() {
			while (true) {
				if (!buffer.isEmpty()) {
					return buffer.nextToken();
				}

				switch (input.peekToken()) {
					case EofToken _ -> {
						return input.nextToken();
					}
					case JcyoDirectiveStartToken startToken -> {
						DirectiveParser parser = new DirectiveParser(buffer.copying(input));
						JcyoDirective directive = parser.nextDirective();
						evaluateDirective(startToken, directive);
					}
					case LineBreakToken _ -> {
						clearIndent();
						return input.nextToken();
					}
					case RepresentableToken representableToken -> {
						pushIndent(representableToken.raw());
						return input.nextToken();
					}
					case Token _ -> {
						return input.nextToken();
					}
				}
			}
		}

		private void pushIndent(String indent) {
			indentBuilder.add(indent);
			indentLength += indent.length();
		}

		private void clearIndent() {
			indentBuilder.clear();
			indentLength = 0;
		}

		private void evaluateDirective(JcyoDirectiveStartToken startToken, JcyoDirective directive) {
			switch (directive) {
				case IfDirective(JcyoExpression condition) -> {
					try {
						boolean enabled = isCurrentStackEntryEnabled()
								&& expressionEvaluator.evaluate(condition).truthy();
						pushStackEntry(new StackEntry(directive, enabled, startToken.commentStyle()));
					} catch (JcyoExpressionEvaluationException e) {
						throw new DirectiveApplicationException("Failed to evaluate if condition", e);
					}
				}
				case ElifDirective(JcyoExpression condition) -> {
					try {
						StackEntry oldEntry = popStackEntry();
						validateEndDirective(startToken, directive, oldEntry);

						boolean enabled = isCurrentStackEntryEnabled()
								&& !oldEntry.encounteredEnabledBranch()
								&& expressionEvaluator.evaluate(condition).truthy();

						StackEntry newEntry = new StackEntry(directive, enabled, startToken.commentStyle());
						newEntry.encounteredEnabledBranch(oldEntry.encounteredEnabledBranch() || enabled);

						pushStackEntry(newEntry);
					} catch (JcyoExpressionEvaluationException e) {
						throw new DirectiveApplicationException("Failed to evaluate elif condition", e);
					}
				}
				case ElseDirective _ -> {
					StackEntry oldEntry = popStackEntry();
					validateEndDirective(startToken, directive, oldEntry);

					boolean enabled = isCurrentStackEntryEnabled()
							&& !oldEntry.encounteredEnabledBranch();
					StackEntry newEntry = new StackEntry(directive, enabled, startToken.commentStyle());
					newEntry.encounteredEnabledBranch(oldEntry.encounteredEnabledBranch() || enabled);

					pushStackEntry(newEntry);
				}
				default -> {
					if (directive.isBlockEnd()) {
						StackEntry entry = popStackEntry();
						validateEndDirective(startToken, directive, entry);
					}
					if (directive.isBlockBegin()) {
						currentStackEntry().ifPresentOrElse(
								stackEntry -> pushStackEntry(new StackEntry(
										directive,
										stackEntry.enabled(),
										stackEntry.commentStyle()
								)),
								() -> pushStackEntry(new StackEntry(directive, false, startToken.commentStyle()))
						);
					}
				}
			}
		}

		private boolean isCurrentStackEntryEnabled() {
			return currentStackEntry().map(StackEntry::enabled).orElse(true);
		}

		private void pushStackEntry(StackEntry entry) {
			if (!entry.enabled() && isCurrentStackEntryEnabled()) {
				buffer.pushToken(new JcyoDisabledRegionStartToken(
						entry.commentStyle(),
						entry.commentStyle() == CommentStyle.LINE ? concatIndent() : ""
				));
			}
			stack.push(entry);
		}

		private StackEntry popStackEntry() {
			StackEntry innerEntry = stack.pop();
			if (!innerEntry.enabled() && isCurrentStackEntryEnabled()) {
				buffer.pushFrontToken(new JcyoDisabledRegionEndToken());
			}
			return innerEntry;
		}

		private String concatIndent() {
			var sb = new StringBuilder(indentLength);
			for (var indent : indentBuilder) {
				sb.append(indent);
			}
			return sb.toString();
		}

		private void validateEndDirective(
				JcyoDirectiveStartToken startToken,
				JcyoDirective directive,
				StackEntry entry
		) {
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
	}

	@Data
	private static final class StackEntry {
		private final JcyoDirective startDirective;
		private final boolean enabled;
		private final CommentStyle commentStyle;
		private boolean encounteredEnabledBranch;

		public StackEntry(JcyoDirective startDirective, boolean enabled, CommentStyle commentStyle) {

			this.startDirective = startDirective;
			this.enabled = enabled;
			this.commentStyle = commentStyle;
			this.encounteredEnabledBranch = enabled;
		}
	}

	private static class DirectiveApplicationException extends RuntimeException {
		public DirectiveApplicationException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
