/*
 * Copyright 2012-2018 the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * A {@link GenericConverter} that can convert a {@link URI} into an entity.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class UriToEntityConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);

	private final PersistentEntities entities;
	private final RepositoryInvokerFactory invokerFactory;
	private final Repositories repositories;
	private final Set<ConvertiblePair> convertiblePairs;

	/**
	 * Creates a new {@link UriToEntityConverter} using the given {@link PersistentEntities},
	 * {@link RepositoryInvokerFactory} and {@link Repositories}.
	 *
	 * @param entities must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 */
	public UriToEntityConverter(PersistentEntities entities, RepositoryInvokerFactory invokerFactory,
			Repositories repositories) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		Assert.notNull(invokerFactory, "RepositoryInvokerFactory must not be null!");
		Assert.notNull(repositories, "Repositories must not be null!");

		Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();

		for (TypeInformation<?> domainType : entities.getManagedTypes()) {

			Class<?> rawType = domainType.getType();
			Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> entity = entities.getPersistentEntity(rawType);

			if (entity.map(it -> it.hasIdProperty()).orElse(false)) {
				convertiblePairs.add(new ConvertiblePair(URI.class, domainType.getType()));
			}
		}

		this.convertiblePairs = Collections.unmodifiableSet(convertiblePairs);
		this.entities = entities;
		this.invokerFactory = invokerFactory;
		this.repositories = repositories;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return !sourceType.equals(URI_TYPE) ? false
				: repositories.getRepositoryInformationFor(targetType.getType()).isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return convertiblePairs;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> entity = entities
				.getPersistentEntity(targetType.getType());

		if (!entity.isPresent()) {
			throw new ConversionFailedException(sourceType, targetType, source,
					new IllegalArgumentException("No PersistentEntity information available for " + targetType.getType()));
		}

		URI uri = (URI) source;
		String[] parts = uri.getPath().split("/");

		if (parts.length < 2) {
			throw new ConversionFailedException(sourceType, targetType, source, new IllegalArgumentException(
					"Cannot resolve URI " + uri + ". Is it local or remote? Only local URIs are resolvable."));
		}

		return invokerFactory.getInvokerFor(targetType.getType()).invokeFindById(parts[parts.length - 1]).orElse(null);
	}
}
