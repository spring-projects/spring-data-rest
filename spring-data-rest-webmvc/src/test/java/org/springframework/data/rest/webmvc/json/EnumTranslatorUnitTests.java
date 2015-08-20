/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.json;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticMessageSource;

/**
 * Unit tests for {@link EnumTranslator}.
 * 
 * @author Oliver Gierke
 */
public class EnumTranslatorUnitTests {

	StaticMessageSource messageSource;
	EnumTranslator configuration;

	@Before
	public void setUp() {

		LocaleContextHolder.setLocale(Locale.US);

		this.messageSource = new StaticMessageSource();
		this.messageSource.addMessage(MyEnum.class.getName().concat(".").concat(MyEnum.FIRST_VALUE.name()), Locale.US,
				"Translated");
		this.configuration = new EnumTranslator(new MessageSourceAccessor(messageSource));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMessageSourceAccessor() {
		new EnumTranslator(null);
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesNullForNullSource() {
		assertThat(configuration.fromText(MyEnum.class, null), is(nullValue()));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesNullForEmptySource() {
		assertThat(configuration.fromText(MyEnum.class, null), is(nullValue()));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesNullForUnknownValue() {
		assertThat(configuration.fromText(MyEnum.class, "Foobar"), is(nullValue()));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void returnsEnumNameIfDefaultTranslationIsDisabled() {

		configuration.setEnableDefaultTranslation(false);

		assertThat(configuration.asText(MyEnum.SECOND_VALUE), is(MyEnum.SECOND_VALUE.name()));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void returnsDefaultTranslationByDefault() {

		assertThat(configuration.asText(MyEnum.SECOND_VALUE), is("Second value"));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesEnumNameIfDefaultTranslationIsDisabled() {

		configuration.setEnableDefaultTranslation(false);

		assertThat(configuration.fromText(MyEnum.class, "FIRST_VALUE"), is(MyEnum.FIRST_VALUE));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesStandardTranslationAndEnumNameByDefault() {

		assertThat(configuration.fromText(MyEnum.class, "FIRST_VALUE"), is(MyEnum.FIRST_VALUE));
		assertThat(configuration.fromText(MyEnum.class, "Second value"), is(MyEnum.SECOND_VALUE));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void translatesEnumName() {

		LocaleContextHolder.setLocale(Locale.US);

		messageSource.addMessage(MyEnum.class.getName().concat(".").concat(MyEnum.FIRST_VALUE.name()), Locale.US,
				"Translated");

		assertThat(configuration.asText(MyEnum.FIRST_VALUE), is("Translated"));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesEnumNameByDefaultEvenIfMessageDefined() {

		// Parses resolved message and enum name
		assertThat(configuration.fromText(MyEnum.class, "Translated"), is(MyEnum.FIRST_VALUE));
		assertThat(configuration.fromText(MyEnum.class, "FIRST_VALUE"), is(MyEnum.FIRST_VALUE));

		// Does not parse default translation as explicit translation is available
		assertThat(configuration.fromText(MyEnum.class, "First value"), is(nullValue()));

		// Parses default translation as no explicit translation is available
		assertThat(configuration.fromText(MyEnum.class, "Second value"), is(MyEnum.SECOND_VALUE));
		assertThat(configuration.fromText(MyEnum.class, "SECOND_VALUE"), is(MyEnum.SECOND_VALUE));
	}

	/**
	 * @see DATAREST-654
	 */
	@Test
	public void parsesEnumWithDefaultTranslationDisabled() {

		configuration.setEnableDefaultTranslation(false);

		// Parses default translation as no explicit translation is available
		assertThat(configuration.fromText(MyEnum.class, "Second value"), is(nullValue()));
		assertThat(configuration.fromText(MyEnum.class, "SECOND_VALUE"), is(MyEnum.SECOND_VALUE));
	}

	@Test
	public void doesNotResolveEnumNameAsFallbackIfConfigured() {

		configuration.setParseEnumNameAsFallback(false);

		// Parses resolved message and enum name
		assertThat(configuration.fromText(MyEnum.class, "Translated"), is(MyEnum.FIRST_VALUE));
		assertThat(configuration.fromText(MyEnum.class, "FIRST_VALUE"), is(nullValue()));

		// Parses default translation as no explicit translation is available
		assertThat(configuration.fromText(MyEnum.class, "Second value"), is(MyEnum.SECOND_VALUE));
		assertThat(configuration.fromText(MyEnum.class, "SECOND_VALUE"), is(nullValue()));
	}

	static enum MyEnum {
		FIRST_VALUE, SECOND_VALUE;
	}
}
