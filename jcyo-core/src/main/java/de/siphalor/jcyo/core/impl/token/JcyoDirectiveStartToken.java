package de.siphalor.jcyo.core.impl.token;

import de.siphalor.jcyo.core.impl.CommentStyle;

public record JcyoDirectiveStartToken(String raw, CommentStyle commentStyle) implements Token {
}
