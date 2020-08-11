/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.json;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.util.StreamUtils;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration to tweak enum serialization.
 *
 * @author Oliver Gierke
 */
public class EnumTranslator implements EnumTranslationConfiguration {

	private final MessageResolver resolver;

	private boolean enableDefaultTranslation;
	private boolean parseEnumNameAsFallback;

	/**
	 * Creates a new {@link EnumTranslator} using the given {@link MessageSourceAccessor}.
	 *
	 * @param resolver must not be {@literal null}.
	 */
	public EnumTranslator(MessageResolver resolver) {

		Assert.notNull(resolver, "MessageResolver must not be null!");

		this.resolver = resolver;
		this.enableDefaultTranslation = true;
		this.parseEnumNameAsFallback = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EnumTranslationConfiguration#setEnableDefaultTranslation(boolean)
	 */
	@Override
	public void setEnableDefaultTranslation(boolean enableDefaultTranslation) {
		this.enableDefaultTranslation = enableDefaultTranslation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.config.EnumTranslationConfiguration#setParseEnumNameAsFallback(boolean)
	 */
	@Override
	public void setParseEnumNameAsFallback(boolean parseEnumNameAsFallback) {
		this.parseEnumNameAsFallback = parseEnumNameAsFallback;
	}

	/**
	 * Resolves the given enum value into a {@link String} consulting the configured {@link MessageSourceAccessor}
	 * potentially falling back to the default translation if configured. Returning the plain enum name if no resolution
	 * applies.
	 *
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public String asText(Enum<?> value) {

		Assert.notNull(value, "Enum value must not be null!");

		return resolver.resolve(TranslatedEnum.of(value, enableDefaultTranslation));
	}

	/**
	 * Returns all potentially translated values for the given {@link Enum} type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public List<String> getValues(Class<? extends Enum<?>> type) {

		Assert.notNull(type, "Enum type must not be null!");

		return Arrays.stream(type.getEnumConstants()) //
				.map(this::asText) //
				.collect(StreamUtils.toUnmodifiableList());
	}

	/**
	 * Parses the given source text into the corresponding enum value using the configured {@link MessageSourceAccessor}
	 * potentially falling back to the default translation or the plain enum name if configured.
	 *
	 * @param type must not be {@literal null}.
	 * @param text can be {@literal null}
	 * @return the resolved enum or {@literal null} if the resolution failed.
	 */
	public <T extends Enum<?>> T fromText(Class<T> type, String text) {

		if (!StringUtils.hasText(text)) {
			return null;
		}

		Assert.notNull(type, "Enum type must not be null!");

		T value = resolveEnum(type, text, true);

		if (value != null) {
			return value;
		}

		value = fromDefault(type, text);

		// Only parse default translation if no explicit translation is available
		if (value != null && enableDefaultTranslation && asText(value).equals(text)) {
			return value;
		}

		return parseEnumNameAsFallback ? resolveEnum(type, text, false) : null;
	}

	/**
	 * Resolves the given {@link String} text into an enum value of the given type potentially trying to resolve it
	 * through the configured {@link MessageSourceAccessor}.
	 *
	 * @param type must not be {@literal null}.
	 * @param text must not be {@literal null} or empty.
	 * @param resolve whether to resolve the source {@link String} through the message source.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T extends Enum<?>> T resolveEnum(Class<T> type, String text, boolean resolve) {

		for (Enum<?> value : type.getEnumConstants()) {

			String resolved = resolve ? asText(value) : value.name();

			if (resolved != null && resolved.equals(text)) {
				return (T) value;
			}
		}

		return null;
	}

	/**
	 * Tries to obtain an enum value assuming the given text is a default translation of the enum name.
	 *
	 * @param type must not be {@literal null}.
	 * @param text must not be {@literal null} or empty.
	 * @return
	 */
	private <T extends Enum<?>> T fromDefault(Class<T> type, String text) {
		return resolveEnum(type, text.toUpperCase(Locale.US).replaceAll(" ", "_"), true);
	}

	/**
	 * A {@link MessageSourceResolvable} that will use a key of {@code enumClassName.value} and a capitalized version of
	 * the enum value with the underscores replaced by spaces as default if configured.
	 *
	 * @author Oliver Drotbohm
	 * @since 3.2
	 * @soundtrack Dave Matthews Band - #41 [4.20.02] (Best of What's Around – Encore Vol. 1)
	 */
	private static class TranslatedEnum implements MessageSourceResolvable {

		private final Enum<?> value;
		private final boolean withDefaultTranslation;

		private TranslatedEnum(Enum<?> value, boolean withDefaultTranslation) {

			this.value = value;
			this.withDefaultTranslation = withDefaultTranslation;
		}

		public static TranslatedEnum of(Enum<?> value, boolean withDefaultTranslation) {
			return new TranslatedEnum(value, withDefaultTranslation);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.MessageSourceResolvable#getCodes()
		 */
		@Override
		public String[] getCodes() {
			return new String[] { String.format("%s.%s", value.getDeclaringClass().getName(), value.name()) };
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.MessageSourceResolvable#getDefaultMessage()
		 */
		@Override
		public String getDefaultMessage() {
			return withDefaultTranslation ? toDefault(value) : value.name();
		}

		/**
		 * Renders a default translation for the given enum (capitalized, lower case, underscores replaced by spaces).
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		private static String toDefault(Enum<?> value) {
			return StringUtils.capitalize(value.name().toLowerCase(Locale.US).replaceAll("_", " "));
		}
	}
}
