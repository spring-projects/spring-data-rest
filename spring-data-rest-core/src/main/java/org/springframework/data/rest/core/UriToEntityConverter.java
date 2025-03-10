/*
 * Copyright 2012-2025 the original author or authors.
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

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link GenericConverter} that can convert a {@link URI} into an entity.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class UriToEntityConverter implements GenericConverter {

	private static final Class<?> ASSOCIATION_TYPE = ReflectionUtils
			.loadIfPresent("org.jmolecules.ddd.types.Association", UriToEntityConverter.class.getClassLoader());

	private final PersistentEntities entities;
	private final RepositoryInvokerFactory invokerFactory;
	private final Supplier<ConversionService> conversionService;

	private final Set<ConvertiblePair> convertiblePairs;
	private final Set<Class<?>> identifierTypes;

	/**
	 * Creates a new {@link UriToEntityConverter} using the given {@link PersistentEntities},
	 * {@link RepositoryInvokerFactory} and {@link Repositories}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public UriToEntityConverter(PersistentEntities entities, RepositoryInvokerFactory invokerFactory,
			Supplier<ConversionService> conversionService) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(invokerFactory, "RepositoryInvokerFactory must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.convertiblePairs = new HashSet<>();
		this.identifierTypes = new HashSet<>();

		for (TypeInformation<?> domainType : entities.getManagedTypes()) {

			var rawType = domainType.getType();
			var entity = entities.getPersistentEntity(rawType);

			entity.filter(it -> it.hasIdProperty()).ifPresent(it -> {
				convertiblePairs.add(new ConvertiblePair(URI.class, domainType.getType()));
				registerIdentifierType(it.getRequiredIdProperty().getType());
			});
		}

		this.entities = entities;
		this.invokerFactory = invokerFactory;
		this.conversionService = conversionService;

		if (ASSOCIATION_TYPE != null) {
			registerIdentifierType(ASSOCIATION_TYPE);
		}
	}

	private void registerIdentifierType(Class<?> type) {

		convertiblePairs.add(new ConvertiblePair(URI.class, type));
		identifierTypes.add(type);
	}

	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return convertiblePairs;
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			return null;
		}

		if (identifierTypes.contains(targetType.getType())) {

			var segment = getIdentifierSegment(source, sourceType, targetType);

			return conversionService.get().convert(segment, TypeDescriptor.valueOf(String.class), targetType);
		}

		var entity = entities.getPersistentEntity(targetType.getType());

		if (entity.isEmpty()) {
			throw new ConversionFailedException(sourceType, targetType, source,
					new IllegalArgumentException(
							"No PersistentEntity information available for " + targetType.getType()));
		}

		var segment = getIdentifierSegment(source, sourceType, targetType);

		return invokerFactory.getInvokerFor(targetType.getType())
				.invokeFindById(segment)
				.orElse(null);
	}

	private static String getIdentifierSegment(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		URI uri = (URI) source;
		String[] parts = uri.getPath().split("/");

		if (parts.length < 2) {
			throw new ConversionFailedException(sourceType, targetType, source, new IllegalArgumentException(
					"Cannot resolve URI " + uri + "; Is it local or remote; Only local URIs are resolvable"));
		}

		return parts[parts.length - 1];
	}
}
