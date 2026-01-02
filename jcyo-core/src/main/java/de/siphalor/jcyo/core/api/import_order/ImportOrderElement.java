package de.siphalor.jcyo.core.api.import_order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

public sealed interface ImportOrderElement {
	record Prefix(String importPrefix, boolean staticImport) implements ImportOrderElement {}

	static BlankLine blankLine() {
		return BlankLine.INSTANCE;
	}

	static Rest rest(boolean staticImport) {
		return staticImport ? staticRest() : normalRest();
	}

	static Rest normalRest() {
		return Rest.NORMAL;
	}

	static Rest staticRest() {
		return Rest.STATIC;
	}

	@ApiStatus.Internal
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	final class BlankLine implements ImportOrderElement {
		private static final BlankLine INSTANCE = new BlankLine();
	}
	@ApiStatus.Internal
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	final class Rest implements ImportOrderElement {
		private static final Rest NORMAL = new Rest(false);
		public static final Rest STATIC = new Rest(true);

		private final boolean staticImport;
	}
}
