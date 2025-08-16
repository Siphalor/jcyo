package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStreamTest {
	@Test
	void from() {
		IdentifierToken identifierToken = new IdentifierToken("test");
		JavaKeywordToken keywordToken = new JavaKeywordToken(JavaKeyword.IF);
		List<Token> tokens = List.of(identifierToken, keywordToken);

		TokenStream stream = TokenStream.from(tokens);

		assertThat(stream.nextToken()).isSameAs(identifierToken);
		assertThat(stream.nextToken()).isSameAs(keywordToken);
		assertThat(stream.nextToken()).isSameAs(EofToken.instance());
		assertThat(stream.nextToken()).isSameAs(EofToken.instance());
	}
}
