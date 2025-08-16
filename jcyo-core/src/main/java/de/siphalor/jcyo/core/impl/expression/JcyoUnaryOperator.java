package de.siphalor.jcyo.core.impl.expression;

public record JcyoUnaryOperator(Type type, JcyoExpression expression) implements JcyoExpression {
	enum Type {
		MINUS,
		NOT
	}
}
