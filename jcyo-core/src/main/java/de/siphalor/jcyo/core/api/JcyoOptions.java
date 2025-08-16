package de.siphalor.jcyo.core.api;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class JcyoOptions {
	boolean updateInput;
	char commandPrefix = '#';
	char disabledPrefix = '-';
}
