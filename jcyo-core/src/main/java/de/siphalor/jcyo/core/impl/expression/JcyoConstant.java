package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.api.value.JcyoValue;

public record JcyoConstant(JcyoValue value) implements JcyoExpression {
}
