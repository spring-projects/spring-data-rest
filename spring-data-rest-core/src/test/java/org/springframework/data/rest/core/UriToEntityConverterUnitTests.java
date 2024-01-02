/*
 * Copyright 2015-2024 the original author or authors.
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

import lombok.Value;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverterUnitTests.JMoleculesAggregateRoot.JMoleculesIdentifier;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.support.DefaultFormattingConversionService;

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
	static final TypeDescriptor UUID_ENTITY_TYPE = TypeDescriptor.valueOf(UuidEntity.class);
	static final TypeDescriptor UUID_TYPE = TypeDescriptor.valueOf(UUID.class);
	static final TypeDescriptor UNKNOWN_TYPE = TypeDescriptor.valueOf(Override.class);

	@Mock RepositoryInvokerFactory invokerFactory;

	KeyValueMappingContext<?, ?> context;
	UriToEntityConverter converter;
	ConversionService conversionService;

	@BeforeEach
	void setUp() {

		var conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(new PrimitivesToAssociationConverter(() -> conversionService));

		this.context = new KeyValueMappingContext<>();
		this.context.setInitialEntitySet(Set.of(Entity.class, NonEntity.class, UuidEntity.class));
		this.context.afterPropertiesSet();

		this.converter = new UriToEntityConverter(new PersistentEntities(List.of(this.context)), invokerFactory,
				() -> conversionService);
	}

	@Test // DATAREST-427
	void supportsOnlyEntitiesWithIdProperty() {

		Set<ConvertiblePair> result = converter.getConvertibleTypes();

		assertThat(result).contains(new ConvertiblePair(URI.class, Entity.class));
		assertThat(result).doesNotContain(new ConvertiblePair(URI.class, NonEntity.class));
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
				.isThrownBy(() -> converter.convert(URI.create("/foo/1"), URI_TYPE, UNKNOWN_TYPE));
	}

	@Test // DATAREST-427
	void rejectsUriWithLessThanTwoSegments() {

		assertThatExceptionOfType(ConversionFailedException.class) //
				.isThrownBy(() -> converter.convert(URI.create("1"), URI_TYPE, ENTITY_TYPE));
	}

	@Test // DATAREST-741
	void rejectsNullPersistentEntities() {

		assertThatIllegalArgumentException() //
				.isThrownBy(
						() -> new UriToEntityConverter(null, invokerFactory, () -> conversionService));
	}

	@Test // DATAREST-741
	void rejectsNullRepositoryInvokerFactory() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new UriToEntityConverter(mock(PersistentEntities.class), null, () -> conversionService));
	}

	@Test // DATAREST-741
	void rejectsNullConversionService() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new UriToEntityConverter(mock(PersistentEntities.class), invokerFactory, null));
	}

	/**
	 * @see DATAREST-1018
	 */
	@Test
	void doesNotRegisterTypeWithUnmanagedRawType() {

		PersistentEntities entities = mock(PersistentEntities.class);
		doReturn(Streamable.of(TypeInformation.OBJECT)).when(entities).getManagedTypes();

		new UriToEntityConverter(entities, invokerFactory, () -> conversionService);
	}

	@Test
	void resolvesIdentifierType() {

		var uuid = UUID.randomUUID();

		assertThat(converter.convert(URI.create("/foo/" + uuid), STRING_TYPE, UUID_TYPE)).isEqualTo(uuid);
	}

	@Test
	void resolvesAssociations() {

		var typeDescriptor = new TypeDescriptor(
				ResolvableType.forClassWithGenerics(Association.class, JMoleculesAggregateRoot.class,
						JMoleculesIdentifier.class),
				null, null);

		var uuid = UUID.randomUUID();

		assertThat(converter.convert(URI.create("/foo/" + uuid), URI_TYPE, typeDescriptor))
				.isInstanceOfSatisfying(Association.class, it -> {
					assertThat(it.getId()).isEqualTo(JMoleculesIdentifier.of(uuid));
				});
	}

	static class Entity {
		@Id String id;
	}

	static class UuidEntity {
		@Id UUID id;
	}

	static class NonEntity {
		String value;
	}

	static class JMoleculesAggregateRoot implements AggregateRoot<JMoleculesAggregateRoot, JMoleculesIdentifier> {

		@Override
		public JMoleculesIdentifier getId() {
			return JMoleculesIdentifier.of(UUID.randomUUID());
		}

		@Value(staticConstructor = "of")
		static class JMoleculesIdentifier implements Identifier {
			UUID id;
		}
	}

	private static void someMethod(Association<JMoleculesAggregateRoot, JMoleculesIdentifier> association) {}
}
