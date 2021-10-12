/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
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
import org.springframework.hateoas.LinkRelation;
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
class RepositoryEntityLinksIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryRestConfiguration configuration;
	@Autowired RepositoryEntityLinks entityLinks;

	@Test
	void returnsLinkToSingleResource() {

		Link link = entityLinks.linkToItemResource(Person.class, 1);

		assertThat(link.getHref()).endsWith("/people/1{?projection}");
		assertThat(link.getRel()).isEqualTo(LinkRelation.of("person"));
	}

	@Test
	void returnsTemplatedLinkForPagingResource() {

		Link link = entityLinks.linkToCollectionResource(Person.class);

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("page", "size", "sort");
		assertThat(link.getRel()).isEqualTo(LinkRelation.of("people"));
	}

	@Test // DATAREST-221
	void returnsLinkWithProjectionTemplateVariableIfProjectionIsDefined() {

		Link link = entityLinks.linkToItemResource(Order.class, 1);

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains(configuration.getProjectionConfiguration().getParameterName());
	}

	@Test // DATAREST-155
	void usesCustomGeneratedBackendId() {

		Link link = entityLinks.linkToItemResource(Book.class, 7L);
		assertThat(link.expand().getHref()).endsWith("/7-7-7-7-7-7-7");
	}

	@Test // DATAREST-317
	void adaptsToExistingPageable() {

		Link link = entityLinks.linkToPagedResource(Person.class, PageRequest.of(0, 10));

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).hasSize(2);
		assertThat(link.getVariableNames()).contains("sort", "projection");
	}

	@Test // DATAREST-467
	void returnsLinksToSearchResources() {

		Links links = entityLinks.linksToSearchResources(Person.class);

		assertThat(links.hasLink("firstname")).isTrue();

		Link firstnameLink = links.getLink("firstname").orElse(null);
		assertThat(firstnameLink.isTemplated()).isTrue();
		assertThat(firstnameLink.getVariableNames()).contains("page", "size");
	}

	@Test // DATAREST-467
	void returnsLinkToSearchResource() {

		Link link = entityLinks.linkToSearchResource(Person.class, LinkRelation.of("firstname"));

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("firstname", "page", "size");
	}

	@Test // DATAREST-467, DATAREST-519
	void prepopulatesPaginationInformationForSearchResourceLink() {

		Link link = entityLinks.linkToSearchResource(Person.class, LinkRelation.of("firstname"), PageRequest.of(0, 10));

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("firstname");
		assertThat(link.getVariableNames()).doesNotContain("page", "size");

		UriComponents components = UriComponentsBuilder.fromUriString(link.getHref()).build();
		assertThat(components.getQueryParams()).containsKey("page").containsKey("size");
	}

	@Test // DATAREST-467
	void returnsTemplatedLinkForSortedSearchResource() {

		Link link = entityLinks.linkToSearchResource(Person.class, LinkRelation.of("lastname"));

		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("lastname", "sort");
	}

	@Test // DATAREST-467, DATAREST-519
	void prepopulatesSortInformationForSearchResourceLink() {

		Link link = entityLinks.linkToSearchResource(Person.class, LinkRelation.of("lastname"), Sort.by("firstname"));

		assertThat(link).isNotNull();
		assertThat(link.isTemplated()).isTrue();
		assertThat(link.getVariableNames()).contains("lastname");
		assertThat(link.getVariableNames()).doesNotContain("sort");

		UriComponents components = UriComponentsBuilder.fromUriString(link.getHref()).build();
		assertThat(components.getQueryParams()).containsKey("sort");
	}

	@Test // DATAREST-668, DATAREST-519, DATAREST-467
	void addsProjectVariableToSearchResourceIfAvailable() {

		for (Link link : entityLinks.linksToSearchResources(Book.class)) {
			assertThat(link.getVariableNames()).contains("projection");
		}
	}

	@Test // #1980
	void considersIdConverterInLinkForItemResource() {

		Link link = entityLinks.linkForItemResource(Book.class, 7L).withSelfRel();

		assertThat(link.getHref()).endsWith("/books/7-7-7-7-7-7-7");
	}
}
