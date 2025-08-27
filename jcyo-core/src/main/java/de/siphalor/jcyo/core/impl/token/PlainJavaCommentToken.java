package de.siphalor.jcyo.core.impl.token;

import de.siphalor.jcyo.core.impl.CommentStyle;

public record PlainJavaCommentToken(String raw, CommentStyle commentStyle, boolean javadoc) implements Token {
}
