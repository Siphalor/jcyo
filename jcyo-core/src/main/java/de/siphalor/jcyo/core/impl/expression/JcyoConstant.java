package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.impl.expression.value.JcyoValue;

public record JcyoConstant(JcyoValue value) implements JcyoExpression {
}
