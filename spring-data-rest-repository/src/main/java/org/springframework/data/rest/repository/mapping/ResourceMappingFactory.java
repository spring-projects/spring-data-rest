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

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.data.rest.repository.support.RepositoriesUtils;
import org.springframework.hateoas.RelProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class ResourceMappingFactory {

	private final RelProvider relProvider;

	public ResourceMappingFactory(RelProvider relProvider) {
		this.relProvider = relProvider;
	}

	public CollectionResourceMapping getMappingForType(Class<?> type) {
		return getMappingForType(type, new CollectionResourceMapping[0]);
	}

	public CollectionResourceMapping getMappingForType(Class<?> type, CollectionResourceMapping... manualMapping) {

		Class<?> typeToInspect = getTypeToInspect(type);

		InternalMappingBuilder mapping = getBaseMetadata(typeToInspect). //
				merge(discoverConfig(typeToInspect));

		if (type != typeToInspect) {
			mapping = mapping.merge(discoverConfig(type));
		}
		
		for (CollectionResourceMapping externalMapping : manualMapping) {
			mapping = mapping.merge(externalMapping);
		}

		return mapping.getMapping();
	}

	private static Class<?> getTypeToInspect(Class<?> type) {
		
		if (!RepositoriesUtils.isRepositoryInterface(type)) {
			return type;
		}

		return RepositoriesUtils.getDomainType(type);
	}

	private InternalMappingBuilder getBaseMetadata(Class<?> domainType) {

		String path = StringUtils.uncapitalize(domainType.getSimpleName());
		String defaultCollectionRel = relProvider.getCollectionResourceRelFor(domainType);
		String defaultSingleRel = relProvider.getSingleResourceRelFor(domainType);
		
		CollectionResourceMapping mapping = new SimpleCollectionResourceMapping(defaultCollectionRel, defaultSingleRel, path, true);

		return new CollectionResourceMappingBuilder(mapping);
	}

	private static final CollectionResourceMapping discoverConfig(Class<?> type) {

		RestResource resource = AnnotationUtils.findAnnotation(type, RestResource.class);

		if (resource == null) {
			return null;
		}
		
		return new AnnotationResourceMapping(resource);
	}
	
	/**
	 * {@link CollectionResourceMapping} based on an {@link RestResource} annotation.
	 *
	 * @author Oliver Gierke
	 */
	private static class AnnotationResourceMapping implements CollectionResourceMapping {
		
		private final RestResource annotation;
		
		/**
		 * Creates a new {@link AnnotationResourceMapping} for the given {@link RestResource}.
		 * 
		 * @param annotation must not be {@literal null}.
		 */
		public AnnotationResourceMapping(RestResource annotation) {
			Assert.notNull(annotation, "Annotation must not be null!");
			this.annotation = annotation;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#isExported()
		 */
		@Override
		public Boolean isExported() {
			return annotation.exported();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getCollectionRel()
		 */
		@Override
		public String getRel() {
			return StringUtils.hasText(annotation.rel()) ? annotation.rel() : null;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getSingleResourceRel()
		 */
		@Override
		public String getSingleResourceRel() {
			return null;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getPath()
		 */
		@Override
		public String getPath() {
			return StringUtils.hasText(annotation.path()) ? annotation.path() : null;
		}
	}
}
