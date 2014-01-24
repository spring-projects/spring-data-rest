/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.lang.reflect.Modifier;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link CollectionResourceMapping} based on a type. Will derive default relation types and pathes from the type but
 * inspect it for {@link RestResource} annotations for customization.
 * 
 * @author Oliver Gierke
 */
class TypeBasedCollectionResourceMapping implements CollectionResourceMapping {

	private final Class<?> type;
	private final RelProvider relProvider;
	private final RestResource annotation;
	private final Description description;

	/**
	 * Creates a new {@link TypeBasedCollectionResourceMapping} using the given type.
	 * 
	 * @param type must not be {@literal null}.
	 */
	public TypeBasedCollectionResourceMapping(Class<?> type) {
		this(type, new EvoInflectorRelProvider());
	}

	/**
	 * Creates a new {@link TypeBasedCollectionResourceMapping} using the given type and {@link RelProvider}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public TypeBasedCollectionResourceMapping(Class<?> type, RelProvider relProvider) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(relProvider, "RelProvider must not be null!");

		this.type = type;
		this.relProvider = relProvider;
		this.annotation = AnnotationUtils.findAnnotation(type, RestResource.class);
		this.description = AnnotationUtils.findAnnotation(type, Description.class);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {

		String path = annotation == null ? null : annotation.path().trim();
		path = StringUtils.hasText(path) ? path : getDefaultPathFor(type);
		return new Path(path);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isExported()
	 */
	@Override
	public boolean isExported() {
		return annotation == null ? Modifier.isPublic(type.getModifiers()) : annotation.exported();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {

		if (annotation == null || !StringUtils.hasText(annotation.rel())) {
			return relProvider.getCollectionResourceRelFor(type);
		}

		return annotation.rel();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getSingleResourceRel()
	 */
	@Override
	public String getItemResourceRel() {
		return relProvider.getItemResourceRelFor(type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#isPagingResource()
	 */
	@Override
	public boolean isPagingResource() {
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getDescription()
	 */
	@Override
	public ResourceDescription getDescription() {

		ResourceDescription fallback = SimpleResourceDescription.defaultFor(getRel());

		return fallback;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getItemResourceDescription()
	 */
	@Override
	public ResourceDescription getItemResourceDescription() {

		ResourceDescription fallback = SimpleResourceDescription.defaultFor(getItemResourceRel());

		if (annotation != null && StringUtils.hasText(annotation.description().value())) {
			return new AnnotationBasedResourceDescription(annotation.description(), fallback);
		}

		if (description != null) {
			return new AnnotationBasedResourceDescription(description, fallback);
		}

		return fallback;
	}

	/**
	 * Returns the default path to be used if the path is not configured manually.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	protected String getDefaultPathFor(Class<?> type) {
		return getSimpleTypeName(type);
	}

	private String getSimpleTypeName(Class<?> type) {
		return StringUtils.uncapitalize(type.getSimpleName());
	}
}
