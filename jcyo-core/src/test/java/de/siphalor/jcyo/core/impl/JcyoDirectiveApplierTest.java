package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import de.siphalor.jcyo.core.impl.transform.JcyoDirectiveApplier;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JcyoDirectiveApplierTest {
	@Test
	@SneakyThrows
	void testLineBreaks() {
		TokenStream input = TokenStream.from(List.of(
				new IdentifierToken("test"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new IdentifierToken("blub"),
				new OperatorToken('+'),
				new NumberLiteralToken("1"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("else"),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n")
		));

		var applier = new JcyoDirectiveApplier(JcyoOptions.builder().build(), new JcyoVariables());
		TokenStream result = applier.apply(input);

		assertThat(result.stream().toList()).isEqualTo(List.of(
				new IdentifierToken("test"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new JcyoDisabledStartToken("//- ", CommentStyle.LINE),
				new IdentifierToken("blub"),
				new OperatorToken('+'),
				new NumberLiteralToken("1"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new LineBreakToken("\n"),
				new JcyoDisabledStartToken("//- ", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("else"),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n")
		));
	}
}
