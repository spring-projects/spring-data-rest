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
package org.springframework.data.rest.webmvc.support;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to resolve {@link DefaultedPageable} if requested. Allows to find out whether
 * the resolved {@link Pageable} is logically identical to the fallback on configured on the delegate
 * {@link PageableHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 */
public class DefaultedPageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final PageableHandlerMethodArgumentResolver resolver;

	/**
	 * Creates a new {@link DefaultedPageableHandlerMethodArgumentResolver} delegating to the given
	 * {@link PageableHandlerMethodArgumentResolver}.
	 * 
	 * @param resolver must not be {@literal null}.
	 */
	public DefaultedPageableHandlerMethodArgumentResolver(PageableHandlerMethodArgumentResolver resolver) {

		Assert.notNull(resolver, "PageableHandlerMethodArgumentResolver must not be null!");
		this.resolver = resolver;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Pageable pageable = resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
		return new DefaultedPageable(pageable, resolver.isFallbackPageable(pageable));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return DefaultedPageable.class.isAssignableFrom(parameter.getParameterType());
	}
}
