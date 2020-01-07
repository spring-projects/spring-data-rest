/*
 * Copyright 2014-2020 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.projection.ProjectionDefinitions;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link PersistentEntityResourceAssembler}s.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class PersistentEntityResourceAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

	private final @NonNull PersistentEntities entities;
	private final @NonNull SelfLinkProvider linkProvider;
	private final @NonNull ProjectionDefinitions projectionDefinitions;
	private final @NonNull ProjectionFactory projectionFactory;
	private final @NonNull Associations links;

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
				projectionParameter, links.getMappings());

		return new PersistentEntityResourceAssembler(entities, projector, links, linkProvider);
	}
}
