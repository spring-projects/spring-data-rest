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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.support.RepositoriesUtils;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
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

	private final RestResource annotation;
	private final RepositoryRestResource repositoryAnnotation;
	private final CollectionResourceMapping domainTypeMapping;
	private final boolean repositoryIsExportCandidate;
	private final RepositoryMetadata metadata;

	public RepositoryCollectionResourceMapping(RepositoryMetadata metadata) {
		this(metadata, new EvoInflectorRelProvider());
	}

	/**
	 * Creates a new {@link RepositoryCollectionResourceMapping} for the given repository using the given
	 * {@link RelProvider}.
	 * 
	 * @param repositoryType must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public RepositoryCollectionResourceMapping(RepositoryMetadata metadata, RelProvider relProvider) {

		Assert.notNull(metadata, "Repository metadata must not be null!");
		this.metadata = metadata;

		Class<?> repositoryType = metadata.getRepositoryInterface();

		Assert.isTrue(RepositoriesUtils.isRepositoryInterface(repositoryType), "Given type is not a repository!");
		Assert.notNull(relProvider, "RelProvider must not be null!");

		this.annotation = AnnotationUtils.findAnnotation(repositoryType, RestResource.class);
		this.repositoryAnnotation = AnnotationUtils.findAnnotation(repositoryType, RepositoryRestResource.class);
		this.repositoryIsExportCandidate = Modifier.isPublic(repositoryType.getModifiers());

		Class<?> domainType = RepositoriesUtils.getDomainType(repositoryType);
		this.domainTypeMapping = EVO_INFLECTOR_IS_PRESENT ? new EvoInflectorTypeBasedCollectionResourceMapping(domainType,
				relProvider) : new TypeBasedCollectionResourceMapping(domainType, relProvider);

		if (annotation != null) {
			LOGGER.warn(
					"@RestResource detected to customize the repository resource for {}! Use @RepositoryRestResource instead!",
					metadata.getRepositoryInterface().getName());
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {

		Path fallback = domainTypeMapping.getPath();

		if (repositoryAnnotation != null) {
			String path = repositoryAnnotation.path();
			return StringUtils.hasText(path) ? new Path(path) : fallback;
		}

		if (annotation != null) {
			String path = annotation.path();
			return StringUtils.hasText(path) ? new Path(path) : fallback;
		}

		return fallback;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {

		String fallback = domainTypeMapping.getRel();

		if (repositoryAnnotation != null) {
			String rel = repositoryAnnotation.collectionResourceRel();
			return StringUtils.hasText(rel) ? rel : fallback;
		}

		if (annotation != null) {
			String rel = annotation.rel();
			return StringUtils.hasText(rel) ? rel : fallback;
		}

		return fallback;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getSingleResourceRel()
	 */
	@Override
	public String getItemResourceRel() {

		String fallback = domainTypeMapping.getItemResourceRel();

		if (repositoryAnnotation != null) {
			String rel = repositoryAnnotation.itemResourceRel();
			return StringUtils.hasText(rel) ? rel : fallback;
		}

		return fallback;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isExported()
	 */
	@Override
	public boolean isExported() {

		if (repositoryAnnotation != null) {
			return repositoryAnnotation.exported();
		}

		if (annotation != null) {
			return annotation.exported();
		}

		return repositoryIsExportCandidate && domainTypeMapping.isExported();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#isPagingResource()
	 */
	@Override
	public boolean isPagingResource() {
		return metadata.isPagingRepository();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getDescription()
	 */
	@Override
	public ResourceDescription getDescription() {

		ResourceDescription fallback = SimpleResourceDescription.defaultFor(getRel());

		if (repositoryAnnotation != null) {
			return new AnnotationBasedResourceDescription(repositoryAnnotation.collectionResourceDescription(), fallback);
		}

		return fallback;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getItemResourceDescription()
	 */
	@Override
	public ResourceDescription getItemResourceDescription() {

		ResourceDescription fallback = SimpleResourceDescription.defaultFor(getItemResourceRel());

		if (repositoryAnnotation != null) {
			return new AnnotationBasedResourceDescription(repositoryAnnotation.itemResourceDescription(), fallback);
		}

		return fallback;
	}
}
