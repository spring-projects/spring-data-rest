/*
 * Copyright 2015-2018 original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.hateoas.server.mvc.UriComponentsContributor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link ArgumentResolverPagingAndSortingTemplateVariables}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class ArgumentResolverPagingAndSortingTemplateVariablesUnitTests {

	@Mock HateoasPageableHandlerMethodArgumentResolver pageableResolver;
	@Mock HateoasSortHandlerMethodArgumentResolver sortResolver;
	@Mock UriComponentsBuilder builder;

	@Test // DATAREST-467
	void rejectsNullArgumentResolverForPageable() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ArgumentResolverPagingAndSortingTemplateVariables(null, sortResolver));
	}

	@Test // DATAREST-467
	void rejectsNullArgumentResolverForSort() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new ArgumentResolverPagingAndSortingTemplateVariables(pageableResolver, null));
	}

	@Test // DATAREST-467
	void supportsPageableAndSortMethodParameters() {

		PagingAndSortingTemplateVariables variables = new ArgumentResolverPagingAndSortingTemplateVariables(
				pageableResolver, sortResolver);

		assertThat(variables.supportsParameter(getParameterMock(Pageable.class))).isTrue();
		assertThat(variables.supportsParameter(getParameterMock(Sort.class))).isTrue();
		assertThat(variables.supportsParameter(getParameterMock(Object.class))).isFalse();
	}

	@Test // DATAREST-467
	void forwardsEnhanceRequestForPageable() {
		assertForwardsEnhanceFor(PageRequest.of(0, 10), pageableResolver, sortResolver);
	}

	@Test // DATAREST-467
	void forwardsEnhanceRequestForSort() {
		assertForwardsEnhanceFor(Sort.by("property"), sortResolver, pageableResolver);
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
				any());
	}
}
