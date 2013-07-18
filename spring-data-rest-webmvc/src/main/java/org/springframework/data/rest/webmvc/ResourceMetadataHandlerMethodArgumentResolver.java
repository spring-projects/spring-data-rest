/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.springframework.util.ClassUtils.*;
import static org.springframework.util.StringUtils.*;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class ResourceMetadataHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final ResourceMappings mappings;

	/**
	 * @param repositories must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 */
	public ResourceMetadataHandlerMethodArgumentResolver(Repositories repositories, ResourceMappings mappings) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(mappings, "ResourceMappings must not be null!");

		this.repositories = repositories;
		this.mappings = mappings;
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

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		String requestUri = new UrlPathHelper().getLookupPathForRequest(request);

		if (requestUri.startsWith("/")) {
			requestUri = requestUri.substring(1);
		}

		String[] parts = requestUri.split("/");

		if (parts.length == 0) {
			// Root request
			return null;
		}

		return findRepositoryInfoFor(parts[0]);
	}

	private ResourceMetadata findRepositoryInfoFor(String pathSegment) {

		if (!hasText(pathSegment)) {
			return null;
		}

		for (Class<?> domainType : repositories) {
			ResourceMetadata mapping = mappings.getMappingFor(domainType);
			if (mapping.getPath().matches(pathSegment) && mapping.isExported()) {
				return mapping;
			}
		}

		return null;
	}
}
