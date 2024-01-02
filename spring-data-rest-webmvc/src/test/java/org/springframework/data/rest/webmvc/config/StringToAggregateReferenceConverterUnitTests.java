/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.UUID;

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.rest.core.AggregateReference;
import org.springframework.data.rest.core.AssociationAggregateReference;

/**
 * Unit tests for {@link StringToAggregateReferenceConverter}.
 *
 * @author Oliver Drotbohm
 */
@ExtendWith(MockitoExtension.class)
class StringToAggregateReferenceConverterUnitTests {

	@Mock ConversionService conversionService;

	StringToAggregateReferenceConverter converter = new StringToAggregateReferenceConverter(() -> conversionService);

	@Test // GH-2239
	void convertsUriIntoAggregateReference() {

		var aggregate = new Object();

		when(conversionService.convert(any(), any(), eq(toTypeDescriptor(Long.class)))).thenReturn(42L);
		when(conversionService.convert(any(), any(), eq(toTypeDescriptor(Object.class)))).thenReturn(aggregate);

		var source = "/foo/42";

		var result = converter.convert(source, TypeDescriptor.valueOf(String.class),
				toTypeDescriptor(AggregateReference.class, Object.class, Long.class));

		assertThat(result.getUri()).isEqualTo(URI.create(source));
		assertThat(result.resolveId()).isEqualTo(42L);
		assertThat(result.resolveAggregate()).isEqualTo(aggregate);
	}

	@Test // GH-2239
	void convertsUriIntoAggregateReferenceUsingCustomExtractor() {

		var aggregate = new Object();

		when(conversionService.convert(any(), any(), eq(toTypeDescriptor(Long.class)))).thenReturn(42L);
		when(conversionService.convert(any(), any(), eq(toTypeDescriptor(Object.class)))).thenReturn(aggregate);

		var source = "/foo/42";

		var result = converter.convert(source, TypeDescriptor.valueOf(String.class),
				toTypeDescriptor(AggregateReference.class, Object.class, Long.class));

		result = result.withIdSource(it -> it.getPathSegments().get(1));

		assertThat(result.getUri()).isEqualTo(URI.create(source));
		assertThat(result.resolveId()).isEqualTo(42L);
		assertThat(result.resolveAggregate()).isEqualTo(aggregate);
	}

	@Test // GH-2239
	void createsAssociationAggregateReference() {

		var identifier = new CustomIdentifier();

		when(conversionService.convert(any(), any(), eq(toTypeDescriptor(CustomIdentifier.class)))).thenReturn(identifier);

		var source = "/foo/42";

		var result = converter.convert(source, TypeDescriptor.valueOf(String.class),
				toTypeDescriptor(AssociationAggregateReference.class, Object.class, CustomIdentifier.class));

		assertThat(result).isInstanceOfSatisfying(AssociationAggregateReference.class, it -> {
			assertThat(it.resolveAssociation()).isNotNull()
					.extracting(Association::getId).isEqualTo(identifier);
		});
	}

	@Test // GH-2239
	void rejectsNullSource() {

		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> converter.convert(null, TypeDescriptor.valueOf(String.class),
						toTypeDescriptor(AggregateReference.class, Object.class, UUID.class)));
	}

	@Test // GH-2239
	void rejectsInvalidURI() {

		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> converter.convert("@\\", TypeDescriptor.valueOf(String.class),
						toTypeDescriptor(AggregateReference.class, Object.class, UUID.class)));
	}

	@Test // GH-2239
	void registersConversions() {

		var service = new DefaultConversionService();
		service.addConverter(converter);

		assertThat(service.canConvert(String.class, AggregateReference.class)).isTrue();
		assertThat(service.canConvert(String.class, AssociationAggregateReference.class)).isTrue();
	}

	private static TypeDescriptor toTypeDescriptor(Class<?> type, Class<?>... generics) {

		var resolvableType = ResolvableType.forClassWithGenerics(type, generics);

		return new TypeDescriptor(resolvableType, null, null);
	}

	private static class CustomIdentifier implements Identifier {}
}
