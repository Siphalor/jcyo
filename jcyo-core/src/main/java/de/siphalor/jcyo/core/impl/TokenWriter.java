package de.siphalor.jcyo.core.impl;

import de.siphalor.jcyo.core.api.JcyoOptions;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenWriter implements AutoCloseable {
	private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\r\n|\r|\n|$");

	private final Writer writer;
	private final JcyoHelper helper;

	private @Nullable JcyoDisabledState disabledState;

	public TokenWriter(Writer writer, JcyoOptions options) {
		this(writer, new JcyoHelper(options));
	}

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
		if (disabledState == null) {
			writePlain(token);
			return;
		}
		switch (disabledState.disabledStartToken().suggestedCommentStyle()) {
			case LINE -> writeInDisabledLineMode(token);
			case FLEX -> writeInDisabledFlexMode(token);
		}
	}

	private void writePlain(Token token) throws IOException {
		assert disabledState == null;
		switch (token) {
			case EofToken _ -> {}
			case RepresentableToken representableToken -> writer.write(representableToken.raw());
			case JcyoDisabledRegionStartToken startToken -> disabledState = new JcyoDisabledState(startToken);
			default -> throw new IllegalArgumentException("Unexpected token: " + token);
		}
	}

	private void writeInDisabledLineMode(Token token) throws IOException {
		assert disabledState != null;
		switch (token) {
			case EofToken _ -> {}
			case JcyoDisabledRegionEndToken _ -> disabledState = null;
			case JcyoDisabledRegionStartToken _ -> throw new IllegalArgumentException(
					"Unexpected disabled region start token, already in disabled region: " + token
			);
			case LineBreakToken lineBreakToken -> {
				writer.write(lineBreakToken.raw());
				disabledState.reset();
			}
			case WhitespaceToken whitespaceToken when !disabledState.disabledPending() ->
					writer.write(whitespaceToken.raw());
			case WhitespaceToken whitespaceToken when
					disabledState.fulfilledIndent() < disabledState.disabledStartToken().suggestedIndent().length() -> {
				String suggestedIndent = disabledState.disabledStartToken().suggestedIndent();
				if ((whitespaceToken.codepoint() == '\t') == (suggestedIndent.charAt(disabledState.fulfilledIndent()) == '\t')) {
					disabledState.fulfilledIndent(disabledState.fulfilledIndent() + 1);
					writer.write(whitespaceToken.raw());
				} else {
					writer.write(helper.disabledForLine());
					disabledState.disabledPending(false);
					writer.write(whitespaceToken.raw());
				}
			}
			case PlainJavaCommentToken commentToken when commentToken.commentStyle() == CommentStyle.FLEX ->
					writeFlexCommentInDisabledLineMode(commentToken);
			case RepresentableToken representableToken when disabledState.disabledPending() -> {
				writer.write(helper.disabledForLine());
				disabledState.disabledPending(false);
				writer.write(representableToken.raw());
			}
			case RepresentableToken representableToken -> writer.write(representableToken.raw());
		}
	}

	private void writeFlexCommentInDisabledLineMode(PlainJavaCommentToken commentToken) throws IOException {
		assert disabledState != null;
		if (disabledState.disabledPending()) {
			writer.write(helper.disabledForLine());
			disabledState.disabledPending(false);
		}
		String raw = commentToken.raw();
		Matcher matcher = LINE_BREAK_PATTERN.matcher(raw);
		int pos = 0;
		if (matcher.find()) {
			writer.write(raw, pos, matcher.end() - pos);
			pos = matcher.end();
		}
		String suggestedIndent = disabledState.disabledStartToken().suggestedIndent();
		while (matcher.find()) {
			int lineStart = pos;
			for (; pos < matcher.start(); pos++) {
				if (pos - lineStart >= suggestedIndent.length()) {
					if (pos + 1 >= matcher.start()) {
						writer.write(helper.disabledForLineNoWhitespace());
					} else {
						writer.write(helper.disabledForLine());
					}
					break;
				}

				char rawChar = raw.charAt(pos);
				char suggestedIndentChar = suggestedIndent.charAt(pos - lineStart);
				if (rawChar == ' ' && suggestedIndentChar != '\t') {
					writer.write(rawChar);
				} else if (rawChar == '\t' && suggestedIndentChar == '\t') {
					writer.write(rawChar);
				} else {
					if (pos + 1 >= matcher.start()) {
						writer.write(helper.disabledForLineNoWhitespace());
					} else {
						writer.write(helper.disabledForLine());
					}
					break;
				}
			}
			writer.write(raw, pos, matcher.end() - pos);
			pos = matcher.end();
		}
	}

	private void writeInDisabledFlexMode(Token token) throws IOException {
		assert disabledState != null;
		switch (token) {
			case EofToken _ -> {}
			case JcyoDisabledRegionEndToken _ -> {
				writer.write(helper.disabledForFlexEnd());
				disabledState = null;
			}
			case JcyoDisabledRegionStartToken _ -> throw new IllegalArgumentException(
					"Unexpected disabled region start token, already in disabled region: " + token
			);
			case JcyoEndToken endToken -> {
				writer.write(endToken.raw());
				disabledState.disabledPending(true);
			}
			case PlainJavaCommentToken commentToken when commentToken.commentStyle() == CommentStyle.FLEX -> {
				writer.write(commentToken.raw());
				disabledState.disabledPending(true);
			}
			case LineBreakToken lineBreakToken when disabledState.disabledPending() -> {
				writer.write(helper.disabledForFlexStartNoWhitespace());
				disabledState.disabledPending(false);
				writer.write(lineBreakToken.raw());
			}
			case RepresentableToken representableToken when disabledState.disabledPending() -> {
				writer.write(helper.disabledForFlexStart());
				disabledState.disabledPending(false);
				writer.write(representableToken.raw());
			}
			case RepresentableToken representableToken -> writer.write(representableToken.raw());
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Data
	private static class JcyoDisabledState {
		private final JcyoDisabledRegionStartToken disabledStartToken;
		private boolean disabledPending = true;
		private int fulfilledIndent;

		public void reset() {
			disabledPending = true;
			fulfilledIndent = 0;
		}
	}
}
