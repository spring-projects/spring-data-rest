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
package org.springframework.data.rest.core.mapping;

import java.util.Optional;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.util.Optionals;
import org.springframework.hateoas.LinkRelation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Special resource mapping for {@link PersistentProperty} instances.
 *
 * @author Oliver Gierke
 */
class PersistentPropertyResourceMapping implements PropertyAwareResourceMapping {

	private final PersistentProperty<?> property;
	private final ResourceMappings mappings;
	private final Optional<RestResource> annotation;
	private final Optional<Description> description;

	/**
	 * Creates a new {@link RootPropertyResourceMapping}.
	 *
	 * @param property must not be {@literal null}.
	 * @param exported whether the property is exported or not.
	 */
	public PersistentPropertyResourceMapping(PersistentProperty<?> property, ResourceMappings mappings) {

		Assert.notNull(property, "PersistentProperty must not be null");

		this.property = property;
		this.mappings = mappings;
		this.annotation = Optional
				.ofNullable(property.isAssociation() ? property.findAnnotation(RestResource.class) : null);
		this.description = Optional.ofNullable(property.findAnnotation(Description.class));
	}

	@Override
	public Path getPath() {

		return annotation.filter(it -> StringUtils.hasText(it.path()))//
				.map(it -> new Path(it.path()))//
				.orElseGet(() -> new Path(property.getName()));
	}

	@Override
	public LinkRelation getRel() {

		return LinkRelation.of(annotation.filter(it -> StringUtils.hasText(it.rel())) //
				.map(it -> it.rel()) //
				.orElseGet(() -> property.getName()));
	}

	@Override
	public boolean isExported() {

		if (!property.isAssociation()) {
			return false;
		}

		ResourceMapping typeMapping = mappings.getMetadataFor(property.getAssociationTargetType());

		return typeMapping != null && typeMapping.isExported()
				? annotation.map(it -> it.exported()).orElse(true)
				: false;
	}

	@Override
	public boolean isPagingResource() {
		return false;
	}

	@Override
	public ResourceDescription getDescription() {

		CollectionResourceMapping ownerTypeMapping = mappings.getMetadataFor(property.getOwner().getType());
		ResourceDescription fallback = TypedResourceDescription.defaultFor(ownerTypeMapping.getItemResourceRel(), property);

		return Optionals.<ResourceDescription> firstNonEmpty(//
				() -> description.map(it -> new AnnotationBasedResourceDescription(it, fallback)), //
				() -> annotation.map(it -> new AnnotationBasedResourceDescription(it.description(), fallback)))
				.orElse(fallback);
	}

	@Override
	public PersistentProperty<?> getProperty() {
		return property;
	}
}
