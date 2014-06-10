/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.projection.ProjectionDefinitions;
import org.springframework.data.rest.core.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
import org.springframework.hateoas.EntityLinks;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link PersistentEntityResourceAssembler}s.
 * 
 * @author Oliver Gierke
 */
public class PersistentEntityResourceAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final EntityLinks entityLinks;
	private final ProjectionDefinitions projectionDefinitions;
	private final ProjectionFactory projectionFactory;
	private final ResourceMappings mappings;

	/**
	 * Creates a new {@link PersistentEntityResourceAssemblerArgumentResolver} for the given {@link Repositories},
	 * {@link EntityLinks}, {@link ProjectionDefinitions} and {@link ProjectionFactory}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param entityLinks must not be {@literal null}.
	 * @param projectionDefinitions must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 */
	public PersistentEntityResourceAssemblerArgumentResolver(Repositories repositories, EntityLinks entityLinks,
			ProjectionDefinitions projectionDefinitions, ProjectionFactory projectionFactory, ResourceMappings mappings) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(entityLinks, "EntityLinks must not be null!");
		Assert.notNull(projectionDefinitions, "ProjectionDefinitions must not be null!");
		Assert.notNull(projectionFactory, "ProjectionFactory must not be null!");

		this.repositories = repositories;
		this.entityLinks = entityLinks;
		this.projectionDefinitions = projectionDefinitions;
		this.projectionFactory = projectionFactory;
		this.mappings = mappings;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PersistentEntityResourceAssembler.class.equals(parameter.getParameterType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		String projectionParameter = webRequest.getParameter(projectionDefinitions.getParameterName());
		PersistentEntityProjector projector = new PersistentEntityProjector(projectionDefinitions, projectionFactory,
				projectionParameter, mappings);

		return new PersistentEntityResourceAssembler(repositories, entityLinks, projector, mappings);
	}
}
