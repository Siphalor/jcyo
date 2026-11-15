package de.siphalor.jcyo.core.impl.transform;

import de.siphalor.jcyo.core.api.import_order.ImportOrder;
import de.siphalor.jcyo.core.api.import_order.ImportOrderElement;
import de.siphalor.jcyo.core.impl.directive.DirectiveParser;
import de.siphalor.jcyo.core.impl.directive.JcyoDirective;
import de.siphalor.jcyo.core.impl.stream.PeekableTokenStream;
import de.siphalor.jcyo.core.impl.stream.TokenBuffer;
import de.siphalor.jcyo.core.impl.stream.TokenStream;
import de.siphalor.jcyo.core.impl.token.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import javax.sound.sampled.Line;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class JcyoImportReorderer {
	private final ImportOrder importOrder;
	private final List<Map.Entry<Integer, ImportOrderElement.Prefix>> prefixOrderElements;

	public JcyoImportReorderer(ImportOrder importOrder) {
		this.importOrder = importOrder;
		this.prefixOrderElements = new ArrayList<>(importOrder.elements().size());
		for (int i = 0; i < importOrder.elements().size(); i++) {
			ImportOrderElement element = importOrder.elements().get(i);
			if (element instanceof ImportOrderElement.Prefix prefix) {
				prefixOrderElements.add(Map.entry(i, prefix));
			}
		}
	}

	public TokenStream apply(PeekableTokenStream input) {
		List<Token> buffer = new ArrayList<>();

		loop:
		while (true) {
			switch (input.peekToken()) {
				case WhitespaceToken _, LineBreakToken _, PlainJavaCommentToken _, IdentifierToken _, OperatorToken _:
					buffer.add(input.nextToken());
					break;
				case JavaKeywordToken keywordToken when keywordToken.keyword() == JavaKeyword.PACKAGE:
					buffer.add(input.nextToken());
					break;
				default:
					break loop;
			}
		}

		SectionProcessResult sectionProcessResult = processSection(input);
		Section section = sectionProcessResult.section();

		List<Stream<Token>> tokenStreams = new ArrayList<>(4);
		tokenStreams.add(buffer.stream());
		tokenStreams.add(section.tokens().stream());

		if (!(input.peekToken() instanceof EofToken)) {
			if (sectionProcessResult.pendingWhitespace() != null) {
				tokenStreams.add(sectionProcessResult.pendingWhitespace().stream());
			} else if (!section.tokens().isEmpty()) {
				tokenStreams.add(Stream.of(LineBreakToken.defaultInstance()));
			}
			tokenStreams.add(input.stream());
		}
		return TokenStream.from(tokenStreams.stream().flatMap(Function.identity()).iterator());
	}

	private Section processBlockDirectiveSection(
			PeekableTokenStream input,
			JcyoDirective startDirective,
			TokenBuffer buffer
	) {
		var sectionBuilder = new SectionBuilder(buffer);

		SectionProcessResult sectionProcessResult = processSection(input);
		sectionBuilder.append(sectionProcessResult.section);

		if (sectionProcessResult.pendingWhitespace() != null) {
			sectionProcessResult.pendingWhitespace().stream().forEach(sectionBuilder.buffer()::pushToken);
		}

		if (sectionProcessResult.endDirective != null) {
			if (sectionProcessResult.endDirective.isBlockBegin()) {
				Section sibling = processBlockDirectiveSection(
						input,
						sectionProcessResult.endDirective,
						new TokenBuffer()
				);
				sectionBuilder.append(sibling);
			}
		} else {
			var endingDirective = chompToEndingDirective(input, buffer, startDirective);
			if (endingDirective != null && endingDirective.isBlockBegin()) {
				Section sibling = processBlockDirectiveSection(
						input,
						endingDirective,
						new TokenBuffer()
				);
				sectionBuilder.append(sibling);
			}
		}

		return sectionBuilder.build();
	}

	private @Nullable JcyoDirective chompToEndingDirective(
			PeekableTokenStream input,
			TokenBuffer buffer,
			JcyoDirective startDirective
	) {
		Deque<JcyoDirective> directiveStack = new ArrayDeque<>();
		directiveStack.push(startDirective);
		while (true) {
			Token token = input.peekToken();

			if (token instanceof EofToken) {
				return null;
			} else if (token instanceof JcyoDirectiveStartToken) {
				var directive = new DirectiveParser(buffer.copying(input)).nextDirective();
				if (directive.ends(Objects.requireNonNull(directiveStack.peek()))) {
					directiveStack.pop();
					if (directiveStack.isEmpty()) {
						return directive;
					}
				}
				if (directive.isBlockBegin()) {
					directiveStack.push(directive);
				}
			} else {
				buffer.pushToken(input.nextToken());
			}
		}
	}

	private SectionProcessResult processSection(PeekableTokenStream input) {
		List<Element> elements = new ArrayList<>();
		TokenBuffer endBuffer = new TokenBuffer();

		Token token;
		JcyoDirective endDirective = null;
		loop:
		while (true) {
			token = input.peekToken();
			switch (token) {
				case JavaKeywordToken(JavaKeyword keyword) when keyword == JavaKeyword.IMPORT:
					endBuffer.clear();
					elements.add(parseImport(input));
					break;
				case LineBreakToken _, WhitespaceToken _:
					endBuffer.pushToken(input.nextToken());
					break;
				case JcyoDirectiveStartToken _:
					endBuffer.clear();
					JcyoDirective directive = new DirectiveParser(endBuffer.copying(input)).nextDirective();
					if (directive.isBlockEnd()) {
						endDirective = directive;
						break loop;
					} else if (directive.isBlockBegin()) {
						Section section = processBlockDirectiveSection(input, directive, endBuffer);
						elements.add(section);
						endBuffer = new TokenBuffer();
					} else {
						elements.add(new Other(endBuffer.stream().toList()));
						endBuffer.clear();
					}
					break;
				default:
					break loop;
			}
		}

		elements.sort(Comparator.naturalOrder());

		var firstImport = elements.stream()
				.filter(element -> element instanceof JcyoImportReorderer.Import)
				.findFirst()
				.map(Import.class::cast)
				.orElse(null);

		var lastImport = firstImport == null ? null : elements.reversed()
				.stream()
				.filter(element -> element instanceof JcyoImportReorderer.Import)
				.findFirst()
				.map(Import.class::cast)
				.orElse(null);

		var resultTokens = renderElements(elements);

		endBuffer.finish();
		if (endDirective != null) {
			// we're exiting because of an end directive, meaning the directive tokens are in the end buffer
			endBuffer.stream().forEach(resultTokens::add);
			return new SectionProcessResult(new Section(resultTokens, firstImport, lastImport), endDirective, null);
		} else {
			// we're exiting because we ran into non-import elements, the end buffer will contain pending whitespace
			// for the non-import code, that should be retained exactly as-is.
			return new SectionProcessResult(new Section(resultTokens, firstImport, lastImport), null, endBuffer);
		}
	}

	private SequencedCollection<Token> renderElements(SequencedCollection<Element> elements) {
		List<Token> result = new ArrayList<>();

		int lastOrderIndex = -Integer.MAX_VALUE;
		String lastImportPath = null; // used to detect simple duplicate imports

		for (Element element : elements) {
			switch (element) {
				case Import _import -> {
					// If the current import has a lower order index than the last processed import
					// then this was caused by sectioning, we should not insert a blank line.
					if (lastOrderIndex >= 0 && _import.orderIndex() > lastOrderIndex) {
						if (
								importOrder.elements().subList(lastOrderIndex, _import.orderIndex())
										.contains(ImportOrderElement.blankLine())
						) {
							result.add(LineBreakToken.defaultInstance());
						}
					} else if (
							lastImportPath != null
									&& lastImportPath.length() == _import.importPath.length()
									&& _import.importPath.endsWith(lastImportPath)
					) {
						// skip duplicate imports
						continue;
					}
					result.addAll(_import.tokens());

					lastOrderIndex = _import.orderIndex();
					lastImportPath = _import.importPath;

					result.add(LineBreakToken.defaultInstance());
				}
				case Section section -> {
					if (section.firstImport != null) {
						if (lastOrderIndex >= 0 && section.firstImport.orderIndex() != lastOrderIndex) {
							if (
									importOrder.elements().subList(lastOrderIndex, section.firstImport.orderIndex())
											.contains(ImportOrderElement.blankLine())
							) {
								result.add(LineBreakToken.defaultInstance());
							}
						}
					} else if (lastOrderIndex >= 0) {
						result.add(LineBreakToken.defaultInstance());
					}
					result.addAll(section.tokens());

					if (section.lastImport != null) {
						lastOrderIndex = section.lastImport.orderIndex();
					}

					lastImportPath = null;
				}
				case Other other -> result.addAll(other.tokens());
			}
		}

		return result;
	}

	private Import parseImport(PeekableTokenStream input) {
		var importTokens = new ArrayList<Token>();
		while (true) {
			Token token = input.peekToken();
			if (token instanceof EofToken) {
				break;
			} else if (token instanceof OperatorToken(int codepoint) && codepoint == ';') {
				importTokens.add(input.nextToken());
				break;
			} else {
				importTokens.add(input.nextToken());
			}
		}

		// We're chomping a whitespace until a line break here,
		// because we assume that whitespace after an import is never an explicit choice.
		// The line break will be regenerated in the output logic,
		// whitespace in front of it is treated as a formatting error and will be discarded.
		while (true) {
			Token token = input.peekToken();
			if (token instanceof WhitespaceToken) {
				input.nextToken();
			} else if (token instanceof LineBreakToken) {
				input.nextToken();
				break;
			} else {
				break;
			}
		}

		var pathBuilder = new StringBuilder();

		Iterator<Token> iter = importTokens.stream()
				.filter(t -> !(t instanceof WhitespaceToken))
				.iterator();

		iter.next();

		var isStatic = false;
		var pToken = iter.next();
		if (pToken instanceof JavaKeywordToken(JavaKeyword pKeyword) && pKeyword == JavaKeyword.STATIC) {
			isStatic = true;
		} else if (pToken instanceof RepresentableToken rToken) {
			pathBuilder.append(rToken.raw());
		}

		while (iter.hasNext()) {
			pToken = iter.next();
			if (pToken instanceof OperatorToken(int codepoint) && codepoint == ';') {
				break;
			}
			if (pToken instanceof RepresentableToken rToken) {
				pathBuilder.append(rToken.raw());
			}
		}

		var path = pathBuilder.toString();
		var orderIndex = getImportOrderIndex(isStatic, path);
		return new Import(importTokens, isStatic, path, orderIndex);
	}

	private int getImportOrderIndex(boolean isStatic, String importPath) {
		return prefixOrderElements.stream()
				.filter(element -> element.getValue().staticImport() == isStatic
						&& importPath.startsWith(element.getValue().importPrefix()))
				.max(Map.Entry.comparingByValue(Comparator.comparingInt(element -> element.importPrefix().length())))
				.map(Map.Entry::getKey)
				.or(() -> {
					int i = importOrder.elements().indexOf(ImportOrderElement.rest(isStatic));
					return i >= 0 ? Optional.of(i) : Optional.empty();
				})
				.orElse(prefixOrderElements.size());
	}

	record SectionProcessResult(
			Section section,
			@Nullable JcyoDirective endDirective,
			@Nullable TokenStream pendingWhitespace
	) {}

	sealed interface Element extends Comparable<Element> {
		@Override
		default int compareTo(JcyoImportReorderer.Element other) {
			var thisImport = getRepresentativeImportForElement(this);
			var otherImport = getRepresentativeImportForElement(other);

			int cmp = Integer.compare(
					thisImport.map(Import::orderIndex).orElse(Integer.MAX_VALUE),
					otherImport.map(Import::orderIndex).orElse(Integer.MAX_VALUE)
			);
			if (cmp != 0) return cmp;

			return IMPORT_PATH_COMPARATOR.compare(
					thisImport.map(Import::importPath).orElse(null),
					otherImport.map(Import::importPath).orElse(null)
			);
		}
	}

	private static final Comparator<@Nullable String> IMPORT_PATH_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private static Optional<Import> getRepresentativeImportForElement(Element element) {
		return switch (element) {
			case Import _import -> Optional.of(_import);
			case Section section -> Optional.ofNullable(section.firstImport);
			case Other _ -> Optional.empty();
		};
	}

	record Import(SequencedCollection<Token> tokens, boolean isStatic, String importPath, int orderIndex) implements Element {
	}

	record Section(SequencedCollection<Token> tokens, @Nullable Import firstImport, @Nullable Import lastImport) implements Element {
	}

	record Other(SequencedCollection<Token> tokens) implements Element {
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	static class SectionBuilder {
		private final TokenBuffer buffer;
		private @Nullable Import firstImport;
		private @Nullable Import lastImport;

		Section build() {
			buffer.finish();
			return new Section(buffer.stream().toList(), firstImport, lastImport);
		}

		void append(Section section) {
			section.tokens().forEach(buffer::pushToken);
			if (firstImport == null) {
				firstImport = section.firstImport;
			}
			lastImport = section.lastImport;
		}
	}
}
