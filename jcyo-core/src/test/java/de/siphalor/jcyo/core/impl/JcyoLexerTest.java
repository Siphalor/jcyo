package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.token.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JcyoLexerTest {
	@Test
	void empty() {
		JcyoLexer lexer = new JcyoLexer(new StringReader(""), JcyoOptions.builder().build());
		assertThat(lexer.nextToken()).isEqualTo(EofToken.instance());
	}

	@Test
	void test() {
		JcyoLexer lexer = new JcyoLexer(
				new StringReader("""
						package test.abc;
						//- import static hi;
						
						//# if false
						1E100+"test"
						//# end:if
						"""), JcyoOptions.builder().build()
		);

		assertThat(lexer.stream().toList()).isEqualTo(List.of(
				new JavaKeywordToken(JavaKeyword.PACKAGE),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("abc"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDisabledStartToken("//-", CommentStyle.LINE),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.STATIC),
				new WhitespaceToken(" "),
				new IdentifierToken("hi"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.IF),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new NumberLiteralToken("1E100"),
				new OperatorToken('+'),
				new StringLiteralToken("\"test\""),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new WhitespaceToken(" "),
				new IdentifierToken("end"),
				new OperatorToken(':'),
				new JavaKeywordToken(JavaKeyword.IF),
				new LineBreakToken("\n")
		));
	}

	@Test
	void testDisabledFlexCommentsImplicitEnds() {
		JcyoLexer lexer = new JcyoLexer(
				new StringReader("/*-test/* blub *//*- /*#if false*//*#end*/"),
				JcyoOptions.builder().build()
		);

		assertThat(lexer.stream().toList()).isEqualTo(List.of(
				new JcyoDisabledStartToken("/*-", CommentStyle.FLEX),
				new IdentifierToken("test"),
				new PlainJavaCommentToken("/* blub */", CommentStyle.FLEX, false),
				new JcyoEndToken(""), // implicit
				new JcyoDisabledStartToken("/*-", CommentStyle.FLEX),
				new WhitespaceToken(" "),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.IF),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoEndToken("*/"), // directive end
				new JcyoEndToken(""), // implicit
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new JcyoEndToken("*/") // directive end
		));
	}
}
