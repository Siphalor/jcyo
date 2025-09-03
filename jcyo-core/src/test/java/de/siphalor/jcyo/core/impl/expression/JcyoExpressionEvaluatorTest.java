package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JcyoExpressionEvaluatorTest {
	@ParameterizedTest
	@CsvSource({
			"1,-1",
			"-1.234,1.234",
			"0,-0",
	})
	@SneakyThrows
	void unaryMinus(double input, double expected) {
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(new JcyoVariables());
		JcyoValue result = evaluator.evaluate(new JcyoUnaryOperator(
				JcyoUnaryOperator.Type.MINUS,
				new JcyoConstant(new JcyoNumber(input))
		));
		assertThat(result).isEqualTo(new JcyoNumber(expected));
	}

	@ParameterizedTest
	@MethodSource("nonNumberValues")
	void unaryMinusThrows(JcyoValue value) {
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(new JcyoVariables());
		JcyoUnaryOperator operator = new JcyoUnaryOperator(
				JcyoUnaryOperator.Type.MINUS,
				new JcyoConstant(value)
		);
		assertThatThrownBy(() -> evaluator.evaluate(operator)).isInstanceOf(JcyoExpressionEvaluationException.class);
	}

	@ParameterizedTest
	@MethodSource("unaryNotParams")
	@SneakyThrows
	void unaryNot(JcyoValue value, boolean expected) {
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(new JcyoVariables());
		JcyoValue result = evaluator.evaluate(new JcyoUnaryOperator(
				JcyoUnaryOperator.Type.NOT,
				new JcyoConstant(value)
		));
		assertThat(result).isEqualTo(new JcyoBoolean(expected));
	}

	static Stream<Arguments> unaryNotParams() {
		return Stream.of(
				arguments(new JcyoUndefined("test"), true),
				arguments(new JcyoString(""), false),
				arguments(new JcyoString("hi"), false),
				arguments(new JcyoBoolean(false), true),
				arguments(new JcyoBoolean(true), false),
				arguments(new JcyoNumber(0), true),
				arguments(new JcyoNumber(123), false)
		);
	}

	@ParameterizedTest
	@CsvSource({
			"PLUS, 1, 2, 3",
			"PLUS, 10, -20, -10",
			"MINUS, 1, 2, -1",
			"MINUS, 10, -20, 30",
			"MULTIPLY, 1, 2, 2",
			"MULTIPLY, 10, -20, -200",
			"DIVIDE, 1, 2, 0.5",
			"DIVIDE, 10, -20, -0.5",
	})
	@SneakyThrows
	void binaryNumberOperator(JcyoBinaryOperator.Type type, double left, double right, double expected) {
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(new JcyoVariables());
		JcyoValue result = evaluator.evaluate(new JcyoBinaryOperator(
				type,
				new JcyoConstant(new JcyoNumber(left)),
				new JcyoConstant(new JcyoNumber(right))
		));
		assertThat(result).isEqualTo(new JcyoNumber(expected));
	}

	@ParameterizedTest
	@MethodSource("binaryNumberOperatorThrowsParams")
	void binaryNumberOperatorThrows(JcyoBinaryOperator.Type type, JcyoValue left, JcyoValue right) {
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(new JcyoVariables());
		JcyoBinaryOperator operator = new JcyoBinaryOperator(
				type,
				new JcyoConstant(left),
				new JcyoConstant(right)
		);
		assertThatThrownBy(() -> evaluator.evaluate(operator)).isInstanceOf(JcyoExpressionEvaluationException.class);
	}

	static Stream<Arguments> binaryNumberOperatorThrowsParams() {
		return Stream.of(
				JcyoBinaryOperator.Type.MINUS,
				JcyoBinaryOperator.Type.MULTIPLY,
				JcyoBinaryOperator.Type.DIVIDE,
				JcyoBinaryOperator.Type.GREATER_THAN,
				JcyoBinaryOperator.Type.GREATER_THAN_OR_EQUAL,
				JcyoBinaryOperator.Type.LESS_THAN,
				JcyoBinaryOperator.Type.LESS_THAN_OR_EQUAL
		).flatMap(type -> Stream.concat(
				nonNumberValues().map(left -> arguments(type, left, new JcyoNumber(12.34))),
				nonNumberValues().map(right -> arguments(type, new JcyoNumber(12.34), right))
		));
	}

	static Stream<JcyoValue> nonNumberValues() {
		return Stream.of(
				new JcyoUndefined("test"),
				new JcyoString("hi"),
				new JcyoBoolean(true)
		);
	}

	@Test
	@SneakyThrows
	void additionWithVariableReferences() {
		JcyoVariables variables = new JcyoVariables();
		variables.set("a", new JcyoNumber(1));
		variables.set("b", new JcyoNumber(2));
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(variables);

		JcyoValue result = evaluator.evaluate(new JcyoBinaryOperator(
				JcyoBinaryOperator.Type.PLUS,
				new JcyoVariableReference("a"),
				new JcyoVariableReference("b")
		));
		assertThat(result).isEqualTo(new JcyoNumber(3));
	}

	@Test
	@SneakyThrows
	void variableStringEquality() {
		JcyoVariables variables = new JcyoVariables();
		variables.set("test", new JcyoString("hello"));
		JcyoExpressionEvaluator evaluator = new JcyoExpressionEvaluator(variables);

		JcyoValue result = evaluator.evaluate(new JcyoBinaryOperator(
				JcyoBinaryOperator.Type.EQUAL,
				new JcyoVariableReference("TEST"),
				new JcyoConstant(new JcyoString("hello"))
		));
		assertThat(result).isEqualTo(new JcyoBoolean(true));
	}
}
