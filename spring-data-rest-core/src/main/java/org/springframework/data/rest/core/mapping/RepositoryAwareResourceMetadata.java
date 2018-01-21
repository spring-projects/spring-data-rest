/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.util.Assert;

/**
 * {@link ResourceMetadata} for a single repository.
 *
 * @author Oliver Gierke
 */
class RepositoryAwareResourceMetadata implements ResourceMetadata {

	private final CollectionResourceMapping mapping;
	private final PersistentEntitiesResourceMappings provider;
	private final RepositoryMetadata repositoryMetadata;
	private final SupportedHttpMethods crudMethodsSupportedHttpMethods;

	private MappingResourceMetadata mappingMetadata;

	/**
	 * Creates a new {@link RepositoryAwareResourceMetadata} for the given {@link CollectionResourceMapping},
	 * {@link ResourceMappings} and {@link RepositoryMetadata}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param mapping must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param repositoryMetadata must not be {@literal null}.
	 */
	public RepositoryAwareResourceMetadata(PersistentEntity<?, ?> entity, CollectionResourceMapping mapping,
			RepositoryResourceMappings provider, RepositoryMetadata repositoryMetadata) {

		Assert.notNull(entity, "PersistentEntity must not be null!");
		Assert.notNull(mapping, "CollectionResourceMapping must not be null!");
		Assert.notNull(provider, "ResourceMetadataProvider must not be null!");
		Assert.notNull(repositoryMetadata, "RepositoryMetadata must not be null!");

		this.mapping = mapping;
		this.provider = provider;
		this.repositoryMetadata = repositoryMetadata;
		this.crudMethodsSupportedHttpMethods = new CrudMethodsSupportedHttpMethods(repositoryMetadata.getCrudMethods(), provider);
	}

	/**
	 * Returns whether the current {@link RootResourceMetadata} instance for the repository is the primary one to be used.
	 * Reflects to the primary state of the bean definition.
	 *
	 * @return
	 */
	public boolean isPrimary() {
		return AnnotationUtils.findAnnotation(repositoryMetadata.getRepositoryInterface(), Primary.class) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getDomainType()
	 */
	@Override
	public Class<?> getDomainType() {
		return repositoryMetadata.getDomainType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.RootResourceMetadata#getProperty(java.lang.String)
	 */
	@Override
	public PropertyAwareResourceMapping getProperty(String mappedPath) {

		if (this.mappingMetadata == null) {
			this.mappingMetadata = provider.getMappingMetadataFor(getDomainType());
		}

		return mappingMetadata.getProperty(mappedPath);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadataProvider#getMappingFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public ResourceMapping getMappingFor(PersistentProperty<?> property) {
		return provider.getMappingFor(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadataProvider#hasMappingFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public boolean isExported(PersistentProperty<?> property) {
		return provider.isMapped(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#isExported()
	 */
	@Override
	public boolean isExported() {
		return mapping.isExported();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getCollectionRel()
	 */
	@Override
	public String getRel() {
		return mapping.getRel();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getSingleResourceRel()
	 */
	@Override
	public String getItemResourceRel() {
		return mapping.getItemResourceRel();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {
		return mapping.getPath();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#isPagingResource()
	 */
	@Override
	public boolean isPagingResource() {
		return mapping.isPagingResource();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getDescription()
	 */
	@Override
	public ResourceDescription getDescription() {
		return mapping.getDescription();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getItemResourceDescription()
	 */
	@Override
	public ResourceDescription getItemResourceDescription() {
		return mapping.getItemResourceDescription();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.CollectionResourceMapping#getExcerptProjection()
	 */
	@Override
	public Class<?> getExcerptProjection() {
		return mapping.getExcerptProjection();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getSearchResourceMappings()
	 */
	@Override
	public SearchResourceMappings getSearchResourceMappings() {
		return provider.getSearchResourceMappings(repositoryMetadata.getDomainType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMetadata#getSupportedHttpMethods()
	 */
	@Override
	public SupportedHttpMethods getSupportedHttpMethods() {
		return crudMethodsSupportedHttpMethods;
	}
}
