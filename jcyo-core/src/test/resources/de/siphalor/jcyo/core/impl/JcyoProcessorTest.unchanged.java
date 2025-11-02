package de.siphalor.tweed5.coat.bridge.impl;

import de.siphalor.coat.handler.ConfigEntryHandler;
import de.siphalor.coat.input.CheckBoxConfigInput;
import de.siphalor.coat.input.ConfigInput;
import de.siphalor.coat.input.CycleButtonConfigInput;
import de.siphalor.coat.input.TextConfigInput;
import de.siphalor.coat.list.complex.ConfigCategoryWidget;
import de.siphalor.coat.list.entry.ConfigCategoryConfigEntry;
import de.siphalor.coat.screen.ConfigContentWidget;
import de.siphalor.coat.util.EnumeratedMaterial;
import de.siphalor.tweed5.attributesextension.api.AttributesExtension;
import de.siphalor.tweed5.coat.bridge.api.TweedCoatAttributes;
import de.siphalor.tweed5.coat.bridge.api.mapping.TweedCoatEntryCreationContext;
import de.siphalor.tweed5.coat.bridge.api.mapping.TweedCoatEntryMappingContext;
import de.siphalor.tweed5.coat.bridge.api.mapping.TweedCoatEntryMappingResult;
import de.siphalor.tweed5.coat.bridge.api.mapping.TweedCoatMapper;
import de.siphalor.tweed5.coat.bridge.api.mapping.handler.BasicTweedCoatEntryHandler;
import de.siphalor.tweed5.coat.bridge.api.mapping.handler.ConvertingTweedCoatEntryHandler;
import de.siphalor.tweed5.core.api.entry.CompoundConfigEntry;
import de.siphalor.tweed5.core.api.entry.ConfigEntry;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.siphalor.tweed5.coat.bridge.api.TweedCoatMappingUtils.translatableComponentWithFallback;

@CommonsLog
@SuppressWarnings("unchecked")
public class TweedCoatMappersImpl {
	public static TweedCoatMapper<Byte> BYTE_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Byte.class, byte.class},
			value -> Byte.toString(value),
			Byte::parseByte
	);
	public static TweedCoatMapper<Short> SHORT_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Short.class, short.class},
			value -> Short.toString(value),
			Short::parseShort
	);
	public static TweedCoatMapper<Integer> INTEGER_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Integer.class, int.class},
			value -> Integer.toString(value),
			Integer::parseInt
	);
	public static TweedCoatMapper<Long> LONG_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Long.class, long.class},
			value -> Long.toString(value),
			Long::parseLong
	);
	public static TweedCoatMapper<Float> FLOAT_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Float.class, float.class},
			value -> Float.toString(value),
			Float::parseFloat
	);
	public static TweedCoatMapper<Double> DOUBLE_TEXT_MAPPER = convertingTextMapper(
			new Class[]{Double.class, double.class},
			value -> Double.toString(value),
			Double::parseDouble
	);
	public static TweedCoatMapper<Boolean> BOOLEAN_CHECKBOX_MAPPER
			= new SimpleMapper<Boolean>(new Class[]{Boolean.class, boolean.class}, CheckBoxConfigInput::new);

	public static TweedCoatMapper<String> STRING_TEXT_MAPPER = new SimpleMapper<String>(
			new Class[]{String.class},
			TextConfigInput::new
	);

	public static TweedCoatMapper<Enum<?>> ENUM_CYCLE_BUTTON_MAPPER = new EnumCycleButtonMapper<>();

	public static TweedCoatMapper<Object> COMPOUND_CATEGORY_MAPPER = new CompoundCategoryMapper<>();

	public static <T> TweedCoatMapper<T> convertingTextMapper(
			Class<T>[] valueClasses,
			Function<T, String> textMapper,
			Function<String, T> textParser
	) {
		return new ConvertingTextMapper<>(valueClasses, textMapper, textParser);
	}

	@RequiredArgsConstructor
	public static class ConvertingTextMapper<T> implements TweedCoatMapper<T> {
		private final Class<T>[] valueClasses;
		private final Function<T, String> textMapper;
		private final Function<String, T> textParser;

		@Override
		public TweedCoatEntryMappingResult<T, String> mapEntry(ConfigEntry<T> entry, TweedCoatEntryMappingContext context) {
			if (!anyClassMatches(entry.valueClass(), valueClasses)) {
				return TweedCoatEntryMappingResult.notApplicable();
			}

			return new TweedCoatEntryMappingResult<T, String>() {
				@Override
				public boolean isApplicable() {
					return true;
				}

				@Override
				public ConfigInput<String> createInput(TweedCoatEntryCreationContext<T> context) {
					return new TextConfigInput(textMapper.apply(context.currentValue()));
				}

				@Override
				public ConfigEntryHandler<String> createHandler(TweedCoatEntryCreationContext<T> context) {
					if (context.parentSaveHandler() == null) {
						throw new IllegalArgumentException("No parent save handler provided");
					}
					return new ConvertingTweedCoatEntryHandler<>(
							new BasicTweedCoatEntryHandler<>(
									context.entry(), context.defaultValue(), context.parentSaveHandler()
							),
							textMapper,
							textParser
					);
				}

				@Override
				public @Nullable ConfigContentWidget createContentWidget(TweedCoatEntryCreationContext<T> context) {
					return null;
				}
			};
		}
	}

	@RequiredArgsConstructor
	public static class EnumeratedMaterialCycleButtonMapper<T> implements TweedCoatMapper<T> {
		private final Class<T> valueClass;
		private final EnumeratedMaterial<T> material;

		@Override
		public TweedCoatEntryMappingResult<T, ?> mapEntry(ConfigEntry<T> entry, TweedCoatEntryMappingContext context) {
			if (!valueClass.isAssignableFrom(entry.valueClass())) {
				return TweedCoatEntryMappingResult.notApplicable();
			}

			return new CycleButtonMappingResult<>(material);
		}
	}

	private static class EnumCycleButtonMapper<T extends Enum<?>> implements TweedCoatMapper<T> {
		@Override
		public TweedCoatEntryMappingResult<T, ?> mapEntry(ConfigEntry<T> entry, TweedCoatEntryMappingContext context) {
			if (!Enum.class.isAssignableFrom(entry.valueClass())) {
				return TweedCoatEntryMappingResult.notApplicable();
			}
			Class<T> enumClass = entry.valueClass();

			String translationKeyPrefix = entry.container().extension(AttributesExtension.class)
					.map(extension -> extension.getAttributeValue(entry, TweedCoatAttributes.ENUM_TRANSLATION_KEY))
					.orElse(enumClass.getPackage().getName());

			CoatEnumMaterial<T> material = new CoatEnumMaterial<>(enumClass, translationKeyPrefix + ".");
			return new CycleButtonMappingResult<>(material);
		}
	}

	@RequiredArgsConstructor
	private static class CycleButtonMappingResult<T> implements TweedCoatEntryMappingResult<T, T> {
		private final EnumeratedMaterial<T> material;

		@Override
		public boolean isApplicable() {
			return true;
		}

		@Override
		public ConfigInput<T> createInput(TweedCoatEntryCreationContext<T> context) {
			return new CycleButtonConfigInput<>(material, false, context.currentValue());
		}

		@Override
		public ConfigEntryHandler<T> createHandler(TweedCoatEntryCreationContext<T> context) {
			if (context.parentSaveHandler() == null) {
				throw new IllegalArgumentException("No parent save handler provided");
			}
			return new BasicTweedCoatEntryHandler<>(
					context.entry(), context.defaultValue(), context.parentSaveHandler()
			);
		}

		@Override
		public @Nullable ConfigContentWidget createContentWidget(TweedCoatEntryCreationContext<T> context) {
			return null;
		}
	}

	@RequiredArgsConstructor
	private static class SimpleMapper<T> implements TweedCoatMapper<T> {
		private final Class<T>[] valueClasses;
		private final Function<T, ConfigInput<T>> inputFactory;

		@Override
		public TweedCoatEntryMappingResult<T, T> mapEntry(ConfigEntry<T> entry, TweedCoatEntryMappingContext context) {
			if (!anyClassMatches(entry.valueClass(), valueClasses)) {
				return TweedCoatEntryMappingResult.notApplicable();
			}

			return new TweedCoatEntryMappingResult<T, T>() {
				@Override
				public boolean isApplicable() {
					return true;
				}

				@Override
				public ConfigInput<T> createInput(TweedCoatEntryCreationContext<T> context) {
					return inputFactory.apply(context.currentValue());
				}

				@Override
				public ConfigEntryHandler<T> createHandler(TweedCoatEntryCreationContext<T> context) {
					if (context.parentSaveHandler() == null) {
						throw new IllegalArgumentException("No parent save handler provided");
					}
					return new BasicTweedCoatEntryHandler<>(
							context.entry(), context.defaultValue(), context.parentSaveHandler()
					);
				}

				@Override
				public @Nullable ConfigContentWidget createContentWidget(TweedCoatEntryCreationContext<T> context) {
					return null;
				}
			};
		}
	}

	private static class CompoundCategoryMapper<T> implements TweedCoatMapper<T> {
		@Override
		public TweedCoatEntryMappingResult<T, ?> mapEntry(ConfigEntry<T> entry, TweedCoatEntryMappingContext mappingContext) {
			@Value
			class MappedEntry<U> {
				String name;
				String translationKeyPrefix;
				ConfigEntry<U> entry;
				TweedCoatEntryMappingContext mappingContext;
				TweedCoatEntryMappingResult<U, ?> mappingResult;
			}

			if (!(entry instanceof CompoundConfigEntry)) {
				return TweedCoatEntryMappingResult.notApplicable();
			}
			CompoundConfigEntry<T> compoundEntry = (CompoundConfigEntry<T>) entry;

			Optional<AttributesExtension> attributesExtension = entry.container().extension(AttributesExtension.class);
			ResourceLocation backgroundTexture = attributesExtension
					.map(extension -> extension.getAttributeValue(
							entry,
							TweedCoatAttributes.BACKGROUND_TEXTURE
					))
					.map(ResourceLocation::tryParse)
					.orElse(null);
			String translationKey = attributesExtension
					.map(extension -> extension.getAttributeValue(
							entry,
							TweedCoatAttributes.TRANSLATION_KEY
					))
					.orElse(mappingContext.translationKeyPrefix());

			List<MappedEntry<Object>> mappedEntries = compoundEntry.subEntries().entrySet().stream()
					.map(mapEntry -> {
						String subTranslationKeyPrefix = translationKey + "." + mapEntry.getKey();
						TweedCoatEntryMappingContext subMappingContext = mappingContext.subContextBuilder(mapEntry.getKey())
								.translationKeyPrefix(subTranslationKeyPrefix)
								.parentWidgetClass(ConfigCategoryWidget.class)
								.build();
						return new MappedEntry<>(
								mapEntry.getKey(),
								subTranslationKeyPrefix,
								(ConfigEntry<Object>) mapEntry.getValue(),
								subMappingContext,
								(TweedCoatEntryMappingResult<@NonNull Object, ?>) subMappingContext.mapEntry(
										mapEntry.getValue(),
										subMappingContext
								)
						);
					})
					.filter(mappedEntry -> mappedEntry.mappingResult.isApplicable())
					.collect(Collectors.toList());

			return new TweedCoatEntryMappingResult<T, T>() {
				@Override
				public boolean isApplicable() {
					return true;
				}

				@Override
				public @Nullable ConfigInput<T> createInput(TweedCoatEntryCreationContext<T> context) {
					return null;
				}

				@Override
				public @Nullable ConfigEntryHandler<T> createHandler(TweedCoatEntryCreationContext<T> context) {
					return null;
				}

				@Override
				public ConfigContentWidget createContentWidget(TweedCoatEntryCreationContext<T> context) {
					ConfigCategoryWidget categoryWidget = new ConfigCategoryWidget(
							Minecraft.getInstance(),
							translatableComponentWithFallback(
									translationKey,
									mappingContext.entryName()
							),
							Collections.emptyList(),
							backgroundTexture
					);
					for (MappedEntry<Object> mappedEntry : mappedEntries) {
						TweedCoatEntryMappingResult<Object, ?> mappingResult = mappedEntry.mappingResult();
						if (!mappingResult.isApplicable()) {
							log.warn(
									"Failed to resolve mapping for entry \"" + mappedEntry.name() + "\" at \""
											+ translationKey + "\". Entry will be ignored in UI."
							);
							continue;
						}

						Object subEntryValue = compoundEntry.get(context.currentValue(), mappedEntry.name());
						Object subEntryDefaultValue = compoundEntry.get(context.defaultValue(), mappedEntry.name());
						TweedCoatEntryCreationContext<Object> creationContext = TweedCoatEntryCreationContext.builder()
								.entry(mappedEntry.entry())
								.currentValue(subEntryValue)
								.defaultValue(subEntryDefaultValue)
								.parentSaveHandler(value -> compoundEntry.set(context.currentValue(), mappedEntry.name(), value))
								.build();

						ConfigInput<?> input = mappingResult.createInput(creationContext);
						if (input != null) {
							ConfigCategoryConfigEntry<Object> entry = new ConfigCategoryConfigEntry<>(
									translatableComponentWithFallback(mappedEntry.translationKeyPrefix(), mappedEntry.name()),
									translatableComponentWithFallback(mappedEntry.translationKeyPrefix() + ".description", null),
									(ConfigEntryHandler<Object>) mappingResult
											.createHandler(creationContext),
									(ConfigInput<Object>) input
							);
							categoryWidget.addEntry(entry);
							continue;
						}

						ConfigContentWidget contentWidget = mappingResult.createContentWidget(creationContext);
						if (contentWidget != null) {
							categoryWidget.addSubTree(contentWidget);
						}
					}
					return categoryWidget;
				}
			};
		}
	}

	private static boolean anyClassMatches(Class<?> valueClass, Class<?>... classes) {
		for (Class<?> clazz : classes) {
			if (clazz.isAssignableFrom(valueClass)) {
				return true;
			}
		}
		return false;
	}
}
