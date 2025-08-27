package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoProcessingException;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.JcyoHelper;
import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.directive.*;
import de.siphalor.jcyo.core.impl.expression.JcyoExpression;
import de.siphalor.jcyo.core.impl.expression.JcyoExpressionEvaluator;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class JcyoDirectiveApplier {
	private final JcyoHelper helper;
	private final JcyoExpressionEvaluator expressionEvaluator;

	public JcyoDirectiveApplier(JcyoOptions options, JcyoVariables variables) {
		this.helper = new JcyoHelper(options);
		this.expressionEvaluator = new JcyoExpressionEvaluator(variables);
	}

	public TokenStream apply(TokenStream stream) throws JcyoProcessingException {
		StreamTransformer transformer = new StreamTransformer(new PeekableTokenStream(stream));
		transformer.run();
		return transformer.output();
	}

	@RequiredArgsConstructor
	private class StreamTransformer {
		private final PeekableTokenStream input;
		@Getter
		private final TokenBuffer output = new TokenBuffer();
		private final Deque<StackEntry> stack = new ArrayDeque<>();
		private boolean lastTokenWasWhitespace = false;

		public void run() throws JcyoProcessingException {
			var directiveParser = new DirectiveParser(new PeekableTokenStream(output.copying(input)));

			loop:
			while (true) {
				switch (input.peekToken()) {
					case EofToken _ -> {
						output.pushToken(input.nextToken());
						break loop;
					}
					case JcyoDirectiveStartToken startToken -> {
						explicitlyEndFlexDisabledRegion();

						evaluateDirective(startToken, directiveParser.nextDirective());

						lastTokenWasWhitespace = false;
					}
					case PlainJavaCommentToken(_, CommentStyle commentStyle, boolean javadoc)
							when commentStyle == CommentStyle.LINE -> {
						Token commentToken = input.nextToken();
						pushDisabledTokenIfNecessary();
						output.pushToken(commentToken);
						lastTokenWasWhitespace = false;
					}
					case PlainJavaCommentToken(_, CommentStyle commentStyle, _)
							when commentStyle == CommentStyle.FLEX -> {
						Token commentToken = input.nextToken();
						pushDisabledTokenIfNecessary();
						output.pushToken(commentToken);
						implicitlyEndFlexDisabledRegion();
						lastTokenWasWhitespace = false;
					}
					case LineBreakToken _ -> {
						Token lineBreakToken = input.nextToken();
						pushDisabledTokenIfNecessary();
						currentStackEntry()
								.filter(stackEntry -> stackEntry.commentStyle == CommentStyle.LINE)
								.ifPresent(stackEntry -> stackEntry.disabledActive(false));
						output.pushToken(lineBreakToken);
						lastTokenWasWhitespace = false;
					}
					case WhitespaceToken _ -> {
						currentStackEntryIfDisabledTokenRequired()
								.filter(stackEntry -> stackEntry.commentStyle == CommentStyle.FLEX)
								.ifPresent(stackEntry -> {
									output.pushToken(createDisabledStartToken(stackEntry.commentStyle));
									stackEntry.disabledActive(true);
								});
						output.pushToken(input.nextToken());
						lastTokenWasWhitespace = true;
					}
					case Token _ -> {
						pushDisabledTokenIfNecessary();
						output.pushToken(input.nextToken());
						lastTokenWasWhitespace = false;
					}
				}
			}
		}

		private void evaluateDirective(JcyoDirectiveStartToken startToken, JcyoDirective directive)
				throws JcyoProcessingException {
			switch (directive) {
				case IfDirective(JcyoExpression condition) -> {
					boolean enabled = isCurrentStackEntryEnabled() && expressionEvaluator.evaluate(condition).truthy();
					stack.push(new StackEntry(directive, enabled, startToken.commentStyle()));
				}
				case ElifDirective(JcyoExpression condition) -> {
					StackEntry oldEntry = stack.pop();
					validateEndDirective(startToken, directive, oldEntry);

					boolean enabled = isCurrentStackEntryEnabled()
							&& !oldEntry.encounteredEnabledBranch()
							&& expressionEvaluator.evaluate(condition).truthy();

					StackEntry newEntry = new StackEntry(directive, enabled, startToken.commentStyle());
					newEntry.encounteredEnabledBranch(oldEntry.encounteredEnabledBranch() || enabled);

					stack.push(newEntry);
				}
				case ElseDirective _ -> {
					StackEntry oldEntry = stack.pop();
					validateEndDirective(startToken, directive, oldEntry);

					boolean enabled = isCurrentStackEntryEnabled()
							&& !oldEntry.encounteredEnabledBranch();
					StackEntry newEntry = new StackEntry(directive, enabled, startToken.commentStyle());
					newEntry.encounteredEnabledBranch(oldEntry.encounteredEnabledBranch() || enabled);

					stack.push(newEntry);
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
										stackEntry.enabled(),
										stackEntry.commentStyle()
								)),
								() -> stack.push(new StackEntry(directive, false, startToken.commentStyle()))
						);
					}
				}
			}
		}

		private void explicitlyEndFlexDisabledRegion() {
			currentStackEntry()
					.filter(stackEntry -> stackEntry.commentStyle() == CommentStyle.FLEX && stackEntry.disabledActive())
					.ifPresent(stackEntry -> {
						output.pushToken(createDisabledFlexEndToken());
						stackEntry.disabledActive(false);
					});
		}

		private void implicitlyEndFlexDisabledRegion() {
			currentStackEntry()
					.filter(stackEntry -> stackEntry.commentStyle() == CommentStyle.FLEX && stackEntry.disabledActive())
					.ifPresent(stackEntry -> {
						output.pushToken(JcyoEndToken.implicit());
						stackEntry.disabledActive(false);
					});
		}

		private boolean isCurrentStackEntryEnabled() {
			return currentStackEntry().map(StackEntry::enabled).orElse(true);
		}

		private void pushDisabledTokenIfNecessary() {
			currentStackEntryIfDisabledTokenRequired()
					.ifPresent(stackEntry -> {
						output.pushToken(createDisabledStartToken(stackEntry.commentStyle()));
						stackEntry.disabledActive(true);
					});
		}

		private Optional<StackEntry> currentStackEntryIfDisabledTokenRequired() {
			return currentStackEntry()
					.filter(stackEntry -> !stackEntry.enabled() && !stackEntry.disabledActive());
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

		private JcyoDisabledStartToken createDisabledStartToken(CommentStyle commentStyle) {
			return switch (commentStyle) {
				case LINE -> helper.disabledForLine();
				case FLEX -> {
					Token peek = input.peekToken();
					if (peek instanceof WhitespaceToken || peek instanceof LineBreakToken) {
						yield helper.disabledForFlexStartNoWhitespace();
					} else {
						yield helper.disabledForFlexStart();
					}
				}
			};
		}

		private JcyoEndToken createDisabledFlexEndToken() {
			if (lastTokenWasWhitespace) {
				return helper.disabledForFlexEndNoWhitespace();
			} else {
				return helper.disabledForFlexEnd();
			}
		}
	}

	@Data
	private static final class StackEntry {
		private final JcyoDirective startDirective;
		private final boolean enabled;
		private final CommentStyle commentStyle;
		private boolean disabledActive;
		private boolean encounteredEnabledBranch;

		public StackEntry(JcyoDirective startDirective, boolean enabled, CommentStyle commentStyle) {

			this.startDirective = startDirective;
			this.enabled = enabled;
			this.commentStyle = commentStyle;
			this.encounteredEnabledBranch = enabled;
		}
	}
}
