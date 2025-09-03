package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoVariables;
import de.siphalor.jcyo.core.api.value.JcyoBoolean;
import de.siphalor.jcyo.core.api.value.JcyoNumber;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import de.siphalor.jcyo.core.impl.transform.JcyoCleaner;
import de.siphalor.jcyo.core.impl.transform.JcyoDirectiveApplier;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JcyoDirectiveApplierTest {
	@Test
	@SneakyThrows
	void testLineMode() {
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

		var applier = new JcyoDirectiveApplier(new JcyoVariables());
		TokenStream result = applier.apply(input);

		assertThat(result.stream().toList()).isEqualTo(List.of(
				new IdentifierToken("test"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionStartToken(CommentStyle.LINE, "test"),
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
				new JcyoDisabledRegionEndToken(),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("else"),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n")
		));
	}

	@Test
	@SneakyThrows
	void testFlexMode() {
		TokenStream input = TokenStream.from(List.of(
				new IdentifierToken("start"),
				new WhitespaceToken(" "),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.IF),
				new IdentifierToken("blub"),
				new JcyoEndToken("*/"),
				new IdentifierToken("hey"),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.ELSE),
				new JcyoEndToken("*/"),
				new IdentifierToken("ho"),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new JcyoEndToken("*/"),
				new IdentifierToken("end")
		));

		JcyoVariables variables = new JcyoVariables();
		variables.set("blub", new JcyoBoolean(true));
		var applier = new JcyoDirectiveApplier(variables);
		TokenStream result = applier.apply(input);

		assertThat(result.stream().toList()).isEqualTo(List.of(
				new IdentifierToken("start"),
				new WhitespaceToken(" "),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.IF),
				new IdentifierToken("blub"),
				new JcyoEndToken("*/"),
				new IdentifierToken("hey"),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.ELSE),
				new JcyoEndToken("*/"),
				new JcyoDisabledRegionStartToken(CommentStyle.FLEX, ""),
				new IdentifierToken("ho"),
				new JcyoDisabledRegionEndToken(),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new JcyoEndToken("*/"),
				new IdentifierToken("end")
		));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			1,a
			2,b
			3,c
			4,d
			""")
	@SneakyThrows
	void testElifs(int variable, String expected) {
		List<Token> inputTokens = List.of(
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new IdentifierToken("t"),
				new OperatorToken('='),
				new OperatorToken('='),
				new NumberLiteralToken("1"),
				new LineBreakToken("\n"),
				new IdentifierToken("a"),
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new IdentifierToken("elif"),
				new IdentifierToken("t"),
				new OperatorToken('='),
				new OperatorToken('='),
				new NumberLiteralToken("2"),
				new LineBreakToken("\n"),
				new IdentifierToken("b"),
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new IdentifierToken("elif"),
				new IdentifierToken("t"),
				new OperatorToken('='),
				new OperatorToken('='),
				new NumberLiteralToken("3"),
				new LineBreakToken("\n"),
				new IdentifierToken("c"),
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.ELSE),
				new LineBreakToken("\n"),
				new IdentifierToken("d"),
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n"),
				EofToken.instance()
		);

		var vars = new JcyoVariables();
		vars.set("t", new JcyoNumber(variable));
		JcyoDirectiveApplier applier = new JcyoDirectiveApplier(vars);
		TokenStream outputStream = new JcyoCleaner(applier.apply(TokenStream.from(inputTokens)));

		assertThat(outputStream.stream().toList()).isEqualTo(List.of(new IdentifierToken(expected)));
	}

	@Test
	@SneakyThrows
	void testFlexCommentDisabling() {
		List<Token> inputTokens = List.of(
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoEndToken("*/"),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("/* test \n next */", CommentStyle.FLEX, false),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new JcyoEndToken("*/"),
				EofToken.instance()
		);

		JcyoDirectiveApplier applier = new JcyoDirectiveApplier(new JcyoVariables());
		TokenStream outputStream = applier.apply(TokenStream.from(inputTokens));

		assertThat(outputStream.stream().toList()).isEqualTo(List.of(
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new JcyoEndToken("*/"),
				new JcyoDisabledRegionStartToken(CommentStyle.FLEX, ""),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("/* test \n next */", CommentStyle.FLEX, false),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new JcyoDisabledRegionEndToken(),
				new JcyoDirectiveStartToken("/*#", CommentStyle.FLEX),
				new IdentifierToken("end"),
				new JcyoEndToken("*/")
		));
	}

	@Test
	@SneakyThrows
	void testLineCommentDisabling() {
		List<Token> inputTokens = List.of(
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("// test", CommentStyle.LINE, false),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("/* test \n next */", CommentStyle.FLEX, false),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n")
		);

		JcyoDirectiveApplier applier = new JcyoDirectiveApplier(new JcyoVariables());
		TokenStream outputStream = applier.apply(TokenStream.from(inputTokens));

		assertThat(outputStream.stream().toList()).isEqualTo(List.of(
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new JavaKeywordToken(JavaKeyword.IF),
				new JavaKeywordToken(JavaKeyword.FALSE),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionStartToken(CommentStyle.LINE, ""),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("// test", CommentStyle.LINE, false),
				new LineBreakToken("\n"),
				new WhitespaceToken(" "),
				new PlainJavaCommentToken("/* test \n next */", CommentStyle.FLEX, false),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionEndToken(),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("end"),
				new LineBreakToken("\n")
		));
	}
}
