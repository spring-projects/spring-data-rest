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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.webmvc.TestMvcClient.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class RepositoryControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryController controller;

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void rootResourceExposesGetOnly() {

		HttpEntity<?> response = controller.optionsForRepositories();
		assertAllowHeaders(response, HttpMethod.GET);
	}

	/**
	 * @see DATAREST-333, DATAREST-330
	 */
	@Test
	public void headRequestReturnsNoContent() {
		assertThat(controller.headForRepositories().getStatusCode(), is(HttpStatus.NO_CONTENT));
	}

	@Test
	public void exposesLinksToRepositories() {

		RepositoryLinksResource resource = controller.listRepositories().getBody();

		assertThat(resource.getLinks(), hasSize(5));

		assertThat(resource.hasLink("people"), is(true));
		assertThat(resource.hasLink("orders"), is(true));
		assertThat(resource.hasLink("addresses"), is(true));
		assertThat(resource.hasLink("books"), is(true));
		assertThat(resource.hasLink("authors"), is(true));
	}
}
