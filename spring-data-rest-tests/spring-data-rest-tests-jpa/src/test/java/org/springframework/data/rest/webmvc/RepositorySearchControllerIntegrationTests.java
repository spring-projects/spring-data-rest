/*
 * Copyright 2013-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.tests.ResourceTester;
import org.springframework.data.rest.tests.ResourceTester.HasSelfLink;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.Author;
import org.springframework.data.rest.webmvc.jpa.Book;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.TestDataPopulator;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration tests for the {@link RepositorySearchController}.
 *
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class RepositorySearchControllerIntegrationTests extends AbstractControllerIntegrationTests {

	static final DefaultedPageable PAGEABLE = new DefaultedPageable(PageRequest.of(0, 10), true);

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
		RepresentationModel resource = controller.listSearches(request);

		ResourceTester tester = ResourceTester.of(resource);
		tester.assertNumberOfLinks(7); // Self link included
		tester.assertHasLinkEndingWith("findFirstPersonByFirstName", "findFirstPersonByFirstName{?firstname,projection}");
		tester.assertHasLinkEndingWith("firstname", "firstname{?firstname,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("lastname", "lastname{?lastname,sort,projection}");
		tester.assertHasLinkEndingWith("findByCreatedUsingISO8601Date",
				"findByCreatedUsingISO8601Date{?date,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("findByCreatedGreaterThan",
				"findByCreatedGreaterThan{?date,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("findCreatedDateByLastName", "findCreatedDateByLastName{?lastname}");
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

		RootResourceInformation resourceInformation = getResourceInformation(Person.class);
		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>(1);
		parameters.add("firstname", "John");

		ResponseEntity<?> response = controller.executeSearch(resourceInformation, parameters, "firstname", PAGEABLE,
				Sort.unsorted(), assembler, new HttpHeaders());

		ResourceTester tester = ResourceTester.of(response.getBody());
		PagedModel<Object> pagedResources = tester.assertIsPage();
		assertThat(pagedResources.getContent()).hasSize(1);

		ResourceMetadata metadata = getMetadata(Person.class);
		tester.withContentResource(new HasSelfLink(BASE.slash(metadata.getPath()).slash("{id}")));
	}

	@Test(expected = ResourceNotFoundException.class) // DATAREST-330
	public void doesNotExposeHeadForSearchResourceIfResourceDoesnHaveSearches() {
		controller.headForSearches(getResourceInformation(Author.class));
	}

	@Test(expected = ResourceNotFoundException.class) // DATAREST-330
	public void exposesHeadForSearchResourceIfResourceIsNotExposed() {
		controller.headForSearches(getResourceInformation(CreditCard.class));
	}

	@Test // DATAREST-330
	public void exposesHeadForSearchResourceIfResourceIsExposed() {
		controller.headForSearches(getResourceInformation(Person.class));
	}

	@Test // DATAREST-330
	public void exposesHeadForExistingQueryMethodResource() {
		controller.headForSearch(getResourceInformation(Person.class), "findByCreatedUsingISO8601Date");
	}

	@Test(expected = ResourceNotFoundException.class) // DATAREST-330
	public void doesNotExposeHeadForInvalidQueryMethodResource() {
		controller.headForSearch(getResourceInformation(Person.class), "foobar");
	}

	@Test // DATAREST-333
	public void searchResourceSupportsGetOnly() {
		assertAllowHeaders(controller.optionsForSearches(getResourceInformation(Person.class)), HttpMethod.GET);
	}

	@Test(expected = ResourceNotFoundException.class) // DATAREST-333
	public void returns404ForOptionsForRepositoryWithoutSearches() {
		controller.optionsForSearches(getResourceInformation(Address.class));
	}

	@Test // DATAREST-333
	public void queryMethodResourceSupportsGetOnly() {

		RootResourceInformation resourceInformation = getResourceInformation(Person.class);
		HttpEntity<Object> response = controller.optionsForSearch(resourceInformation, "firstname");

		assertAllowHeaders(response, HttpMethod.GET);
	}

	@Test // DATAREST-502
	public void interpretsUriAsReferenceToRelatedEntity() {

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>(1);
		parameters.add("author", "/author/1");

		RootResourceInformation resourceInformation = getResourceInformation(Book.class);

		ResponseEntity<?> result = controller.executeSearch(resourceInformation, parameters, "findByAuthorsContains",
				PAGEABLE, Sort.unsorted(), assembler, new HttpHeaders());

		assertThat(result.getBody()).isInstanceOf(CollectionModel.class);
	}

	@Test // DATAREST-515
	public void repositorySearchResourceExposesDomainType() {

		RepositorySearchesResource searches = controller.listSearches(getResourceInformation(Person.class));

		assertThat(searches.getDomainType()).isAssignableFrom(Person.class);
	}

	@Test // DATAREST-1121
	public void returnsSimpleResponseEntityForQueryMethod() {

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.add("lastname", "Thornton");

		ResponseEntity<?> entity = controller.executeSearch(getResourceInformation(Person.class), parameters,
				"findCreatedDateByLastName", PAGEABLE, Sort.unsorted(), assembler, new HttpHeaders());

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders()).isEmpty();
	}
}
