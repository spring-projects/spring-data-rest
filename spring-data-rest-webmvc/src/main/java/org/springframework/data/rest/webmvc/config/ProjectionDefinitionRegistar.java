/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} to make sure all excerpt projections defined in {@link RepositoryResourceMappings} are
 * registered with the {@link RepositoryRestConfiguration}. This rather external configuration has been used to make
 * sure we don't introduce a cyclic dependency between {@link RepositoryRestConfiguration} an
 * {@link RepositoryResourceMappings} as the latter need access to the former to discover mappings in the first place.
 * 
 * @author Oliver Gierke
 * @since 2.5
 * @soundtrack Katinka - Ausverkauf
 */
public class ProjectionDefinitionRegistar extends InstantiationAwareBeanPostProcessorAdapter {

	private final ObjectFactory<RepositoryRestConfiguration> config;

	/**
	 * Creates a new {@link ProjectionDefinitionRegistar} for the given {@link RepositoryRestConfiguration}.
	 * 
	 * @param config must not be {@literal null}.
	 */
	public ProjectionDefinitionRegistar(ObjectFactory<RepositoryRestConfiguration> config) {

		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");

		this.config = config;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if (!(bean instanceof ResourceMappings)) {
			return bean;
		}

		ResourceMappings mappings = (ResourceMappings) bean;

		for (ResourceMetadata resourceMetadata : mappings) {

			Class<?> projection = resourceMetadata.getExcerptProjection();

			if (projection != null) {

				Projection annotation = AnnotationUtils.findAnnotation(projection, Projection.class);
				Class<?>[] target = annotation == null ? new Class[] { resourceMetadata.getDomainType() } : annotation.types();

				config.getObject().getProjectionConfiguration().addProjection(projection, target);
			}
		}

		return bean;
	}
}
