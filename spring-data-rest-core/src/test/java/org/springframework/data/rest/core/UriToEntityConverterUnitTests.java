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
package org.springframework.data.rest.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Unit tests for {@link UriToEntityConverter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class UriToEntityConverterUnitTests {

	static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);
	static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	static final TypeDescriptor ENTITY_TYPE = TypeDescriptor.valueOf(Entity.class);

	@Mock ConversionService conversionService;

	MongoMappingContext context;
	UriToEntityConverter converter;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		this.context = new MongoMappingContext();
		this.context.setInitialEntitySet(new HashSet<Class<?>>(Arrays.asList(Entity.class, NonEntity.class)));
		this.context.afterPropertiesSet();

		this.converter = new UriToEntityConverter(new PersistentEntities(Arrays.asList(this.context)), conversionService);
	}

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void supportsOnlyEntitiesWithIdProperty() {

		Set<ConvertiblePair> result = converter.getConvertibleTypes();

		assertThat(result, hasItem(new ConvertiblePair(URI.class, Entity.class)));
		assertThat(result, not(hasItem(new ConvertiblePair(URI.class, NonEntity.class))));
	}

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void cannotConvertEntityWithIdPropertyIfStringConversionMissing() {
		assertThat(converter.matches(URI_TYPE, ENTITY_TYPE), is(false));
	}

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void canConvertEntityWithIdPropertyAndFromStringConversionPossible() {

		when(conversionService.canConvert(STRING_TYPE, ENTITY_TYPE)).thenReturn(true);

		assertThat(converter.matches(URI_TYPE, ENTITY_TYPE), is(true));
	}

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void cannotConvertEntityWithoutIdentifier() {
		assertThat(converter.matches(URI_TYPE, TypeDescriptor.valueOf(NonEntity.class)), is(false));
	}

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void invokesConverterWithLastUriPathSegment() {

		Entity reference = new Entity();
		when(conversionService.convert("1", Entity.class)).thenReturn(reference);

		assertThat(converter.convert(URI.create("/foo/bar/1"), URI_TYPE, ENTITY_TYPE), is((Object) reference));
	}

	/**
	 * @see DATAREST-427
	 */
	@Test(expected = ConversionFailedException.class)
	public void rejectsUnknownType() {
		converter.convert(URI.create("/foo/1"), URI_TYPE, STRING_TYPE);
	}

	/**
	 * @see DATAREST-427
	 */
	@Test(expected = ConversionFailedException.class)
	public void rejectsUriWithLessThanTwoSegments() {
		converter.convert(URI.create("1"), URI_TYPE, ENTITY_TYPE);
	}

	static class Entity {
		String id;
	}

	static class NonEntity {
		String value;
	}
}
