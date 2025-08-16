package de.siphalor.jcyo.core.impl.stream;

import de.siphalor.jcyo.core.impl.token.EofToken;
import de.siphalor.jcyo.core.impl.token.Token;

import java.util.ArrayDeque;
import java.util.SequencedCollection;
import java.util.stream.Stream;

public interface TokenStream {
	static TokenStream from(SequencedCollection<Token> tokens) {
		return new StaticTokenStream(new ArrayDeque<>(tokens));
	}

	Token nextToken();

	default Stream<Token> stream() {
		return Stream.generate(this::nextToken).takeWhile(token -> !(token instanceof EofToken));
	}
}
