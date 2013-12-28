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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Custom {@link HandlerMethodArgumentResolver} to create {@link PersistentEntityResource} instances.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityResourceHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String ERROR_MESSAGE = "Could not read an object of type %s from the request! Converter %s returned null!";
	private static final String NO_CONVERTER_FOUND = "No suitable HttpMessageConverter found to read request body into object of type %s from request with content type of %s!";

	private final RepositoryRestRequestHandlerMethodArgumentResolver repoRequestResolver;
	private final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * Creates a new {@link PersistentEntityResourceHandlerMethodArgumentResolver} for the given
	 * {@link HttpMessageConverter}s and {@link RepositoryRestRequestHandlerMethodArgumentResolver}..
	 * 
	 * @param messageConverters must not be {@literal null}.
	 * @param repositoryRequestResolver must not be {@literal null}.
	 */
	public PersistentEntityResourceHandlerMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
			RepositoryRestRequestHandlerMethodArgumentResolver repositoryRequestResolver) {

		Assert.notEmpty(messageConverters, "MessageConverters must not be null or empty!");
		Assert.notNull(repositoryRequestResolver, "RepositoryRestRequestHandlerMethodArgumentResolver must not be empty!");

		this.messageConverters = messageConverters;
		this.repoRequestResolver = repositoryRequestResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PersistentEntityResource.class.isAssignableFrom(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		RepositoryRestRequest repoRequest = (RepositoryRestRequest) repoRequestResolver.resolveArgument(parameter,
				mavContainer, webRequest, binderFactory);

		HttpServletRequest nativeRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		ServletServerHttpRequest request = new ServletServerHttpRequest(nativeRequest);

		Class<?> domainType = repoRequest.getPersistentEntity().getType();
		MediaType contentType = request.getHeaders().getContentType();

		for (HttpMessageConverter converter : messageConverters) {

			if (!converter.canRead(domainType, contentType)) {
				continue;
			}

			Object obj = converter.read(domainType, request);

			if (obj == null) {
				throw new HttpMessageNotReadableException(String.format(ERROR_MESSAGE, domainType, converter));
			}

			return new PersistentEntityResource<Object>(repoRequest.getPersistentEntity(), obj);
		}

		throw new HttpMessageNotReadableException(String.format(NO_CONVERTER_FOUND, domainType, contentType));
	}
}
