/*
 * Copyright 2014-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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
		assertThat(link.getRel()).isEqualTo("person");
	}

	@Test
	public void returnsTemplatedLinkForPagingResource() {

		Link link = entityLinks.linkToCollectionResource(Person.class);

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("page", "size", "sort");
		assertThat(link.getRel()).isEqualTo("people");
	}

	@Test // DATAREST-221
	public void returnsLinkWithProjectionTemplateVariableIfProjectionIsDefined() {

		Link link = entityLinks.linkToSingleResource(Order.class, 1);

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains(configuration.getProjectionConfiguration().getParameterName());
	}

	@Test // DATAREST-155
	public void usesCustomGeneratedBackendId() {

		Link link = entityLinks.linkToSingleResource(Book.class, 7L);
		assertThat(link.expand().getHref(), endsWith("/7-7-7-7-7-7-7"));
	}

	@Test // DATAREST-317
	public void adaptsToExistingPageable() {

		Link link = entityLinks.linkToPagedResource(Person.class, PageRequest.of(0, 10));

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).hasSize(2);
		assertThat(link.getVariableNames()).contains("sort", "projection");
	}

	@Test // DATAREST-467
	public void returnsLinksToSearchResources() {

		Links links = entityLinks.linksToSearchResources(Person.class);

		assertThat(links.hasLink("firstname")).isTrue();

		Link firstnameLink = links.getLink("firstname").orElse(null);
		assertThat(firstnameLink.isTemplated()).isTrue();
		assertThat(firstnameLink.getVariableNames()).contains("page", "size");
	}

	@Test // DATAREST-467
	public void returnsLinkToSearchResource() {

		Link link = entityLinks.linkToSearchResource(Person.class, "firstname");

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("firstname", "page", "size");
	}

	@Test // DATAREST-467, DATAREST-519
	public void prepopulatesPaginationInformationForSearchResourceLink() {

		Link link = entityLinks.linkToSearchResource(Person.class, "firstname", PageRequest.of(0, 10));

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("firstname");
		assertThat(link.getVariableNames()).doesNotContain("page", "size");

		UriComponents components = UriComponentsBuilder.fromUriString(link.getHref()).build();
		assertThat(components.getQueryParams(), allOf(hasKey("page"), hasKey("size")));
	}

	@Test // DATAREST-467
	public void returnsTemplatedLinkForSortedSearchResource() {

		Link link = entityLinks.linkToSearchResource(Person.class, "lastname");

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("lastname", "sort");
	}

	@Test // DATAREST-467, DATAREST-519
	public void prepopulatesSortInformationForSearchResourceLink() {

		Link link = entityLinks.linkToSearchResource(Person.class, "lastname", Sort.by("firstname"));

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("lastname");
		assertThat(link.getVariableNames()).doesNotContain("sort");

		UriComponents components = UriComponentsBuilder.fromUriString(link.getHref()).build();
		assertThat(components.getQueryParams(), hasKey("sort"));
	}

	@Test // DATAREST-668, DATAREST-519, DATAREST-467
	public void addsProjectVariableToSearchResourceIfAvailable() {

		for (Link link : entityLinks.linksToSearchResources(Book.class)) {
			assertThat(link.getVariableNames()).contains("projection");
		}
	}
}
