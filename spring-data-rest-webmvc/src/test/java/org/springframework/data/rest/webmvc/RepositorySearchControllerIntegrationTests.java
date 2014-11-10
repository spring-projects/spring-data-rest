/*
 * Copyright 2013-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.ResourceTester.HasSelfLink;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.Author;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.TestDataPopulator;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
	@Autowired PersistentEntityResourceAssembler assembler;

	@Before
	public void setUp() {
		loader.populateRepositories();
	}

	@Test
	public void rendersCorrectSearchLinksForPersons() throws Exception {

		RootResourceInformation request = getResourceInformation(Person.class);
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
		controller.listSearches(getResourceInformation(CreditCard.class));
	}

	@Test(expected = ResourceNotFoundException.class)
	public void returns404ForRepositoryWithoutSearches() {
		controller.listSearches(getResourceInformation(Author.class));
	}

	@Test
	public void executesSearchAgainstRepository() {

		RequestParameters parameters = new RequestParameters("firstName", "John");
		RootResourceInformation resourceInformation = getResourceInformation(Person.class);

		ResponseEntity<Object> response = controller.executeSearch(resourceInformation, getRequest(parameters),
				"firstname", new DefaultedPageable(new PageRequest(0, 10), true), null, assembler);

		ResourceTester tester = ResourceTester.of(response.getBody());
		PagedResources<Object> pagedResources = tester.assertIsPage();
		assertThat(pagedResources.getContent().size(), is(1));

		ResourceMetadata metadata = getMetadata(Person.class);
		tester.withContentResource(new HasSelfLink(BASE.slash(metadata.getPath()).slash("{id}")));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForSearchResourceIfResourceDoesnHaveSearches() {
		controller.headForSearches(getResourceInformation(Author.class));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void exposesHeadForSearchResourceIfResourceIsNotExposed() {
		controller.headForSearches(getResourceInformation(CreditCard.class));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForSearchResourceIfResourceIsExposed() {
		controller.headForSearches(getResourceInformation(Person.class));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForExistingQueryMethodResource() {
		controller.headForSearch(getResourceInformation(Person.class), "findByCreatedUsingISO8601Date");
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForInvalidQueryMethodResource() {
		controller.headForSearch(getResourceInformation(Person.class), "foobar");
	}

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void searchResourceSupportsGetOnly() {
		assertAllowHeaders(controller.optionsForSearches(getResourceInformation(Person.class)), HttpMethod.GET);
	}

	/**
	 * @see DATAREST-333
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void returns404ForOptionsForRepositoryWithoutSearches() {
		controller.optionsForSearches(getResourceInformation(Address.class));
	}

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void queryMethodResourceSupportsGetOnly() {

		RootResourceInformation resourceInformation = getResourceInformation(Person.class);
		HttpEntity<Object> response = controller.optionsForSearch(resourceInformation, "firstname");

		assertAllowHeaders(response, HttpMethod.GET);
	}
}
