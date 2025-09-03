package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public class JcyoExpressionEvaluator {
	private final JcyoVariables variables;

	public JcyoValue evaluate(JcyoExpression expression) throws JcyoExpressionEvaluationException {
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
				case EQUAL -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) == 0);
				case NOT_EQUAL -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) != 0);
				case GREATER_THAN -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) > 0);
				case GREATER_THAN_OR_EQUAL -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) >= 0);
				case LESS_THAN -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) < 0);
				case LESS_THAN_OR_EQUAL -> new JcyoBoolean(compare(evaluate(left), evaluate(right)) <= 0);
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

	private int compare(JcyoValue left, JcyoValue right) throws JcyoExpressionEvaluationException {
		if (left instanceof JcyoString(String leftString) && right instanceof JcyoString(String rightString)) {
			return leftString.compareTo(rightString);
		}
		Integer cmp = tryCompareAsNumbers(left, right);
		if (cmp != null) {
			return cmp;
		}
		throw new JcyoExpressionEvaluationException(
				"Cannot compare " + left + " and " + right + ": Both must be numbers or strings"
		);
	}

	private @Nullable Integer tryCompareAsNumbers(JcyoValue left, JcyoValue right) throws JcyoExpressionEvaluationException {
		Double leftNumber = null;
		Double rightNumber = null;
		if (left instanceof JcyoNumber (double value)) {
			leftNumber = value;
		}
		if (right instanceof JcyoNumber (double value)) {
			rightNumber = value;
		}
		if (leftNumber != null) {
			if (rightNumber != null) {
				return Double.compare(leftNumber, rightNumber);
			}
			rightNumber = tryParseAsDouble(valueToString(right));
			if (rightNumber != null) {
				return Double.compare(leftNumber, rightNumber);
			}
		} else if (rightNumber != null) {
			leftNumber = tryParseAsDouble(valueToString(left));
			if (leftNumber != null) {
				return Double.compare(leftNumber, rightNumber);
			}
		}
		return null;
	}

	private @Nullable Double tryParseAsDouble(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private JcyoNumber assertNumber(JcyoValue value, String context) throws JcyoExpressionEvaluationException {
		if (value instanceof JcyoNumber number) {
			return number;
		}
		throw new JcyoExpressionEvaluationException("Expected number " + context + ", got: " + value);
	}
}
