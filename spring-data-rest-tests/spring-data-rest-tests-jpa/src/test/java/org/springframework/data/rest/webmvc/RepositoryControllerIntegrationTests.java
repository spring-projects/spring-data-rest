/*
 * Copyright 2014-2025 the original author or authors.
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
import static org.springframework.data.rest.tests.TestMvcClient.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link RepositoryController}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
class RepositoryControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryController controller;

	@Test // DATAREST-333
	void rootResourceExposesGetOnly() {

		HttpEntity<?> response = controller.optionsForRepositories();
		assertAllowHeaders(response, HttpMethod.GET);
	}

	@Test // DATAREST-333, DATAREST-330
	void headRequestReturnsNoContent() {
		assertThat(controller.headForRepositories().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test // DATAREST-160, DATAREST-333, DATAREST-463
	void exposesLinksToRepositories() {

		RepositoryLinksResource resource = controller.listRepositories().getBody();

		assertThat(resource.getLinks()).hasSize(9);

		assertThat(resource.hasLink("people")).isTrue();
		assertThat(resource.hasLink("orders")).isTrue();
		assertThat(resource.hasLink("addresses")).isTrue();
		assertThat(resource.hasLink("books")).isTrue();
		assertThat(resource.hasLink("authors")).isTrue();
		assertThat(resource.hasLink("receipts")).isTrue();
		assertThat(resource.hasLink("items")).isTrue();
		assertThat(resource.hasLink("categories")).isTrue();
	}
}
