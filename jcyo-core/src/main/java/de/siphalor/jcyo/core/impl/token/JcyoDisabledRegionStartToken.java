package de.siphalor.jcyo.core.impl.token;

import de.siphalor.jcyo.core.impl.CommentStyle;

public record JcyoDisabledRegionStartToken(CommentStyle suggestedCommentStyle, String suggestedIndent)
		implements Token {
}
