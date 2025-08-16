package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import de.siphalor.jcyo.core.impl.transform.GeneratedAndDisabledTokenRemover;
import org.junit.jupiter.api.Test;

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
				new JcyoDirectiveEndToken(""),
				endToken
		));

		GeneratedAndDisabledTokenRemover remover = new GeneratedAndDisabledTokenRemover(tokens);

		assertThat(remover.nextToken()).isSameAs(startToken);
		assertThat(remover.nextToken()).isSameAs(endToken);
	}
}
