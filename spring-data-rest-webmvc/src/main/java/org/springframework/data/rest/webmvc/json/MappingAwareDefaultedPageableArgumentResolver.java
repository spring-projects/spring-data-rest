/*
 * Copyright 2016-2023 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to resolve {@link DefaultedPageable} from a
 * {@link PageableHandlerMethodArgumentResolver} applying field to property mapping.
 * <p>
 * A resolved {@link DefaultedPageable} is post-processed by applying Jackson field-to-property mapping if it contains a
 * {@link Sort} instance. Customized fields are resolved to their property names. Unknown properties are removed from
 * {@link Sort}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.6
 */
public class MappingAwareDefaultedPageableArgumentResolver implements HandlerMethodArgumentResolver {

	private final JacksonMappingAwareSortTranslator translator;
	private final PageableHandlerMethodArgumentResolver delegate;

	public MappingAwareDefaultedPageableArgumentResolver(JacksonMappingAwareSortTranslator translator,
			PageableHandlerMethodArgumentResolver delegate) {

		Assert.notNull(translator, "JacksonMappingAwareSortTranslator must not be null");
		Assert.notNull(delegate, "Delegate PageableHandlerMethodArgumentResolver must not be null");

		this.translator = translator;
		this.delegate = delegate;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return DefaultedPageable.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

		if (pageable == null || pageable.getSort() == null) {
			return new DefaultedPageable(pageable, delegate.isFallbackPageable(pageable));
		}

		Sort translated = translator.translateSort(pageable.getSort(), parameter, webRequest);
		pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translated);

		return new DefaultedPageable(pageable, delegate.isFallbackPageable(pageable));
	}
}
