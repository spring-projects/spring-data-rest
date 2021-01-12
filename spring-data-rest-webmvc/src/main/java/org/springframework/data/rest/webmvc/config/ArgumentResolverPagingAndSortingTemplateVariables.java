/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link PagingAndSortingTemplateVariables} implementation to delegate to the HATEOAS-enabled
 * {@link HandlerMethodArgumentResolver}s for {@link Pageable} and {@link Sort}.
 *
 * @author Oliver Gierke
 * @since 2.3
 */
class ArgumentResolverPagingAndSortingTemplateVariables implements PagingAndSortingTemplateVariables {

	private static final Set<Class<?>> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays
			.<Class<?>> asList(Pageable.class, Sort.class)));

	private final HateoasPageableHandlerMethodArgumentResolver pagingResolver;
	private final HateoasSortHandlerMethodArgumentResolver sortResolver;

	/**
	 * Creates a new {@link ArgumentResolverPagingAndSortingTemplateVariables} using the given
	 * {@link HateoasPageableHandlerMethodArgumentResolver} and {@link HateoasSortHandlerMethodArgumentResolver}.
	 *
	 * @param pagingResolver must not be {@literal null}.
	 * @param sortResolver must not be {@literal null}.
	 */
	public ArgumentResolverPagingAndSortingTemplateVariables(HateoasPageableHandlerMethodArgumentResolver pagingResolver,
			HateoasSortHandlerMethodArgumentResolver sortResolver) {

		Assert.notNull(pagingResolver, "HateoasPageableHandlerMethodArgumentResolver must not be null!");
		Assert.notNull(sortResolver, "HateoasSortHandlerMethodArgumentResolver must not be null!");

		this.pagingResolver = pagingResolver;
		this.sortResolver = sortResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables#getPaginationTemplateVariables(org.springframework.core.MethodParameter, org.springframework.web.util.UriComponents)
	 */
	@Override
	public TemplateVariables getPaginationTemplateVariables(MethodParameter parameter, UriComponents components) {
		return pagingResolver.getPaginationTemplateVariables(parameter, components);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables#getSortTemplateVariables(org.springframework.core.MethodParameter, org.springframework.web.util.UriComponents)
	 */
	@Override
	public TemplateVariables getSortTemplateVariables(MethodParameter parameter, UriComponents template) {
		return sortResolver.getSortTemplateVariables(parameter, template);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.mvc.UriComponentsContributor#enhance(org.springframework.web.util.UriComponentsBuilder, org.springframework.core.MethodParameter, java.lang.Object)
	 */
	@Override
	public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {

		if (value instanceof Pageable) {
			pagingResolver.enhance(builder, parameter, value);
		} else if (value instanceof Sort) {
			sortResolver.enhance(builder, parameter, value);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.mvc.UriComponentsContributor#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SUPPORTED_TYPES.contains(parameter.getParameterType());
	}
}
