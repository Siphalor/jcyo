package de.siphalor.jcyo.core.impl.expression;

import de.siphalor.jcyo.core.impl.JcyoParseException;
import de.siphalor.jcyo.core.impl.expression.value.JcyoBoolean;
import de.siphalor.jcyo.core.impl.expression.value.JcyoNumber;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.JavaKeyword;
import de.siphalor.jcyo.core.impl.token.JavaKeywordToken;
import de.siphalor.jcyo.core.impl.token.NumberLiteralToken;
import de.siphalor.jcyo.core.impl.token.Token;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExpressionParser {
	private final TokenStream tokenStream;

	public JcyoExpression nextExpression() {
		Token token = tokenStream.nextToken();
		switch (token) {
			case JavaKeywordToken(JavaKeyword keyword) -> {
				if (keyword == JavaKeyword.TRUE) {
					return new JcyoConstant(new JcyoBoolean(true));
				} else if (keyword == JavaKeyword.FALSE) {
					return new JcyoConstant(new JcyoBoolean(false));
				} else {
					throw new JcyoParseException("Unexpected keyword " + keyword);
				}
			}
			case NumberLiteralToken(String raw) -> {
				try {
					return new JcyoConstant(new JcyoNumber(Double.parseDouble(raw)));
				} catch (NumberFormatException e) {
					throw new JcyoParseException("Unexpected number literal " + raw);
				}
			}
			default -> throw new JcyoParseException("Unexpected token " + token);
		}
	}
}
