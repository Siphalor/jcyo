package de.siphalor.jcyo.core.impl.expression;

public sealed interface JcyoExpression
		permits JcyoBinaryOperator, JcyoConstant, JcyoUnaryOperator, JcyoVariableReference {
}
