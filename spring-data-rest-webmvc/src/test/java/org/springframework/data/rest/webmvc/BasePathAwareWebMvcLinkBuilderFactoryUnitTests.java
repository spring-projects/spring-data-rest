/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for {@link BasePathAwareWebMvcLinkBuilderFactory}.
 *
 * @author Steve Rutherford
 */
class BasePathAwareWebMvcLinkBuilderFactoryUnitTests {

	RepositoryRestConfiguration configuration = mock(RepositoryRestConfiguration.class);
	BasePathAwareWebMvcLinkBuilderFactory factory;

	@BeforeEach
	void setUp() {

		// Simulate a request context so WebMvcLinkBuilder can build URIs
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.setContextPath("");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@Test // GH-2509
	void prependsBasePathForBasePathAwareController() {

		doReturn(URI.create("/api")).when(configuration).getBasePath();
		factory = new BasePathAwareWebMvcLinkBuilderFactory(configuration);

		WebMvcLinkBuilder builder = factory.linkTo(methodOn(SampleBasePathAwareController.class).getResource());
		Link link = builder.withSelfRel();

		assertThat(link.getHref()).contains("/api/items");
	}

	@Test // GH-2509
	void prependsBasePathForRepositoryRestController() {

		doReturn(URI.create("/api")).when(configuration).getBasePath();
		factory = new BasePathAwareWebMvcLinkBuilderFactory(configuration);

		WebMvcLinkBuilder builder = factory.linkTo(methodOn(SampleRepositoryRestController.class).getResource());
		Link link = builder.withSelfRel();

		assertThat(link.getHref()).contains("/api/things");
	}

	@Test // GH-2509
	void doesNotPrependBasePathForRegularController() {

		doReturn(URI.create("/api")).when(configuration).getBasePath();
		factory = new BasePathAwareWebMvcLinkBuilderFactory(configuration);

		WebMvcLinkBuilder builder = factory.linkTo(methodOn(SampleRegularController.class).getResource());
		Link link = builder.withSelfRel();

		// Regular controllers should NOT have the basePath prepended
		assertThat(link.getHref()).doesNotContain("/api/other");
		assertThat(link.getHref()).endsWith("/other");
	}

	@Test // GH-2509
	void doesNotPrependEmptyBasePath() {

		doReturn(URI.create("")).when(configuration).getBasePath();
		factory = new BasePathAwareWebMvcLinkBuilderFactory(configuration);

		WebMvcLinkBuilder builder = factory.linkTo(methodOn(SampleBasePathAwareController.class).getResource());
		Link link = builder.withSelfRel();

		assertThat(link.getHref()).endsWith("/items");
	}

	@Test // GH-2509
	void doesNotDoublePrependBasePath() {

		doReturn(URI.create("/api")).when(configuration).getBasePath();
		factory = new BasePathAwareWebMvcLinkBuilderFactory(configuration);

		WebMvcLinkBuilder builder = factory.linkTo(methodOn(SampleBasePathAwareController.class).getResource());
		Link link = builder.withSelfRel();

		// Should contain /api/items exactly once, not /api/api/items
		String href = link.getHref();
		int firstOccurrence = href.indexOf("/api/items");
		int lastOccurrence = href.lastIndexOf("/api/items");
		assertThat(firstOccurrence).isEqualTo(lastOccurrence);
	}

	@BasePathAwareController
	static class SampleBasePathAwareController {

		@GetMapping("/items")
		ResponseEntity<Void> getResource() {
			return ResponseEntity.ok().build();
		}
	}

	@RepositoryRestController
	static class SampleRepositoryRestController {

		@GetMapping("/things")
		ResponseEntity<Void> getResource() {
			return ResponseEntity.ok().build();
		}
	}

	static class SampleRegularController {

		@GetMapping("/other")
		ResponseEntity<Void> getResource() {
			return ResponseEntity.ok().build();
		}
	}
}
