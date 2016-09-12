/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to resolve {@link Pageable} from a
 * {@link PageableHandlerMethodArgumentResolver} applying field to property mapping.
 * <p>
 * A resolved {@link Pageable} is post-processed by applying Jackson field-to-property mapping if it contains a
 * {@link Sort} instance. Customized fields are resolved to their property names. Unknown properties are removed from
 * {@link Sort}.
 * 
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.6, 2.5.3, 2.4.5
 */
public class MappingAwarePageableArgumentResolver implements HandlerMethodArgumentResolver {

	private final JacksonMappingAwareSortTranslator translator;
	private final PageableHandlerMethodArgumentResolver delegate;

	/**
	 * Creates a new {@link MappingAwarePageableArgumentResolver} for the given {@link JacksonMappingAwareSortTranslator}
	 * and {@link PageableHandlerMethodArgumentResolver}.
	 *
	 * @param translator must not be {@literal null}.
	 * @param delegate must not be {@literal null}.
	 */
	public MappingAwarePageableArgumentResolver(JacksonMappingAwareSortTranslator translator,
			PageableHandlerMethodArgumentResolver delegate) {

		Assert.notNull(translator, "JacksonMappingSortTranslator must not be null!");
		Assert.notNull(delegate, "PageableHandlerMethodArgumentResolver must not be null!");

		this.translator = translator;
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return delegate.supportsParameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Pageable resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

		if (pageable == null || pageable.getSort() == null) {
			return null;
		}

		Sort translated = translator.translateSort(pageable.getSort(), parameter, webRequest);
		return new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), translated);
	}
}
