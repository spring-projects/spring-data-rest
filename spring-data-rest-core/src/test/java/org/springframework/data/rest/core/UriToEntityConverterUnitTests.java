/*
 * Copyright 2015-2022 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link UriToEntityConverter}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class UriToEntityConverterUnitTests {

	static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);
	static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	static final TypeDescriptor ENTITY_TYPE = TypeDescriptor.valueOf(Entity.class);

	@Mock Repositories repositories;
	@Mock RepositoryInvokerFactory invokerFactory;

	KeyValueMappingContext<?, ?> context;
	UriToEntityConverter converter;

	@BeforeEach
	void setUp() {

		this.context = new KeyValueMappingContext<>();
		this.context.setInitialEntitySet(new HashSet<Class<?>>(Arrays.asList(Entity.class, NonEntity.class)));
		this.context.afterPropertiesSet();

		this.converter = new UriToEntityConverter(new PersistentEntities(Arrays.asList(this.context)), invokerFactory,
				repositories);
	}

	@Test // DATAREST-427
	void supportsOnlyEntitiesWithIdProperty() {

		Set<ConvertiblePair> result = converter.getConvertibleTypes();

		assertThat(result).contains(new ConvertiblePair(URI.class, Entity.class));
		assertThat(result).doesNotContain(new ConvertiblePair(URI.class, NonEntity.class));
	}

	@Test // DATAREST-427
	void cannotConvertEntityWithIdPropertyIfStringConversionMissing() {
		assertThat(converter.matches(URI_TYPE, ENTITY_TYPE)).isFalse();
	}

	@Test // DATAREST-427
	void canConvertEntityWithIdPropertyAndFromStringConversionPossible() {

		doReturn(Optional.of(mock(RepositoryInformation.class))).when(repositories)
				.getRepositoryInformationFor(ENTITY_TYPE.getType());

		assertThat(converter.matches(URI_TYPE, ENTITY_TYPE)).isTrue();
	}

	@Test // DATAREST-427
	void cannotConvertEntityWithoutIdentifier() {
		assertThat(converter.matches(URI_TYPE, TypeDescriptor.valueOf(NonEntity.class))).isFalse();
	}

	@Test // DATAREST-427
	void invokesConverterWithLastUriPathSegment() {

		Entity reference = new Entity();

		RepositoryInvoker invoker = mock(RepositoryInvoker.class);
		doReturn(Optional.of(reference)).when(invoker).invokeFindById("1");
		doReturn(invoker).when(invokerFactory).getInvokerFor(ENTITY_TYPE.getType());

		assertThat(converter.convert(URI.create("/foo/bar/1"), URI_TYPE, ENTITY_TYPE)).isEqualTo(reference);
	}

	@Test // DATAREST-427
	void rejectsUnknownType() {

		assertThatExceptionOfType(ConversionFailedException.class) //
				.isThrownBy(() -> converter.convert(URI.create("/foo/1"), URI_TYPE, STRING_TYPE));
	}

	@Test // DATAREST-427
	void rejectsUriWithLessThanTwoSegments() {

		assertThatExceptionOfType(ConversionFailedException.class) //
				.isThrownBy(() -> converter.convert(URI.create("1"), URI_TYPE, ENTITY_TYPE));
	}

	@Test // DATAREST-741
	void rejectsNullPersistentEntities() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new UriToEntityConverter(null, invokerFactory, repositories));
	}

	@Test // DATAREST-741
	void rejectsNullRepositoryInvokerFactory() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new UriToEntityConverter(mock(PersistentEntities.class), null, repositories));
	}

	@Test // DATAREST-741
	void rejectsNullRepositories() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new UriToEntityConverter(mock(PersistentEntities.class), invokerFactory, null));
	}

	/**
	 * @see DATAREST-1018
	 */
	@Test
	void doesNotRegisterTypeWithUnmanagedRawType() {

		PersistentEntities entities = mock(PersistentEntities.class);
		doReturn(Streamable.of(ClassTypeInformation.OBJECT)).when(entities).getManagedTypes();

		new UriToEntityConverter(entities, invokerFactory, repositories);
	}

	static class Entity {
		@Id String id;
	}

	static class NonEntity {
		String value;
	}
}
