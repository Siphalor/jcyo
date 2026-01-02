package de.siphalor.jcyo.core.api.import_order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImportOrder {
	private static final Pattern SPOTLESS_IMPORT_DELIMITER = Pattern.compile("\\|");
	private static final Pattern IMPORT_PREFIX = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*");

	@Getter
	private final List<ImportOrderElement> elements;

	public static ImportOrder fromSpotless(List<String> elements) {
		List<ImportOrderElement> mapped = new ArrayList<>(elements.size() * 2);
		for (String element : elements) {
			if (!mapped.isEmpty()) {
				mapped.add(ImportOrderElement.blankLine());
			}
			boolean _static = element.startsWith("\\#");
			if (_static) {
				element = element.substring(2);
			}
			if (element.isEmpty()) {
				mapped.add(ImportOrderElement.rest(_static));
			} else {
				SPOTLESS_IMPORT_DELIMITER.splitAsStream(element)
						.map(part -> new ImportOrderElement.Prefix(part, _static))
						.forEachOrdered(mapped::add);
			}
		}
		return ofElements(mapped);
	}

	public static ImportOrder ofElements(List<ImportOrderElement> elements) {
		if (elements.getFirst() instanceof ImportOrderElement.BlankLine
				|| elements.getLast() instanceof ImportOrderElement.BlankLine) {
			throw new IllegalArgumentException("Import order must not begin or end with a blank line");
		}

		var rests = elements.stream()
				.filter(element -> element instanceof ImportOrderElement.Rest)
				.collect(Collectors.groupingBy(element -> ((ImportOrderElement.Rest) element).staticImport(), Collectors.counting()));
		if (rests.getOrDefault(true, 0L) > 1 || rests.getOrDefault(false, 0L) > 1) {
			throw new IllegalArgumentException("Import order must contain at most one rest");
		}

		for (ImportOrderElement element : elements) {
			if (element instanceof ImportOrderElement.Prefix(String importPrefix, _)) {
				if (!IMPORT_PREFIX.matcher(importPrefix).matches()) {
					throw new  IllegalArgumentException(
							"Import prefix element \"" + importPrefix + "\" isn't in correct format"
					);
				}
			}
		}

		return new ImportOrder(elements);
	}
}
