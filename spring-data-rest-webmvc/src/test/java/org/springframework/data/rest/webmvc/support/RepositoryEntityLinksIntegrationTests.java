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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link RepositoryEntityLinks}.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class RepositoryEntityLinksIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryRestConfiguration configuration;
	@Autowired RepositoryEntityLinks entityLinks;

	@Test
	public void returnsLinkToSingleResource() {

		Link link = entityLinks.linkToSingleResource(Person.class, 1);

		assertThat(link.getHref(), endsWith("/people/1{?projection}"));
		assertThat(link.getRel(), is("person"));
	}

	@Test
	public void returnsTemplatedLinkForPagingResource() {

		Link link = entityLinks.linkToCollectionResource(Person.class);

		assertThat(link.isTemplated(), is(true));
		assertThat(link.getVariableNames(), hasItems("page", "size", "sort"));
		assertThat(link.getRel(), is("people"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void returnsLinkWithProjectionTemplateVariableIfProjectionIsDefined() {

		Link link = entityLinks.linkToSingleResource(Order.class, 1);

		assertThat(link.isTemplated(), is(true));
		assertThat(link.getVariableNames(), hasItem(configuration.projectionConfiguration().getParameterName()));
	}

	/**
	 * @see DATAREST-155
	 */
	@Test
	public void usesCustomGeneratedBackendId() {

		Link link = entityLinks.linkToSingleResource(Book.class, 7L);
		assertThat(link.expand().getHref(), endsWith("/7-7-7-7-7-7-7"));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void adaptsToExistingPageable() {

		Link link = entityLinks.linkToPagedResource(Person.class, new PageRequest(0, 10));

		assertThat(link.isTemplated(), is(true));
		assertThat(link.getVariableNames(), hasSize(2));
		assertThat(link.getVariableNames(), hasItems("sort", "projection"));
	}
}
