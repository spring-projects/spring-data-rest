/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.springframework.util.ClassUtils.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.util.UriUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to create {@link ResourceMetadata} instances.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class ResourceMetadataHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final ResourceMappings mappings;
	private final BaseUri baseUri;

	/**
	 * Creates a new {@link ResourceMetadataHandlerMethodArgumentResolver} for the given {@link Repositories} and
	 * {@link ResourceMappings}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param baseUri must not be {@literal null}.
	 */
	public ResourceMetadataHandlerMethodArgumentResolver(Repositories repositories, ResourceMappings mappings,
			BaseUri baseUri) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(baseUri, "BaseUri must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
		this.baseUri = baseUri;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return isAssignable(parameter.getParameterType(), RepositoryInformation.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public ResourceMetadata resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		String lookupPath = baseUri.getRepositoryLookupPath(webRequest);
		String repositoryKey = UriUtils.findMappingVariable("repository", parameter.getMethod(), lookupPath);

		if (!hasText(repositoryKey)) {
			return null;
		}

		for (Class<?> domainType : repositories) {
			ResourceMetadata mapping = mappings.getMappingFor(domainType);
			if (mapping.getPath().matches(repositoryKey) && mapping.isExported()) {
				return mapping;
			}
		}

		throw new IllegalArgumentException(String.format("Could not resolve repository metadata for %s.", repositoryKey));
	}
}
