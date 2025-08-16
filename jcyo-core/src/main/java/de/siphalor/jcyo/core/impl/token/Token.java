package de.siphalor.jcyo.core.impl.token;

public sealed interface Token
		permits CharacterLiteralToken,
		EofToken,
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
