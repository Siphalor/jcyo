package de.siphalor.jcyo.core.impl.expression;

public record JcyoBinaryOperator(Type type, JcyoExpression left, JcyoExpression right) implements JcyoExpression {
	enum Type {
		PLUS,
		MINUS,
		MULTIPLY,
		DIVIDE,
		AND,
		OR,
		EQUAL,
		NOT_EQUAL,
		LESS_THAN,
		GREATER_THAN,
		LESS_THAN_OR_EQUAL,
		GREATER_THAN_OR_EQUAL,
	}
}
