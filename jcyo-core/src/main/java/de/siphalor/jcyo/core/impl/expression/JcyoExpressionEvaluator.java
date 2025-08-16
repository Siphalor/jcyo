package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.JcyoProcessingException;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JcyoExpressionEvaluator {
	private final JcyoVariables variables;

	public JcyoValue evaluate(JcyoExpression expression) throws JcyoProcessingException {
		return switch (expression) {
			case JcyoConstant(JcyoValue value) -> value;
			case JcyoVariableReference(String name) -> variables.get(name).orElse(new JcyoUndefined(name));
			case JcyoUnaryOperator(JcyoUnaryOperator.Type type, JcyoExpression inner) -> switch (type) {
				case MINUS -> new JcyoNumber(-assertNumber(evaluate(inner), "in unary minus").value());
				case NOT -> new JcyoBoolean(!evaluate(inner).truthy());
			};
			case JcyoBinaryOperator(JcyoBinaryOperator.Type type, JcyoExpression left, JcyoExpression right) -> switch (type) {
				case PLUS -> {
					JcyoValue leftValue = evaluate(left);
					JcyoValue rightValue = evaluate(right);
					if (leftValue instanceof JcyoString || rightValue instanceof JcyoString) {
						yield new JcyoString(valueToString(leftValue) + valueToString(rightValue));
					}
					yield new JcyoNumber(
						assertNumber(leftValue, "on left side of plus").value()
								+ assertNumber(rightValue, "on right side of plus").value()
				);
				}
				case MINUS -> new JcyoNumber(
						assertNumber(evaluate(left), "on left side of minus").value()
								- assertNumber(evaluate(right), "on right side of minus").value()
				);
				case MULTIPLY -> new JcyoNumber(
						assertNumber(evaluate(left), "on left side of multiply").value()
								* assertNumber(evaluate(right), "on right side of multiply").value()
				);
				case DIVIDE -> new JcyoNumber(
						assertNumber(evaluate(left), "on left side of divide").value()
								/ assertNumber(evaluate(right), "on right side of divide").value()
				);
				case AND -> new JcyoBoolean(evaluate(left).truthy() && evaluate(right).truthy());
				case OR -> new JcyoBoolean(evaluate(left).truthy() || evaluate(right).truthy());
				case EQUAL -> new JcyoBoolean(evaluate(left).equals(evaluate(right)));
				case NOT_EQUAL -> new JcyoBoolean(!evaluate(left).equals(evaluate(right)));
				case GREATER_THAN -> new JcyoBoolean(
						assertNumber(evaluate(left), "on left side of greater than").value()
								> assertNumber(evaluate(right), "on right side of greater than").value()
				);
				case GREATER_THAN_OR_EQUAL -> new JcyoBoolean(
						assertNumber(evaluate(left), "on left side of greater than or equal").value()
								> assertNumber(evaluate(right), "on right side of greater than or equal").value()
				);
				case LESS_THAN -> new JcyoBoolean(
						assertNumber(evaluate(left), "on left side of less than").value()
								> assertNumber(evaluate(right), "on right side of less than").value()
				);
				case LESS_THAN_OR_EQUAL -> new JcyoBoolean(
						assertNumber(evaluate(left), "on left side of less than or equal").value()
								> assertNumber(evaluate(right), "on right side of less than or equal").value()
				);
			};
		};
	}

	private String valueToString(JcyoValue value) {
		return switch (value) {
			case JcyoString(String string) -> string;
			case JcyoNumber(double number) -> Double.toString(number);
			case JcyoBoolean(boolean bool) -> Boolean.toString(bool);
			case JcyoUndefined _ -> "";
		};
	}

	private JcyoNumber assertNumber(JcyoValue value, String context) throws JcyoProcessingException {
		if (value instanceof JcyoNumber number) {
			return number;
		}
		throw new JcyoProcessingException("Expected number " + context + ", got: " + value);
	}
}
