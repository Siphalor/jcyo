package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.JcyoHelper;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;

import java.util.HashSet;
import java.util.Set;

public class UnusedImportDisabler {
	private final JcyoHelper helper;

	public UnusedImportDisabler(JcyoOptions options) {
		this.helper = new JcyoHelper(options);
	}

	public TokenStream apply(TokenStream tokenStream) {
		TokenBuffer copy = new TokenBuffer();
		Set<String> usedIdentifiers = collectUsedIdentifiers(copy.copying(tokenStream));
		return disableUnusedImports(copy, usedIdentifiers);
	}

	private Set<String> collectUsedIdentifiers(TokenStream tokenStream) {
		tokenStream = new JcyoCommentRemover(tokenStream);
		Set<String> identifiers = new HashSet<>();
		boolean afterDot = false;
		while (true) {
			switch (tokenStream.nextToken()) {
				case EofToken _ -> {
					return identifiers;
				}
				case JavaKeywordToken keywordToken -> {
					if (keywordToken.keyword() == JavaKeyword.PACKAGE || keywordToken.keyword() == JavaKeyword.IMPORT) {
						chompToSemicolon(tokenStream);
					}
					afterDot = false;
				}
				case OperatorToken operatorToken -> afterDot = operatorToken.codepoint() == '.';
				case IdentifierToken identifierToken -> {
					if (!afterDot) {
						identifiers.add(identifierToken.identifier());
					}
				}
				default -> afterDot = false;
			}
		}
	}

	private void chompToSemicolon(TokenStream tokenStream) {
		while (true) {
			Token token = tokenStream.nextToken();
			if (token instanceof EofToken) {
				return;
			} else if (token instanceof OperatorToken(int codepoint) && codepoint == ';') {
				return;
			}
		}
	}

	private TokenStream disableUnusedImports(TokenStream source, Set<String> usedIdentifiers) {
		return new TokenStream() {
			private final TokenBuffer buffer = new TokenBuffer();

			@Override
			public Token nextToken() {
				if (!buffer.isEmpty()) {
					return buffer.nextToken();
				}

				Token token = source.nextToken();
				if (token instanceof JavaKeywordToken(JavaKeyword keyword) && keyword == JavaKeyword.IMPORT) {
					buffer.pushToken(token);
					boolean used = buffer.copying(source).stream()
							.takeWhile(t ->
									!(t instanceof OperatorToken(int codepoint)) || codepoint == '.' || codepoint == '*'
							)
							.filter(t ->
									t instanceof IdentifierToken
											|| t instanceof JavaKeywordToken
											|| t instanceof OperatorToken(int codepoint) && codepoint == '*'
							)
							.reduce((_, second) -> second)
							.map(t -> t.raw().equals("*") || usedIdentifiers.contains(t.raw()))
							.orElse(false);
					if (used) {
						return buffer.nextToken();
					} else {
						return helper.disabledForLine();
					}
				}
				return token;
			}
		};
	}
}
