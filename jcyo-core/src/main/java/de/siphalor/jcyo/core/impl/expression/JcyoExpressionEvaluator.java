package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.expression.value.JcyoValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JcyoExpressionEvaluator {
	private final JcyoVariables variables;

	public JcyoValue evaluate(JcyoExpression expression) {
		return switch (expression) {
			case JcyoConstant(JcyoValue value) -> value;
		};
	}
}
