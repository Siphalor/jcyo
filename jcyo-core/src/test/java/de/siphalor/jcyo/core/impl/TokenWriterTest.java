package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenWriterTest {
	@Test
	@SneakyThrows
	void testNormal() {
		var stringWriter = new StringWriter();

		var writer = new TokenWriter(stringWriter, JcyoOptions.builder().build());

		writer.writeAll(TokenStream.from(List.of(
				new WhitespaceToken('\t'),
				new JavaKeywordToken(JavaKeyword.RETURN),
				new WhitespaceToken(' '),
				new NumberLiteralToken("123"),
				new WhitespaceToken(' '),
				new OperatorToken('+'),
				new WhitespaceToken(' '),
				new CharacterLiteralToken("\"abc\""),
				new WhitespaceToken(' '),
				new OperatorToken('!'),
				new OperatorToken('='),
				new WhitespaceToken(' '),
				new IdentifierToken("variable"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				EofToken.instance()
		)));

		assertThat(stringWriter.toString()).isEqualTo("\treturn 123 + \"abc\" != variable;\n");
	}

	@Test
	@SneakyThrows
	void testDisabledLineMode() {
		var stringWriter = new StringWriter();

		var writer = new TokenWriter(stringWriter, JcyoOptions.builder().build());

		writer.writeAll(TokenStream.from(List.of(
				new IdentifierToken("test"),
				new OperatorToken('('),
				new OperatorToken(')'),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionStartToken(CommentStyle.LINE, ""),
				new JavaKeywordToken(JavaKeyword.WHILE),
				new OperatorToken('('),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new OperatorToken(')'),
				new OperatorToken('{'),
				new LineBreakToken("\n"),
				new WhitespaceToken('\t'),
				new IdentifierToken("test"),
				new OperatorToken('('),
				new OperatorToken(')'),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new OperatorToken('}'),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionEndToken(),
				new IdentifierToken("test"),
				new OperatorToken('('),
				new OperatorToken(')'),
				new OperatorToken(';'),
				EofToken.instance()
		)));

		assertThat(stringWriter.toString()).isEqualTo("""
				test();
				//- while(true){
				//- \ttest();
				//- }
				test();""");
	}

	@Test
	@SneakyThrows
	void testDisabledLineModeSplitIndent() {
		var stringWriter = new StringWriter();

		var writer = new TokenWriter(stringWriter, JcyoOptions.builder().build());

		writer.writeAll(TokenStream.from(List.of(
				new JcyoDisabledRegionStartToken(CommentStyle.LINE, "\t "),
				new WhitespaceToken('\t'),
				new WhitespaceToken(' '),
				new WhitespaceToken(' '),
				new WhitespaceToken(' '),
				new IdentifierToken("test"),
				new LineBreakToken("\n"),
				new WhitespaceToken('\t'),
				new OperatorToken('('),
				new LineBreakToken("\n"),
				new WhitespaceToken('\t'),
				new WhitespaceToken(' '),
				new OperatorToken(')'),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionEndToken(),
				EofToken.instance()
		)));

		assertThat(stringWriter.toString()).isEqualTo("""
				\t //-   test
				\t//- (
				\t //- );
				""");
	}
}
