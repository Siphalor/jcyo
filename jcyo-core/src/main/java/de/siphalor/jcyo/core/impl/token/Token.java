package de.siphalor.jcyo.core.impl.token;

public sealed interface Token
		permits EofToken, JcyoDisabledRegionEndToken, JcyoDisabledRegionStartToken, RepresentableToken {
}
