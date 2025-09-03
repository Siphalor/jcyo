package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import de.siphalor.jcyo.core.impl.transform.GeneratedAndDisabledTokenRemover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedAndDisabledTokenRemoverTest {
	@Test
	void removeGeneratedCode() {
		IdentifierToken startToken = new IdentifierToken("start");
		IdentifierToken endToken = new IdentifierToken("end");
		TokenStream tokens = TokenStream.from(List.of(
				startToken,
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new IdentifierToken("generated"),
				new WhitespaceToken("   "),
				new LineBreakToken("\n"),
				new IdentifierToken("blub"),
				new JcyoDirectiveStartToken("", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new WhitespaceToken(" "),
				new OperatorToken(':'),
				new LineBreakToken("\r"),
				new IdentifierToken("generated"),
				new WhitespaceToken("  "),
				new JcyoEndToken(""),
				endToken
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(
				tokens,
				JcyoOptions.builder().build()
		);

		assertThat(remover.nextToken()).isSameAs(startToken);
		assertThat(remover.nextToken()).isSameAs(endToken);
	}

	@Test
	void removeFlexDisabledToken() {
		TokenStream tokens = TokenStream.from(List.of(
				new JcyoDisabledStartToken("", CommentStyle.FLEX),
				new IdentifierToken("token"),
				new JcyoEndToken("")
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(
				tokens,
				JcyoOptions.builder().build()
		);

		assertThat(remover.stream().toList()).isEqualTo(List.of(new IdentifierToken("token")));
	}

	@Test
	void removeFlexDisabledTokenWithWhitespace() {
		TokenStream tokens = TokenStream.from(List.of(
				new JcyoDisabledStartToken("", CommentStyle.FLEX),
				new WhitespaceToken(" "),
				new WhitespaceToken(" "),
				new IdentifierToken("token"),
				new WhitespaceToken(" "),
				new WhitespaceToken(" "),
				new JcyoEndToken("")
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(
				tokens,
				JcyoOptions.builder().build()
		);

		assertThat(remover.stream().toList()).isEqualTo(List.of(new IdentifierToken("token")));
	}

	@Test
	void removeFlexDisabledTokensWithImplicitEnd() {
		TokenStream tokens = TokenStream.from(List.of(
				new JcyoDisabledStartToken("/*-", CommentStyle.FLEX),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("if"),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new JcyoEndToken("*/"),
				JcyoEndToken.implicit()
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(
				tokens,
				JcyoOptions.builder().build()
		);

		assertThat(remover.stream().toList()).isEqualTo(List.of(
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("if"),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new JcyoEndToken("*/")
		));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			/* test */,/* test */
			/* //- test */,/* test */
			'/**
			//-  * test
			//-  */','/**
			 * test
			 */'
			""")
	void removeLineDisabledTokensFromFlexComments(String comment, String plainComment) {
		TokenStream tokens = TokenStream.from(List.of(
				new PlainJavaCommentToken(comment, CommentStyle.FLEX, false)
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(
				tokens,
				JcyoOptions.builder().build()
		);

		assertThat(remover.stream().toList())
				.isEqualTo(List.of(new PlainJavaCommentToken(plainComment, CommentStyle.FLEX, false)));
	}
}
