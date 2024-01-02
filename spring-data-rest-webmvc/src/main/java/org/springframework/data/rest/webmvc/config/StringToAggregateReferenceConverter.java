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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.rest.core.AggregateReference;
import org.springframework.data.rest.core.AssociationAggregateReference;
import org.springframework.data.rest.core.ResolvingAggregateReference;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.UriComponents;

/**
 * A {@link GenericConverter} to convert {@link String}s into {@link AggregateReference} instance for the latter to be
 * injectable into Spring MVC controller methods.
 *
 * @author Oliver Drotbohm
 * @since 4.1
 */
class StringToAggregateReferenceConverter implements GenericConverter {

	private static final boolean JMOLECULES_PRESENT = ClassUtils.isPresent(
			"org.jmolecules.spring.IdentifierToPrimitivesConverter",
			StringToAggregateReferenceConverter.class.getClassLoader());
	private static final Class<?> ASSOCIATION_AGGREGATE_REFERENCE_TYPE = tryToLoadAssociationReferenceClass();

	private final Supplier<ConversionService> conversionService;

	/**
	 * Creates a new {@link StringToAggregateReferenceConverter} for the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	StringToAggregateReferenceConverter(Supplier<ConversionService> conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.conversionService = conversionService;
	}

	private static Class<?> tryToLoadAssociationReferenceClass() {

		var classLoader = StringToAggregateReferenceConverter.class.getClassLoader();

		if (!ClassUtils.isPresent("org.jmolecules.ddd.types.Association", classLoader)) {
			return null;
		}

		try {
			return ClassUtils.forName("org.springframework.data.rest.core.AssociationAggregateReference", classLoader);
		} catch (ClassNotFoundException o_O) {
			return null;
		}
	}

	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Set.of(new ConvertiblePair(String.class, AggregateReference.class));
	}

	@NonNull
	@Override
	public AggregateReference<?, ?> convert(@Nullable Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {

		if (source == null) {
			throw new ConversionFailedException(sourceType, targetType, source,
					new IllegalArgumentException("Source value must not be null"));
		}

		try {

			var uri = new URI(source.toString());
			var resolvableType = targetType.getResolvableType();

			var aggregateDescriptor = new TypeDescriptor(resolvableType.getGeneric(0), null, targetType.getAnnotations());
			var identifierDescriptor = new TypeDescriptor(resolvableType.getGeneric(1), null, targetType.getAnnotations());

			Function<Object, Object> aggregateResolver = it -> conversionService.get().convert(it, sourceType,
					aggregateDescriptor);
			Function<Object, Object> identifierResolver = it -> conversionService.get().convert(it, sourceType,
					identifierDescriptor);

			var result = new ResolvingAggregateReference<>(uri, aggregateResolver, identifierResolver);

			return JMOLECULES_PRESENT && resolvableType.toClass().equals(ASSOCIATION_AGGREGATE_REFERENCE_TYPE) //
					? withJMolecules(result) //
					: result;

		} catch (URISyntaxException e) {
			throw new ConversionFailedException(sourceType, targetType, source, e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AggregateReference<?, ?> withJMolecules(AggregateReference<?, ?> source) {
		return new ResolvingAssociationAggregateReference(source);
	}

	/**
	 * An {@link AssociationAggregateReference} delegating to a simple {@link AggregateReference}.
	 *
	 * @author Oliver Drotbohm
	 * @since 4.1
	 */
	private static class ResolvingAssociationAggregateReference<T extends AggregateRoot<T, ID>, ID extends Identifier>
			implements AssociationAggregateReference<T, ID> {

		private AggregateReference<T, ID> delegate;

		ResolvingAssociationAggregateReference(AggregateReference<T, ID> delegate) {
			this.delegate = delegate;
		}

		@Override
		public URI getUri() {
			return delegate.getUri();
		}

		@Override
		public ID resolveId() {
			return delegate.resolveId();
		}

		@Override
		public T resolveAggregate() {
			return delegate.resolveAggregate();
		}

		@Override
		public AssociationAggregateReference<T, ID> withIdSource(Function<UriComponents, Object> extractor) {
			return new ResolvingAssociationAggregateReference<T, ID>(delegate.withIdSource(extractor));
		}
	}
}
