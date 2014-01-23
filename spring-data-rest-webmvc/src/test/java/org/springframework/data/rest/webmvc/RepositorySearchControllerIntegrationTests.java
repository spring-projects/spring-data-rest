/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.ResourceTester.HasSelfLink;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.TestDataPopulator;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link RepositorySearchController}.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class RepositorySearchControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired TestDataPopulator loader;
	@Autowired RepositorySearchController controller;

	@Before
	public void setUp() {
		loader.populateRepositories();
	}

	@Test
	public void rendersCorrectSearchLinksForPersons() {

		RepositoryRestRequest request = getRequest(Person.class);
		ResourceSupport resource = controller.listSearches(request);

		ResourceTester tester = ResourceTester.of(resource);
		tester.assertNumberOfLinks(4);
		tester.assertHasLinkEndingWith("findFirstPersonByFirstName", "findFirstPersonByFirstName{?firstName}");
		tester.assertHasLinkEndingWith("firstname", "firstname{?firstName,page,size,sort}");
		tester.assertHasLinkEndingWith("findByCreatedUsingISO8601Date",
				"findByCreatedUsingISO8601Date{?date,page,size,sort}");
		tester.assertHasLinkEndingWith("findByCreatedGreaterThan", "findByCreatedGreaterThan{?date,page,size,sort}");
	}

	@Test(expected = ResourceNotFoundException.class)
	public void returns404ForUnexportedRepository() {

		RepositoryRestRequest request = getRequest(CreditCard.class);
		controller.listSearches(request);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void returns404ForRepositoryWithoutSearches() {

		RepositoryRestRequest request = getRequest(Order.class);
		controller.listSearches(request);
	}

	@Test
	public void executesSearchAgainstRepository() {

		RequestParameters parameters = new RequestParameters("firstName", "John");
		RepositoryRestRequest request = getRequest(Person.class, parameters);

		ResponseEntity<Resources<?>> response = controller.executeSearch(request, "firstname", null);

		ResourceTester tester = ResourceTester.of(response.getBody());
		PagedResources<Object> pagedResources = tester.assertIsPage();
		assertThat(pagedResources.getContent().size(), is(1));

		ResourceMetadata metadata = getMetadata(Person.class);
		tester.withContentResource(new HasSelfLink(BASE.slash(metadata.getPath()).slash("{id}")));
	}
}
