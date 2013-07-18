/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.Assert;

/**
 * A {@link ConditionalGenericConverter} that can convert a {@link URI} domain entity.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class UriDomainClassConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);

	private final Repositories repositories;
	private final DomainClassConverter<?> domainClassConverter;
	private final Set<ConvertiblePair> convertiblePairs;

	/**
	 * Creates a new {@link UriDomainClassConverter} using the given {@link Repositories} and {@link DomainClassConverter}
	 * .
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param domainClassConverter must not be {@literal null}.
	 */
	public UriDomainClassConverter(Repositories repositories, DomainClassConverter<?> domainClassConverter) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(domainClassConverter, "DomainClassConverter must not be null!");

		this.repositories = repositories;
		this.domainClassConverter = domainClassConverter;
		this.convertiblePairs = new HashSet<ConvertiblePair>();

		for (Class<?> domainType : repositories) {
			convertiblePairs.add(new ConvertiblePair(URI.class, domainType));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

		return URI.class.isAssignableFrom(sourceType.getType())
				&& repositories.getPersistentEntity(targetType.getType()) != null;
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

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(targetType.getType());

		if (entity == null || !domainClassConverter.matches(STRING_TYPE, targetType)) {
			throw new ConversionFailedException(sourceType, targetType, source, new IllegalArgumentException(
					"No PersistentEntity information available for " + targetType.getType()));
		}

		URI uri = (URI) source;
		String[] parts = uri.getPath().split("/");

		if (parts.length < 2) {
			throw new ConversionFailedException(sourceType, targetType, source, new IllegalArgumentException(
					"Cannot resolve URI " + uri + ". Is it local or remote? Only local URIs are resolvable."));
		}

		return domainClassConverter.convert(parts[parts.length - 1], STRING_TYPE, targetType);
	}

}
