/*
 * Copyright 2013-2025 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Optionals;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link CollectionResourceMapping} to be built from repository interfaces. Will inspect {@link RestResource}
 * annotations on the repository interface but fall back to the mapping information of the managed domain type for
 * defaults.
 *
 * @author Oliver Gierke
 */
class RepositoryCollectionResourceMapping implements CollectionResourceMapping {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCollectionResourceMapping.class);
	private static final boolean EVO_INFLECTOR_IS_PRESENT = ClassUtils.isPresent("org.atteo.evo.inflector.English", null);

	private final boolean repositoryExported;
	private final RepositoryMetadata metadata;
	private final Path path;

	private final Lazy<LinkRelation> rel, itemResourceRel;
	private final Lazy<ResourceDescription> description, itemDescription;
	private final Lazy<Class<?>> excerptProjection;

	public RepositoryCollectionResourceMapping(RepositoryMetadata metadata, RepositoryDetectionStrategy strategy) {
		this(metadata, strategy, new EvoInflectorLinkRelationProvider());
	}

	/**
	 * Creates a new {@link RepositoryCollectionResourceMapping} for the given repository using the given
	 * {@link RelProvider}.
	 *
	 * @param strategy must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 * @param repositoryType must not be {@literal null}.
	 */
	RepositoryCollectionResourceMapping(RepositoryMetadata metadata, RepositoryDetectionStrategy strategy,
			LinkRelationProvider relProvider) {

		Assert.notNull(metadata, "Repository metadata must not be null");
		Assert.notNull(relProvider, "LinkRelationProvider must not be null");
		Assert.notNull(strategy, "RepositoryDetectionStrategy must not be null");

		Class<?> repositoryType = metadata.getRepositoryInterface();

		Optional<RestResource> annotation = Optional
				.ofNullable(AnnotationUtils.findAnnotation(repositoryType, RestResource.class));
		Optional<RepositoryRestResource> repositoryAnnotation = Optional
				.ofNullable(AnnotationUtils.findAnnotation(repositoryType, RepositoryRestResource.class));

		this.metadata = metadata;
		this.repositoryExported = strategy.isExported(metadata);

		Class<?> domainType = metadata.getDomainType();

		CollectionResourceMapping domainTypeMapping = EVO_INFLECTOR_IS_PRESENT
				? new EvoInflectorTypeBasedCollectionResourceMapping(domainType, relProvider)
				: new TypeBasedCollectionResourceMapping(domainType, relProvider);

		this.rel = Lazy.of(() -> Optionals.firstNonEmpty(//
				() -> toLinkRelation(repositoryAnnotation.map(RepositoryRestResource::collectionResourceRel)), //
				() -> toLinkRelation(annotation.map(RestResource::rel))) //
				.orElseGet(domainTypeMapping::getRel));

		this.itemResourceRel = Lazy
				.of(() -> toLinkRelation(repositoryAnnotation.map(RepositoryRestResource::itemResourceRel)) //
						.orElseGet(domainTypeMapping::getItemResourceRel));

		this.path = Optionals.firstNonEmpty(//
				() -> repositoryAnnotation.map(RepositoryRestResource::path), //
				() -> annotation.map(RestResource::path)) //
				.filter(StringUtils::hasText) //
				.filter(it -> {

					if (it.contains("/")) {
						throw new IllegalStateException(
								String.format("Path %s configured for %s must only contain a single path segment", it,
										metadata.getRepositoryInterface().getName()));
					}

					return true;
				})
				// .peek(it -> it.wait()) //
				.map(Path::new)//
				.orElseGet(domainTypeMapping::getPath);

		this.description = Lazy.of(() -> {

			ResourceDescription fallback = SimpleResourceDescription.defaultFor(getRel());

			return repositoryAnnotation.map(RepositoryRestResource::collectionResourceDescription) //
					.<ResourceDescription> map(it -> new AnnotationBasedResourceDescription(it, fallback)) //
					.orElse(fallback);
		});

		this.itemDescription = Lazy.of(() -> {

			ResourceDescription fallback = SimpleResourceDescription.defaultFor(getItemResourceRel());

			return repositoryAnnotation.map(RepositoryRestResource::itemResourceDescription) //
					.<ResourceDescription> map(it -> new AnnotationBasedResourceDescription(it, fallback)) //
					.orElse(fallback);
		});

		this.excerptProjection = Lazy.of(() -> repositoryAnnotation//
				.map(RepositoryRestResource::excerptProjection)//
				.filter(it -> !it.equals(RepositoryRestResource.None.class)) //
				.orElse(null));

		if (annotation.isPresent()) {
			LOGGER.warn(
					"@RestResource detected to customize the repository resource for {}; Use @RepositoryRestResource instead",
					metadata.getRepositoryInterface().getName());
		}
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public LinkRelation getRel() {
		return rel.get();
	}

	@Override
	public LinkRelation getItemResourceRel() {
		return itemResourceRel.get();
	}

	@Override
	public boolean isExported() {
		return repositoryExported;
	}

	@Override
	public boolean isPagingResource() {
		return metadata.isPagingRepository();
	}

	@Override
	public ResourceDescription getDescription() {
		return description.get();
	}

	@Override
	public ResourceDescription getItemResourceDescription() {
		return itemDescription.get();
	}

	@Override
	public Optional<Class<?>> getExcerptProjection() {
		return excerptProjection.getOptional();
	}

	private static Optional<LinkRelation> toLinkRelation(Optional<String> source) {

		return source //
				.filter(StringUtils::hasText) //
				.map(LinkRelation::of);
	}
}
