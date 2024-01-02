/*
 * Copyright 2018-2024 the original author or authors.
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
package org.springframework.data.rest.core;

import static org.assertj.core.api.Assertions.*;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link StringToLdapNameConverter}.
 *
 * @author Mark Paluch
 */
class StringToLdapNameConverterUnitTests {

	@Test // DATAREST-1198
	void shouldCreateLdapName() throws InvalidNameException {

		LdapName converted = StringToLdapNameConverter.INSTANCE.convert("dc=foo");

		assertThat(converted).isEqualTo(new LdapName("dc=foo"));
	}

	@Test // DATAREST-1198
	void failedConversionShouldThrowIAE() {

		assertThatThrownBy(() -> StringToLdapNameConverter.INSTANCE.convert("foo"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAREST-1198
	void shouldConvertStringInNameViaConversionService() {

		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(StringToLdapNameConverter.INSTANCE);

		Name converted = conversionService.convert("dc=foo", Name.class);

		assertThat(converted).isInstanceOf(LdapName.class);
	}
}
