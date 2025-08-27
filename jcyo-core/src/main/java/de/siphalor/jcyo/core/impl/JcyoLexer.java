package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;

@CommonsLog
@RequiredArgsConstructor
public class JcyoLexer implements AutoCloseable, TokenStream {
	private final Reader reader;
	private final JcyoOptions options;

	private final StringBuilder buffer = new StringBuilder();

	private int peek = -1;
	private @Nullable Token nextToken = null;
	private boolean inDisabledFlexComment = false;

	@Override
	public Token nextToken() {
		if (nextToken != null) {
			Token token = nextToken;
			nextToken = null;
			return token;
		}

		while (true) {
			int codepoint = peek();
			switch (codepoint) {
				case -1 -> {
					return EofToken.instance();
				}
				case '/' -> {
					buffer.appendCodePoint(eat());
					codepoint = peek();
					if (codepoint == '/') {
						buffer.appendCodePoint(eat());
						codepoint = peek();
						if (codepoint == options.commandPrefix()) {
							buffer.appendCodePoint(eat());
							return new JcyoDirectiveStartToken(takeFromBuffer(), CommentStyle.LINE);
						} else if (codepoint == options.disabledPrefix()) {
							buffer.appendCodePoint(eat());
							return new JcyoDisabledStartToken(takeFromBuffer(), CommentStyle.LINE);
						} else if (codepoint == '/') {
							buffer.appendCodePoint(eat());
							chompToAnyExclusive(new int[]{'\n', '\r'});
							return new PlainJavaCommentToken(takeFromBuffer(), CommentStyle.LINE, true);
						} else {
							chompToAnyExclusive(new int[]{'\n', '\r'});
							return new PlainJavaCommentToken(takeFromBuffer(), CommentStyle.LINE, false);
						}
					} else if (codepoint == '*') {
						buffer.appendCodePoint(eat());
						codepoint = peek();
						if (codepoint == options.commandPrefix()) {
							buffer.appendCodePoint(eat());
							return new JcyoDirectiveStartToken(takeFromBuffer(), CommentStyle.FLEX);
						} else if (codepoint == options.disabledPrefix()) {
							buffer.appendCodePoint(eat());
							if (inDisabledFlexComment) {
								log.warn(
										"Encountered nested disabled flex comment."
												+ "This is not supported and will be ignored."
								);
								continue;
							}
							inDisabledFlexComment = true;
							return new JcyoDisabledStartToken(takeFromBuffer(), CommentStyle.FLEX);
						} else {
							boolean javadoc = codepoint == '*';
							if (javadoc) {
								buffer.appendCodePoint(eat());
							}
							chompToMultilineCommentEnd();
							// A plain java flex comment implicitly ends a disabled flex comment
							if (inDisabledFlexComment) {
								inDisabledFlexComment = false;
								nextToken = JcyoEndToken.implicit();
							}
							return new PlainJavaCommentToken(takeFromBuffer(), CommentStyle.FLEX, javadoc);
						}
					} else {
						clearBuffer();
						return new OperatorToken('/');
					}
				}
				case '*' -> {
					buffer.appendCodePoint(eat());
					if (peek() == '/') {
						// This should only be the end of a JCYO directive
						buffer.appendCodePoint(eat());
						// this ends disabled flex comments implicitly
						if (inDisabledFlexComment) {
							inDisabledFlexComment = false;
							nextToken = JcyoEndToken.implicit();
						}
						return new JcyoEndToken(takeFromBuffer());
					} else {
						clearBuffer();
						return new OperatorToken('*');
					}
				}
				case '\'' -> {
					buffer.appendCodePoint(eat());
					chompToInclusive('\'');
					return new CharacterLiteralToken(takeFromBuffer());
				}
				case '"' -> {
					buffer.appendCodePoint(eat());
					if (eatToBuffer() == '"') {
						if (eatToBuffer() == '"') {
							int quoteCount = 0;
							while (true) {
								codepoint = eatToBuffer();
								if (codepoint == '\\') {
									quoteCount = 0;
									buffer.appendCodePoint(eat());
								} else if (codepoint == '"') {
									if (++quoteCount >= 3) {
										break;
									}
								} else if (codepoint == -1) {
									break;
								} else {
									quoteCount = 0;
								}
							}
						}
						return new StringLiteralToken(takeFromBuffer());
					} else {
						while (true) {
							codepoint = eatToBuffer();
							if (codepoint == '\\') {
								buffer.appendCodePoint(eat());
							} else if (codepoint == '"') {
								return new StringLiteralToken(takeFromBuffer());
							} else if (codepoint == -1) {
								return new StringLiteralToken(takeFromBuffer());
							}
						}
					}
				}
				case '\n' -> {
					buffer.appendCodePoint(eat());
					if (peek() == '\r') {
						buffer.appendCodePoint(eat());
						return new LineBreakToken(takeFromBuffer());
					}
					return new LineBreakToken(takeFromBuffer());
				}
				case '\r' -> {
					buffer.appendCodePoint(eat());
					if (peek() == '\n') {
						buffer.appendCodePoint(eat());
						return new LineBreakToken(takeFromBuffer());
					}
					return new LineBreakToken(takeFromBuffer());
				}
				case '.' -> {
					eat();
					if (Character.isDigit(peek())) {
						chompNumeric();
						return new NumberLiteralToken(takeFromBuffer());
					} else {
						clearBuffer();
						return new OperatorToken('.');
					}
				}
				default -> {
					if (Character.isJavaIdentifierStart(codepoint)) {
						do {
							buffer.appendCodePoint(eat());
						} while (Character.isJavaIdentifierPart(peek()));
						String identifier = takeFromBuffer();
						JavaKeyword keyword = JavaKeyword.getKeyword(identifier);
						if (keyword != null) {
							return new JavaKeywordToken(keyword);
						} else {
							return new IdentifierToken(identifier);
						}
					} else if (Character.isWhitespace(codepoint)) {
						do {
							buffer.appendCodePoint(eat());
						} while (Character.isWhitespace(peek()));
						return new WhitespaceToken(takeFromBuffer());
					} else if (Character.isDigit(codepoint)) {
						chompNumeric();
						return new NumberLiteralToken(takeFromBuffer());
					} else {
						eat();
						return new OperatorToken(codepoint);
					}
				}
			}
		}
	}

	private void chompToMultilineCommentEnd() {
		int codepoint;
		while (true) {
			codepoint = eatToBuffer();
			if (codepoint == '*') {
				codepoint = eatToBuffer();
				if (codepoint == '/') {
					break;
				}
			} else if (codepoint == -1) {
				break;
			}
		}
	}

	private void chompNumeric() {
		while (true) {
			int codepoint = peek();
			if (codepoint == -1) {
				break;
			} else if (Character.isDigit(codepoint) || codepoint == '.' || Character.isLetter(codepoint)) {
				buffer.appendCodePoint(eat());
			} else {
				break;
			}
		}
	}

	private void chompToInclusive(int stop) {
		while (true) {
			int codepoint = eatToBuffer();
			if (codepoint == stop || codepoint == -1) {
				return;
			}
		}
	}

	private void chompToAnyExclusive(int[] stops) {
		int codepoint;
		while (true) {
			codepoint = peek();
			for (int stop : stops) {
				if (codepoint == stop) {
					return;
				}
			}
			if (codepoint == -1) {
				return;
			}
			buffer.appendCodePoint(eat());
		}
	}

	private int eatToBuffer() {
		int codepoint = eat();
		if (codepoint != -1) {
			buffer.appendCodePoint(codepoint);
		}
		return codepoint;
	}

	private int eat() {
		if (peek >= 0) {
			int value = peek;
			peek = -1;
			return value;
		}
		return readNextCodepoint();
	}

	private int peek() {
		if (peek < 0) {
			peek = readNextCodepoint();
		}
		return peek;
	}

	private int readNextCodepoint() {
		try {
			return reader.read();
		} catch (IOException e) {
			throw new JcyoParseException(e);
		}
	}

	private String takeFromBuffer() {
		String string = buffer.toString();
		clearBuffer();
		return string;
	}

	private void clearBuffer() {
		buffer.setLength(0);
	}

	@Override
	public void close() throws Exception {
		reader.close();
	}
}
