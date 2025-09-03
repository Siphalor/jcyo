package de.siphalor.jcyo.core.impl.directive;

import de.siphalor.jcyo.core.api.value.JcyoBoolean;
import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.expression.JcyoConstant;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectiveParserTest {

	@Test
	void ifLineMode() {
		TokenStream tokenStream = TokenStream.from(List.of(
				new JcyoDirectiveStartToken("", CommentStyle.LINE),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.IF),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new WhitespaceToken(" "),
				new LineBreakToken("\n"),
				new IdentifierToken("blub")
		));
		DirectiveParser parser = new DirectiveParser(PeekableTokenStream.from(tokenStream));

		JcyoDirective directive = parser.nextDirective();

		assertThat(directive).isEqualTo(new IfDirective(new JcyoConstant(new JcyoBoolean(true))));
		assertThat(tokenStream.nextToken()).isEqualTo(new IdentifierToken("blub"));
	}

	@Test
	void ifFlexMode() {
		TokenStream tokenStream = TokenStream.from(List.of(
				new JcyoDirectiveStartToken("", CommentStyle.FLEX),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.IF),
				new WhitespaceToken(" "),
				new JavaKeywordToken(JavaKeyword.TRUE),
				new WhitespaceToken(" "),
				new JcyoEndToken(""),
				new IdentifierToken("blub")
		));
		DirectiveParser parser = new DirectiveParser(PeekableTokenStream.from(tokenStream));

		JcyoDirective directive = parser.nextDirective();

		assertThat(directive).isEqualTo(new IfDirective(new JcyoConstant(new JcyoBoolean(true))));
		assertThat(tokenStream.nextToken()).isEqualTo(new IdentifierToken("blub"));
	}

}
