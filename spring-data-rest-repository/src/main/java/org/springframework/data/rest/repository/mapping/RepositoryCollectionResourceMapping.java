/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.repository.mapping;

import java.lang.reflect.Modifier;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.data.rest.repository.support.RepositoriesUtils;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link CollectionResourceMapping} to be built from repository interfaces. Will inspect {@link RestResource}
 * annotations on the repository interface but fall back to the mapping information of the managed domain type for
 * defaults.
 * 
 * @author Oliver Gierke
 */
class RepositoryCollectionResourceMapping implements CollectionResourceMapping {

	private final RestResource annotation;
	private final CollectionResourceMapping domainTypeMapping;
	private final boolean repositoryIsExportCandidate;

	public RepositoryCollectionResourceMapping(Class<?> repositoryType) {
		this(repositoryType, new EvoInflectorRelProvider());
	}

	/**
	 * Creates a new {@link RepositoryCollectionResourceMapping} for the given repository using the given
	 * {@link RelProvider}.
	 * 
	 * @param repositoryType must not be {@literal null}.
	 * @param relProvider must not be {@literal null}.
	 */
	public RepositoryCollectionResourceMapping(Class<?> repositoryType, RelProvider relProvider) {

		Assert.isTrue(RepositoriesUtils.isRepositoryInterface(repositoryType), "Given type is not a repository!");
		Assert.notNull(relProvider, "RelProvider must not be null!");

		this.annotation = AnnotationUtils.findAnnotation(repositoryType, RestResource.class);
		this.domainTypeMapping = new TypeBasedCollectionResourceMapping(RepositoriesUtils.getDomainType(repositoryType),
				relProvider);
		this.repositoryIsExportCandidate = Modifier.isPublic(repositoryType.getModifiers());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {

		return annotation == null || !StringUtils.hasText(annotation.path()) ? domainTypeMapping.getPath() : new Path(
				annotation.path());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {
		return annotation == null || !StringUtils.hasText(annotation.rel()) ? domainTypeMapping.getRel() : annotation.rel();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getSingleResourceRel()
	 */
	@Override
	public String getSingleResourceRel() {
		return domainTypeMapping.getSingleResourceRel();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#isExported()
	 */
	@Override
	public Boolean isExported() {
		return annotation == null ? repositoryIsExportCandidate && domainTypeMapping.isExported() : annotation.exported();
	}
}
