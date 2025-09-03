package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusedImportDisablerTest {

	@Test
	void apply() {
		TokenStream tokenStream = TokenStream.from(List.of(
				new JavaKeywordToken(JavaKeyword.PACKAGE),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("blub"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("Utils"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.STATIC),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("whoosh"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new OperatorToken('*'),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("whoosh"),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.CLASS),
				new IdentifierToken("Test"),
				new JavaKeywordToken(JavaKeyword.IMPLEMENTS),
				new IdentifierToken("SomeInterface"),
				new OperatorToken('<'),
				new IdentifierToken("Utils"),
				new OperatorToken('>')
		));

		UnusedImportDisabler unusedImportDisabler = new UnusedImportDisabler();
		TokenStream result = unusedImportDisabler.apply(tokenStream);

		assertThat(result.stream().toList()).isEqualTo(List.of(
				new JavaKeywordToken(JavaKeyword.PACKAGE),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("blub"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("Utils"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionStartToken(CommentStyle.LINE, ""),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.STATIC),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new IdentifierToken("whoosh"),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDisabledRegionEndToken(),
				new JavaKeywordToken(JavaKeyword.IMPORT),
				new WhitespaceToken(" "),
				new IdentifierToken("test"),
				new OperatorToken('.'),
				new OperatorToken('*'),
				new OperatorToken(';'),
				new LineBreakToken("\n"),
				new JcyoDirectiveStartToken("//#", CommentStyle.LINE),
				new IdentifierToken("whoosh"),
				new LineBreakToken("\n"),
				new JavaKeywordToken(JavaKeyword.CLASS),
				new IdentifierToken("Test"),
				new JavaKeywordToken(JavaKeyword.IMPLEMENTS),
				new IdentifierToken("SomeInterface"),
				new OperatorToken('<'),
				new IdentifierToken("Utils"),
				new OperatorToken('>')
		));
	}
}
