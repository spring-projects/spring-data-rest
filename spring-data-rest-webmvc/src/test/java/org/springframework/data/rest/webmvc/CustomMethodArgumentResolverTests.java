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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.annotation.BaseURI;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomMethodArgumentResolverTests {

	static final MethodParameter BASE_URI;
	static final MethodParameter PAGE_SORT;

	static {
		try {
			BASE_URI = MethodParameter.forMethodOrConstructor(Methods.class.getDeclaredMethod("baseUri", URI.class), 0);
			PAGE_SORT = MethodParameter.forMethodOrConstructor(
					Methods.class.getDeclaredMethod("pagingAndSorting", Pageable.class), 0);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	private final RepositoryRestConfiguration config = new RepositoryRestConfiguration().setBaseUri(URI
			.create("http://localhost:8080"));
	private final BaseUriMethodArgumentResolver baseUriResolver = new BaseUriMethodArgumentResolver(config);
	private final PageableHandlerMethodArgumentResolver pageSortResolver = new PageableHandlerMethodArgumentResolver();
	private ModelAndViewContainer mavContainer;
	@Mock WebDataBinderFactory webDataBinderFactory;

	@Before
	public void setup() {
		mavContainer = new ModelAndViewContainer();

		pageSortResolver.setOneIndexedParameters(true);
		pageSortResolver.setFallbackPageable(new PageRequest(1, 5));
	}

	@Test
	public void baseUriMethodArgumentResolver() throws Exception {
		assertThat("Finds @BaseURI-annotated java.net.URI parameter", baseUriResolver.supportsParameter(BASE_URI), is(true));

		// Resolve the base URI
		URI baseUri = (URI) baseUriResolver.resolveArgument(BASE_URI, mavContainer, new ServletWebRequest(
				Requests.ROOT_REQUEST), webDataBinderFactory);

		assertThat("Base URI should be 'http://localhost:8080'", baseUri.toString(), is("http://localhost:8080"));
	}

	@Test
	public void pagingAndSortingMethodArgumentResolver() throws Exception {
		assertThat("Finds PagingAndSorting parameter", pageSortResolver.supportsParameter(PAGE_SORT), is(true));

		// Resolve Page and Sort information
		Pageable pageSort = pageSortResolver.resolveArgument(PAGE_SORT, mavContainer, new ServletWebRequest(
				Requests.PAGE_REQUEST), webDataBinderFactory);

		assertThat("Finds page parameter value", pageSort.getPageNumber(), is(1));
		assertThat("Finds limit parameter value", pageSort.getPageSize(), is(10));
	}

	static class Methods {
		void baseUri(@BaseURI URI baseUri) {}

		void pagingAndSorting(Pageable pageSort) {}
	}
}
