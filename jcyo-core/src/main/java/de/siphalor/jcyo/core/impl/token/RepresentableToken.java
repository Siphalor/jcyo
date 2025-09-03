package de.siphalor.jcyo.core.impl.token;

public sealed interface RepresentableToken extends Token
		permits CharacterLiteralToken,
		IdentifierToken,
		JavaKeywordToken,
		JcyoDirectiveStartToken,
		JcyoEndToken,
		JcyoDisabledStartToken,
		LineBreakToken,
		NumberLiteralToken,
		OperatorToken,
		PlainJavaCommentToken,
		StringLiteralToken,
		WhitespaceToken {
	String raw();
}
