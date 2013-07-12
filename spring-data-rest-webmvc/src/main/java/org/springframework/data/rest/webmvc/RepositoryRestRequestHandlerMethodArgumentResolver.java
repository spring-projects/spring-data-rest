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

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestRequestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ConversionService conversionService;

	@Autowired private RepositoryRestConfiguration config;
	@Autowired private Repositories repositories;
	@Autowired private ResourceMetadataHandlerMethodArgumentResolver repoInfoResolver;
	@Autowired private BaseUriMethodArgumentResolver baseUriResolver;

	public RepositoryRestRequestHandlerMethodArgumentResolver(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RepositoryRestRequest.class.isAssignableFrom(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public RepositoryRestRequest resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		URI baseUri = baseUriResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
		ResourceMetadata repoInfo = repoInfoResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

		// TODO reject if ResourceMetadata cannot be resolved

		return new RepositoryRestRequest(config, repositories, webRequest.getNativeRequest(HttpServletRequest.class),
				baseUri, repoInfo, conversionService);
	}
}
