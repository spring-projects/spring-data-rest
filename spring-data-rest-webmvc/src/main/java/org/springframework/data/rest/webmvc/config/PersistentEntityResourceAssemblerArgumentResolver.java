/*
 * Copyright 2014-2023 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.projection.ProjectionDefinitions;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
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

	private final PersistentEntities entities;
	private final SelfLinkProvider linkProvider;
	private final ProjectionDefinitions projectionDefinitions;
	private final ProjectionFactory projectionFactory;
	private final Associations associations;

	public PersistentEntityResourceAssemblerArgumentResolver(PersistentEntities entities, SelfLinkProvider linkProvider,
			ProjectionDefinitions projectionDefinitions, ProjectionFactory projectionFactory,
			Associations associations) {

		Assert.notNull(entities, "PersistentEntities must not be null");
		Assert.notNull(linkProvider, "SelfLinkProvider must not be null");
		Assert.notNull(projectionDefinitions, "ProjectionDefinitions must not be null");
		Assert.notNull(projectionFactory, "ProjectionFactory must not be null");
		Assert.notNull(associations, "Associations must not be null");

		this.entities = entities;
		this.linkProvider = linkProvider;
		this.projectionDefinitions = projectionDefinitions;
		this.projectionFactory = projectionFactory;
		this.associations = associations;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PersistentEntityResourceAssembler.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		String projectionParameter = webRequest.getParameter(projectionDefinitions.getParameterName());
		PersistentEntityProjector projector = new PersistentEntityProjector(projectionDefinitions, projectionFactory,
				projectionParameter, associations.getMappings());

		return new PersistentEntityResourceAssembler(entities, projector, associations, linkProvider);
	}
}
