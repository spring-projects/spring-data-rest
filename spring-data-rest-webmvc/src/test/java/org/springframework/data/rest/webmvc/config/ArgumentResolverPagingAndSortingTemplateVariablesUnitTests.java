/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link ArgumentResolverPagingAndSortingTemplateVariables}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ArgumentResolverPagingAndSortingTemplateVariablesUnitTests {

	@Mock HateoasPageableHandlerMethodArgumentResolver pageableResolver;
	@Mock HateoasSortHandlerMethodArgumentResolver sortResolver;
	@Mock UriComponentsBuilder builder;

	/**
	 * @see DATAREST-467
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullArgumentResolverForPageable() {
		new ArgumentResolverPagingAndSortingTemplateVariables(null, sortResolver);
	}

	/**
	 * @see DATAREST-467
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullArgumentResolverForSort() {
		new ArgumentResolverPagingAndSortingTemplateVariables(pageableResolver, null);
	}

	/**
	 * @see DATAREST-467
	 */
	@Test
	public void supportsPageableAndSortMethodParameters() {

		PagingAndSortingTemplateVariables variables = new ArgumentResolverPagingAndSortingTemplateVariables(
				pageableResolver, sortResolver);

		assertThat(variables.supportsParameter(getParameterMock(Pageable.class)), is(true));
		assertThat(variables.supportsParameter(getParameterMock(Sort.class)), is(true));
		assertThat(variables.supportsParameter(getParameterMock(Object.class)), is(false));
	}

	/**
	 * @see DATAREST-467
	 */
	@Test
	public void forwardsEnhanceRequestForPageable() {
		assertForwardsEnhanceFor(new PageRequest(0, 10), pageableResolver, sortResolver);
	}

	/**
	 * @see DATAREST-467
	 */
	@Test
	public void forwardsEnhanceRequestForSort() {
		assertForwardsEnhanceFor(new Sort("property"), sortResolver, pageableResolver);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static MethodParameter getParameterMock(Class<?> type) {

		MethodParameter parameter = mock(MethodParameter.class);
		when(parameter.getParameterType()).thenReturn((Class) type);

		return parameter;
	}

	private void assertForwardsEnhanceFor(Object value, UriComponentsContributor expected,
			UriComponentsContributor unexpected) {

		PagingAndSortingTemplateVariables variables = new ArgumentResolverPagingAndSortingTemplateVariables(
				pageableResolver, sortResolver);

		variables.enhance(builder, null, value);

		verify(expected, times(1)).enhance(builder, null, value);
		verify(unexpected, times(0)).enhance(Mockito.any(UriComponentsBuilder.class), Mockito.any(MethodParameter.class),
				anyObject());
	}
}
