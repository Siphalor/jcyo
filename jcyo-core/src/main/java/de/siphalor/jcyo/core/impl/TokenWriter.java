package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

@RequiredArgsConstructor
public class TokenWriter implements AutoCloseable {
	private final Writer writer;
	private final JcyoDisabledState disabledState = new JcyoDisabledState();

	public void writeAll(TokenStream tokenStream) throws IOException {
		Token token;
		while (true) {
			token = tokenStream.nextToken();
			if (token instanceof EofToken) {
				return;
			}
			write(token);
		}
	}

	public void write(Token token) throws IOException {
		switch (token) {
			case EofToken _ -> {}
			case JcyoDisabledStartToken startToken -> {
				disabledState.disabledStartToken(startToken);
				writer.write(token.raw());
			}
			case JcyoEndToken _ when disabledState.disabled() -> {
				disabledState.reset();
				writer.write(token.raw());
			}
			case LineBreakToken _ -> {
				disabledState.disabledStartToken().ifPresent(startToken -> {
					if (startToken.commentStyle() != CommentStyle.LINE) {
						disabledState.reset();
					}
				});
				writer.write(token.raw());
			}
			case PlainJavaCommentToken(String rawComment, CommentStyle commentStyle, _)
					when commentStyle == CommentStyle.FLEX
					&& disabledState.disabledStartToken().orElse(null)
					instanceof JcyoDisabledStartToken(String disabledStartRaw, CommentStyle disabledStyle)
					&& disabledStyle == CommentStyle.LINE -> {
				int index = 0;
				boolean charsInLine = false;
				boolean commentedLine = true;
				while (index < rawComment.length()) {
					char c = rawComment.charAt(index);
					if (commentedLine) {
						if (c == '\n' || c == '\r') {
							commentedLine = false;
							charsInLine = false;
						}
						writer.write(c);
					} else {
						if (c == '\n' || c == '\r') {
							if (charsInLine) {
								writer.write(disabledStartRaw);
							}
							writer.write(c);
							charsInLine = false;
						} else if (Character.isWhitespace(c)) {
							writer.write(c);
							charsInLine = true;
						} else {
							writer.write(disabledStartRaw);
							commentedLine = true;
							writer.write(c);
							charsInLine = true;
						}
					}
					index++;
				}
			}
			default -> writer.write(token.raw());
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Data
	private static class JcyoDisabledState {
		private @Nullable JcyoDisabledStartToken disabledStartToken;
		private boolean disabledPendingOnLine;

		public Optional<JcyoDisabledStartToken> disabledStartToken() {
			return Optional.ofNullable(disabledStartToken);
		}

		public boolean disabled() {
			return disabledStartToken != null;
		}

		public void reset() {
			disabledStartToken = null;
			disabledPendingOnLine = false;
		}
	}
}
