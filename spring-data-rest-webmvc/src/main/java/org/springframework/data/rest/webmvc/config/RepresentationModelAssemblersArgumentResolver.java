/*
 * Copyright 2023-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.RepresentationModelAssemblers;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SlicedResourcesAssembler;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to provide {@link RepresentationModelAssemblers}
 *
 * @author Oliver Drotbohm
 * @since 4.1
 * @soundtrack The Intersphere - Down (Wanderer, https://www.youtube.com/watch?v=3RIdTFJvDxg)
 */
public class RepresentationModelAssemblersArgumentResolver implements HandlerMethodArgumentResolver {

	private final PagedResourcesAssembler<Object> pagedResourcesAssembler;
	private final SlicedResourcesAssembler<Object> slicedResourcesAssembler;
	private final PersistentEntityResourceAssemblerArgumentResolver delegate;

	/**
	 * Creates a new {@link RepresentationModelAssemblersArgumentResolver} for the given
	 * {@link PagedResourcesAssembler}, {@link SlicedResourcesAssembler}, and
	 * {@link PersistentEntityResourceAssemblerArgumentResolver}.
	 *
	 * @param pagedResourcesAssembler must not be {@literal null}.
	 * @param slicedResourcesAssembler must not be {@literal null}.
	 * @param delegate must not be {@literal null}.
	 */
	RepresentationModelAssemblersArgumentResolver(PagedResourcesAssembler<Object> pagedResourcesAssembler,
			SlicedResourcesAssembler<Object> slicedResourcesAssembler,
			PersistentEntityResourceAssemblerArgumentResolver delegate) {

		Assert.notNull(pagedResourcesAssembler, "PagedResourcesAssembler must not be null!");
		Assert.notNull(slicedResourcesAssembler, "SlicedResourcesAssembler must not be null!");
		Assert.notNull(delegate, "PersistentEntityResourceAssemblerArgumentResolver must not be null");

		this.pagedResourcesAssembler = pagedResourcesAssembler;
		this.slicedResourcesAssembler = slicedResourcesAssembler;
		this.delegate = delegate;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RepresentationModelAssemblers.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		var persistentEntityResourceAssembler = delegate.resolveArgument(parameter, mavContainer, webRequest,
				binderFactory);

		return new RepresentationModelAssemblers(pagedResourcesAssembler, slicedResourcesAssembler,
				persistentEntityResourceAssembler);
	}
}
