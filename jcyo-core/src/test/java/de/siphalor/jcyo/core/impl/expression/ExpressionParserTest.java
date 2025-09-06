package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.value.JcyoNumber;
import de.siphalor.jcyo.core.api.value.JcyoString;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.NumberLiteralToken;
import de.siphalor.jcyo.core.impl.token.OperatorToken;
import de.siphalor.jcyo.core.impl.token.StringLiteralToken;
import de.siphalor.jcyo.core.impl.token.WhitespaceToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionParserTest {

	@ParameterizedTest
	@ValueSource(strings = {"", "hi"})
	void stringLiteral(String string) {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new WhitespaceToken(' '),
				new StringLiteralToken("\"" + string + "\""),
				new WhitespaceToken(' ')
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoConstant(new JcyoString(string)));
	}

	@ParameterizedTest
	@CsvSource({
			"-,MINUS",
			"!,NOT"
	})
	void simplePrefix(char operator, JcyoUnaryOperator.Type type) {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new WhitespaceToken(' '),
				new OperatorToken(operator),
				new WhitespaceToken(' '),
				new NumberLiteralToken("123"),
				new WhitespaceToken(' ')
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoUnaryOperator(type, new JcyoConstant(new JcyoNumber(123))));
	}

	@ParameterizedTest
	@CsvSource({
			"+,PLUS",
			"-,MINUS",
			"*,MULTIPLY",
			"/,DIVIDE",
			"<,LESS_THAN",
			">,GREATER_THAN",
	})
	void simpleBinary(char operator, JcyoBinaryOperator.Type type) {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new WhitespaceToken(' '),
				new NumberLiteralToken("123"),
				new WhitespaceToken(' '),
				new OperatorToken(operator),
				new WhitespaceToken(' '),
				new NumberLiteralToken("456"),
				new WhitespaceToken(' ')
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoBinaryOperator(
				type,
				new JcyoConstant(new JcyoNumber(123)),
				new JcyoConstant(new JcyoNumber(456))
		));
	}

	@ParameterizedTest
	@CsvSource({
			"&,&,AND",
			"|,|,OR",
			"=,=,EQUAL",
			"!,=,NOT_EQUAL",
			"<,=,LESS_THAN_OR_EQUAL",
			">,=,GREATER_THAN_OR_EQUAL",
	})
	void doubleBinary(char operator1, char operator2, JcyoBinaryOperator.Type type) {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new WhitespaceToken(' '),
				new NumberLiteralToken("123"),
				new WhitespaceToken(' '),
				new OperatorToken(operator1),
				new OperatorToken(operator2),
				new WhitespaceToken(' '),
				new NumberLiteralToken("456"),
				new WhitespaceToken(' ')
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoBinaryOperator(
				type,
				new JcyoConstant(new JcyoNumber(123)),
				new JcyoConstant(new JcyoNumber(456))
		));
	}

	@Test
	void precedence() {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new NumberLiteralToken("123"),
				new OperatorToken('+'),
				new NumberLiteralToken("456"),
				new OperatorToken('*'),
				new NumberLiteralToken("789"),
				new OperatorToken('='),
				new OperatorToken('='),
				new NumberLiteralToken("12"),
				new OperatorToken('-'),
				new NumberLiteralToken("345")
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoBinaryOperator(
				JcyoBinaryOperator.Type.EQUAL,
				new JcyoBinaryOperator(
						JcyoBinaryOperator.Type.PLUS,
						new JcyoConstant(new JcyoNumber(123)),
						new JcyoBinaryOperator(
								JcyoBinaryOperator.Type.MULTIPLY,
								new JcyoConstant(new JcyoNumber(456)),
								new JcyoConstant(new JcyoNumber(789))
						)
				),
				new JcyoBinaryOperator(
						JcyoBinaryOperator.Type.MINUS,
						new JcyoConstant(new JcyoNumber(12)),
						new JcyoConstant(new JcyoNumber(345))
				)
		));
	}

	@Test
	void parenthesis() {
		ExpressionParser parser = new ExpressionParser(PeekableTokenStream.from(TokenStream.from(List.of(
				new NumberLiteralToken("123"),
				new OperatorToken('*'),
				new OperatorToken('('),
				new WhitespaceToken(' '),
				new NumberLiteralToken("456"),
				new OperatorToken('+'),
				new NumberLiteralToken("789"),
				new WhitespaceToken(' '),
				new OperatorToken(')'),
				new OperatorToken('*'),
				new NumberLiteralToken("12")
		))));

		JcyoExpression expression = parser.nextExpression();

		assertThat(expression).isEqualTo(new JcyoBinaryOperator(
				JcyoBinaryOperator.Type.MULTIPLY,
				new JcyoBinaryOperator(
						JcyoBinaryOperator.Type.MULTIPLY,
						new JcyoConstant(new JcyoNumber(123)),
						new JcyoBinaryOperator(
								JcyoBinaryOperator.Type.PLUS,
								new JcyoConstant(new JcyoNumber(456)),
								new JcyoConstant(new JcyoNumber(789))
						)
				),
				new JcyoConstant(new JcyoNumber(12))
		));
	}
}
