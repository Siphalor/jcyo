package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.impl.CommentStyle;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
public class UnusedImportDisabler {
	public TokenStream apply(TokenStream tokenStream) {
		TokenBuffer copy = new TokenBuffer();
		Set<String> usedIdentifiers = collectUsedIdentifiers(copy.copying(tokenStream));
		return disableUnusedImports(copy, usedIdentifiers);
	}

	private Set<String> collectUsedIdentifiers(TokenStream tokenStream) {
		tokenStream = new JcyoCleaner(tokenStream);
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
		var peekableSource = PeekableTokenStream.from(source);
		return new TokenStream() {
			private final TokenBuffer buffer = new TokenBuffer();
			private boolean inDisabledRegion;

			@Override
			public Token nextToken() {
				if (!buffer.isEmpty()) {
					return buffer.nextToken();
				}

				Token token = peekableSource.nextToken();
				switch (token) {
					case JcyoDisabledRegionStartToken startToken when inDisabledRegion ->
							throw new IllegalStateException(
									"Encountered disabled region start token " + startToken +
											"while already in disabled region"
							);
					case JcyoDisabledRegionStartToken _ -> inDisabledRegion = true;
					case JcyoDisabledRegionEndToken endToken when !inDisabledRegion -> throw new IllegalStateException(
							"Encountered disabled region end token " + endToken +
									"while not in disabled region"
					);
					case JcyoDisabledRegionEndToken _ -> inDisabledRegion = false;
					case JavaKeywordToken(JavaKeyword keyword)
							when !inDisabledRegion && keyword == JavaKeyword.IMPORT -> {
						buffer.pushToken(token);
						boolean used = parseImportAndDetermineIfUsed();
						if (used) {
							return buffer.nextToken();
						} else if (peekableSource.peekToken() instanceof LineBreakToken) {
							buffer.pushToken(peekableSource.nextToken());
							buffer.pushToken(new JcyoDisabledRegionEndToken());
							return new JcyoDisabledRegionStartToken(CommentStyle.LINE, "");
						} else {
							buffer.pushToken(new JcyoDisabledRegionEndToken());
							return new JcyoDisabledRegionStartToken(CommentStyle.FLEX, "");
						}
					}
					default -> {
					}
				}
				return token;
			}

			private boolean parseImportAndDetermineIfUsed() {
				return buffer.copying(peekableSource).stream()
						.takeWhile(t ->
								!(t instanceof OperatorToken(int codepoint))
										|| codepoint == '.'
										|| codepoint == '*'
						)
						.map(t -> switch (t) {
							case IdentifierToken(String identifier) -> identifier;
							case JavaKeywordToken(JavaKeyword kw) -> kw.name();
							case OperatorToken(int codepoint) when codepoint == '*' ->
									Character.toString((char) codepoint);
							default -> null;
						})
						.filter(Objects::nonNull)
						.reduce((_, second) -> second)
						.map(t -> t.equals("*") || usedIdentifiers.contains(t))
						.orElse(false);
			}
		};
	}
}
