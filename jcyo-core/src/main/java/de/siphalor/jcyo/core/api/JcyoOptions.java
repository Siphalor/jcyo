package de.siphalor.jcyo.core.api;

import de.siphalor.jcyo.core.api.import_order.ImportOrder;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Builder
@Value
public class JcyoOptions {
	boolean updateInput;
	char commandPrefix = '#';
	char disabledPrefix = '-';
	@Nullable ImportOrder importOrder;
}
