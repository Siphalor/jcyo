package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.api.value.JcyoBoolean;
import de.siphalor.jcyo.core.api.value.JcyoNumber;
import de.siphalor.jcyo.core.api.value.JcyoString;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.*;

@RequiredArgsConstructor
public class ExpressionParser {
	private final PeekableTokenStream tokenStream;

	public JcyoExpression nextExpression() {
		List<JcyoExpression> values = new ArrayList<>();
		values.add(nextValueExpression());
		List<JcyoBinaryOperator.Type> binaryOperators = new ArrayList<>();

		while (true) {
			chompWhitespace();
			Optional<JcyoBinaryOperator.Type> binaryOperator = tryParseBinaryOperator();
			if (binaryOperator.isEmpty()) {
				break;
			}
			chompWhitespace();
			binaryOperators.add(binaryOperator.get());
			values.add(nextValueExpression());
		}

		return stitchBinaryOperators(values, binaryOperators);
	}

	private JcyoExpression nextValueExpression() {
		chompWhitespace();
		return tryParseParenthesizedExpression()
				.or(this::tryParsePrefixedExpression)
				.or(this::tryParseConstant)
				.or(this::tryParseVariableReference)
				.orElseThrow(() -> new JcyoParseException("Unexpected token " + tokenStream.peekToken()));
	}

	private Optional<JcyoExpression> tryParseParenthesizedExpression() {
		if (tokenStream.peekToken() instanceof OperatorToken(int codepoint) && codepoint == '(') {
			tokenStream.nextToken();
			JcyoExpression inner = nextExpression();
			chompWhitespace();
			if (!(tokenStream.nextToken() instanceof OperatorToken(int codepoint2)) || codepoint2 != ')') {
				throw new JcyoParseException("Expected closing parenthesis, got: " + tokenStream.peekToken());
			}

			return Optional.of(inner);
		}
		return Optional.empty();
	}

	private Optional<JcyoExpression> tryParsePrefixedExpression() {
		Token token = tokenStream.peekToken();
		if (token instanceof OperatorToken(int codepoint)) {
			switch (codepoint) {
				case '-' -> {
					tokenStream.nextToken();
					return Optional.of(new JcyoUnaryOperator(JcyoUnaryOperator.Type.MINUS, nextValueExpression()));
				}
				case '!' -> {
					tokenStream.nextToken();
					return Optional.of(new JcyoUnaryOperator(JcyoUnaryOperator.Type.NOT, nextValueExpression()));
				}
			}
		}
		return Optional.empty();
	}

	private Optional<JcyoConstant> tryParseConstant() {
		Token token = tokenStream.peekToken();
		return switch (token) {
			case JavaKeywordToken(JavaKeyword keyword) -> {
				if (keyword == JavaKeyword.TRUE) {
					tokenStream.nextToken();
					yield Optional.of(new JcyoConstant(new JcyoBoolean(true)));
				} else if (keyword == JavaKeyword.FALSE) {
					tokenStream.nextToken();
					yield Optional.of(new JcyoConstant(new JcyoBoolean(false)));
				} else {
					yield Optional.empty();
				}
			}
			case NumberLiteralToken(String raw) -> {
				try {
					tokenStream.nextToken();
					yield Optional.of(new JcyoConstant(new JcyoNumber(Double.parseDouble(raw))));
				} catch (NumberFormatException e) {
					throw new JcyoParseException("Unexpected number literal " + raw);
				}
			}
			case StringLiteralToken(String raw) -> {
				tokenStream.nextToken();
				var sb = new StringBuilder();
				int end = raw.length() - 1;
				for (int i = 1; i < end; i++) {
					char c = raw.charAt(i);
					if (c == '\\') {
						i++;
						c = raw.charAt(i);
						sb.append(switch (c) {
							case 'n' -> '\n';
							case 'r' -> '\r';
							case 't' -> '\t';
							case '"' -> '"';
							case '\\' -> '\\';
							default -> throw new JcyoParseException("Unexpected escape sequence \\" + c);
						});
					} else {
						sb.append(c);
					}
				}
				yield Optional.of(new JcyoConstant(new JcyoString(sb.toString())));
			}
			default -> Optional.empty();
		};
	}

	private Optional<JcyoVariableReference> tryParseVariableReference() {
		return switch (tokenStream.peekToken()) {
			case IdentifierToken identifierToken -> {
				tokenStream.nextToken();
				yield Optional.of(new JcyoVariableReference(identifierToken.identifier()));
			}
			case JavaKeywordToken keywordToken -> {
				tokenStream.peekToken();
				yield Optional.of(new JcyoVariableReference(keywordToken.raw()));
			}
			default -> Optional.empty();
		};
	}

	private Optional<JcyoBinaryOperator.Type> tryParseBinaryOperator() {
		if (tokenStream.peekToken() instanceof OperatorToken(int codepoint)) {
			switch (codepoint) {
				case '+' -> {
					tokenStream.nextToken();
					return Optional.of(JcyoBinaryOperator.Type.PLUS);
				}
				case '-' -> {
					tokenStream.nextToken();
					return Optional.of(JcyoBinaryOperator.Type.MINUS);
				}
				case '*' -> {
					tokenStream.nextToken();
					return Optional.of(JcyoBinaryOperator.Type.MULTIPLY);
				}
				case '/' -> {
					tokenStream.nextToken();
					return Optional.of(JcyoBinaryOperator.Type.DIVIDE);
				}
				case '&' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '&') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.AND);
					} else {
						throw new JcyoParseException(
								"And operator requires double ampersand, got: " + tokenStream.peekToken()
						);
					}
				}
				case '|' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '|') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.OR);
					} else {
						throw new JcyoParseException(
								"Or operator requires double pipe, got: " + tokenStream.peekToken()
						);
					}
				}
				case '=' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '=') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.EQUAL);
					} else {
						throw new JcyoParseException(
								"Equals operator requires double equals, got: " + tokenStream.peekToken()
						);
					}
				}
				case '!' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '=') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.NOT_EQUAL);
					} else {
						throw new JcyoParseException(
								"Not equals operator requires double equals, got: " + tokenStream.peekToken()
						);
					}
				}
				case '<' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '=') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.LESS_THAN_OR_EQUAL);
					} else {
						return Optional.of(JcyoBinaryOperator.Type.LESS_THAN);
					}
				}
				case '>' -> {
					tokenStream.nextToken();
					if (tokenStream.peekToken() instanceof OperatorToken(int codepoint2) && codepoint2 == '=') {
						tokenStream.nextToken();
						return Optional.of(JcyoBinaryOperator.Type.GREATER_THAN_OR_EQUAL);
					} else {
						return Optional.of(JcyoBinaryOperator.Type.GREATER_THAN);
					}
				}
			}
		}
		return Optional.empty();
	}

	private void chompWhitespace() {
		while (tokenStream.peekToken() instanceof WhitespaceToken) {
			tokenStream.nextToken();
		}
	}

	private JcyoExpression stitchBinaryOperators(
			List<JcyoExpression> values,
			List<JcyoBinaryOperator.Type> operatorTypes
	) {
		if (values.size() == 1) {
			return values.getFirst();
		}

		Integer[] operatorIndices = new Integer[operatorTypes.size()];
		for (int i = 0; i < operatorTypes.size(); i++) {
			operatorIndices[i] = i;
		}

		Arrays.sort(
				operatorIndices,
				Comparator.comparingInt(index -> getBinaryOperatorPriority(operatorTypes.get(index)))
		);

		@Nullable JcyoExpression[] valuesArray = values.toArray(JcyoExpression[]::new);

		for (Integer operatorIndex : operatorIndices) {
			int leftValueIndex = findNonNullIndexLeft(valuesArray, operatorIndex);
			JcyoExpression leftValue = valuesArray[leftValueIndex];
			int rightValueIndex = operatorIndex + 1;
			JcyoExpression rightValue = valuesArray[rightValueIndex];

			assert leftValue != null;
			assert rightValue != null;

			valuesArray[rightValueIndex] = null;
			valuesArray[leftValueIndex] = new JcyoBinaryOperator(
					operatorTypes.get(operatorIndex),
					leftValue,
					rightValue
			);
		}

		assert valuesArray[0] != null;
		return valuesArray[0];
	}

	private int getBinaryOperatorPriority(JcyoBinaryOperator.Type type) {
		return switch (type) {
			case AND, OR -> 5;
			case LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL -> 4;
			case EQUAL, NOT_EQUAL -> 3;
			case PLUS, MINUS -> 2;
			case MULTIPLY, DIVIDE -> 1;
		};
	}

	private <T> int findNonNullIndexLeft(@Nullable T[] array, int startIndex) {
		for (int i = startIndex; i >= 0; i--) {
			if (array[i] != null) {
				return i;
			}
		}
		throw new IllegalStateException("No non-null element found");
	}
}
