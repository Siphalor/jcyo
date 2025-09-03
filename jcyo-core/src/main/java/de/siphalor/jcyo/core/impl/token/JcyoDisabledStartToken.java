package de.siphalor.jcyo.core.impl.token;

import de.siphalor.jcyo.core.impl.CommentStyle;

public record JcyoDisabledStartToken(String raw, CommentStyle commentStyle) implements RepresentableToken {
}
