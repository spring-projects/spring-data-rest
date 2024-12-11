/*
 * Copyright 2013-2024 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.util.Optional;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Optionals;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
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
	private final LinkRelationProvider relProvider;
	private final Optional<RestResource> annotation;

	private final Lazy<Path> path;
	private final Lazy<LinkRelation> rel;
	private final Lazy<ResourceDescription> description;
	private final Lazy<ResourceDescription> itemResourceDescription;

	/**
	 * Creates a new {@link TypeBasedCollectionResourceMapping} using the given type.
	 *
	 * @param type must not be {@literal null}.
	 */
	public TypeBasedCollectionResourceMapping(Class<?> type) {
		this(type, new EvoInflectorLinkRelationProvider());
	}

	/**
	 * Creates a new {@link TypeBasedCollectionResourceMapping} using the given type and {@link RelProvider}.
	 *
	 * @param type must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public TypeBasedCollectionResourceMapping(Class<?> type, LinkRelationProvider relProvider) {

		Assert.notNull(type, "Type must not be null");
		Assert.notNull(relProvider, "LinkRelationProvider must not be null");

		this.type = type;
		this.relProvider = relProvider;
		this.annotation = Optional.ofNullable(AnnotationUtils.findAnnotation(type, RestResource.class));

		this.path = Lazy.of(() -> annotation.map(RestResource::path) //
				.map(String::trim) //
				.filter(StringUtils::hasText) //
				.orElseGet(() -> getDefaultPathFor(type)))//
				.map(Path::new);

		this.rel = Lazy.of(() -> annotation //
				.map(RestResource::rel) //
				.filter(StringUtils::hasText) //
				.map(LinkRelation::of) //
				.orElseGet(() -> relProvider.getCollectionResourceRelFor(type)));

		Optional<Description> descriptionAnnotation = Optional
				.ofNullable(AnnotationUtils.findAnnotation(type, Description.class));

		this.description = Lazy.of(() -> {

			ResourceDescription fallback = SimpleResourceDescription.defaultFor(getRel());

			return Optionals.<ResourceDescription> firstNonEmpty(//
					() -> descriptionAnnotation.map(it -> new AnnotationBasedResourceDescription(it, fallback)), //
					() -> annotation.map(RestResource::description)
							.map(it -> new AnnotationBasedResourceDescription(it, fallback))) //
					.orElse(fallback);
		});

		this.itemResourceDescription = Lazy.of(() -> {

			ResourceDescription fallback = SimpleResourceDescription.defaultFor(getItemResourceRel());

			return Optionals.<ResourceDescription> firstNonEmpty(//
					() -> annotation.map(RestResource::description) //
							.filter(it -> StringUtils.hasText(it.value())) //
							.map(it -> new AnnotationBasedResourceDescription(it, fallback)), //
					() -> descriptionAnnotation.map(it -> new AnnotationBasedResourceDescription(it, fallback))) //
					.orElse(fallback);
		});
	}

	@Override
	public Path getPath() {
		return path.get();
	}

	@Override
	public boolean isExported() {

		return annotation //
				.map(RestResource::exported) //
				.orElseGet(() -> Modifier.isPublic(type.getModifiers()));
	}

	@Override
	public LinkRelation getRel() {
		return rel.get();
	}

	@Override
	public LinkRelation getItemResourceRel() {
		return relProvider.getItemResourceRelFor(type);
	}

	@Override
	public boolean isPagingResource() {
		return false;
	}

	@Override
	public ResourceDescription getDescription() {
		return description.get();
	}

	@Override
	public ResourceDescription getItemResourceDescription() {
		return itemResourceDescription.get();
	}

	@Override
	public Optional<Class<?>> getExcerptProjection() {
		return Optional.empty();
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
