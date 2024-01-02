/*
 * Copyright 2013-2024 the original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.tests.TestMvcClient.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
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
class RepositorySearchControllerIntegrationTests extends AbstractControllerIntegrationTests {

	static final DefaultedPageable PAGEABLE = new DefaultedPageable(PageRequest.of(0, 10), true);

	@Autowired TestDataPopulator loader;
	@Autowired RepositorySearchController controller;
	@Autowired PagedResourcesAssembler<Object> pagedResourcesAssembler;
	@Autowired PersistentEntityResourceAssembler entityResourceAssembler;

	RepresentationModelAssemblers assembler;

	@BeforeEach
	void setUp() {
		loader.populateRepositories();

		this.assembler = mock(RepresentationModelAssemblers.class, Answers.RETURNS_SMART_NULLS);
	}

	@Test
	void rendersCorrectSearchLinksForPersons() throws Exception {

		RootResourceInformation request = getResourceInformation(Person.class);
		RepresentationModel<?> resource = controller.listSearches(request);

		ResourceTester tester = ResourceTester.of(resource);
		tester.assertNumberOfLinks(7); // Self link included
		tester.assertHasLinkEndingWith("findFirstPersonByFirstName",
				"findFirstPersonByFirstName{?firstname,projection}");
		tester.assertHasLinkEndingWith("firstname", "firstname{?firstname,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("lastname", "lastname{?lastname,sort,projection}");
		tester.assertHasLinkEndingWith("findByCreatedUsingISO8601Date",
				"findByCreatedUsingISO8601Date{?date,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("findByCreatedGreaterThan",
				"findByCreatedGreaterThan{?date,page,size,sort,projection}");
		tester.assertHasLinkEndingWith("findCreatedDateByLastName", "findCreatedDateByLastName{?lastname}");
	}

	@Test
	void returns404ForUnexportedRepository() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.listSearches(getResourceInformation(CreditCard.class)));
	}

	@Test
	void returns404ForRepositoryWithoutSearches() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.listSearches(getResourceInformation(Author.class)));
	}

	@Test
	void executesSearchAgainstRepository() {

		RootResourceInformation resourceInformation = getResourceInformation(Person.class);
		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>(1);
		parameters.add("firstname", "John");

		doAnswer(new Answer<CollectionModel<?>>() {

			@Override
			public CollectionModel<?> answer(InvocationOnMock invocation) throws Throwable {

				var page = (Page<Object>) invocation.getArgument(0);

				return pagedResourcesAssembler.toModel(page, entityResourceAssembler);
			}
		}).when(assembler).toCollectionModel(any(), any());

		ResponseEntity<?> response = controller.executeSearch(resourceInformation, parameters, "firstname", PAGEABLE,
				Sort.unsorted(), new HttpHeaders(), assembler);

		ResourceTester tester = ResourceTester.of(response.getBody());
		PagedModel<Object> pagedResources = tester.assertIsPage();
		assertThat(pagedResources.getContent()).hasSize(1);

		ResourceMetadata metadata = getMetadata(Person.class);
		tester.withContentResource(new HasSelfLink(BASE.slash(metadata.getPath()).slash("{id}")));
	}

	@Test // DATAREST-330
	void doesNotExposeHeadForSearchResourceIfResourceDoesnHaveSearches() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.headForSearches(getResourceInformation(Author.class)));
	}

	@Test // DATAREST-330
	void exposesHeadForSearchResourceIfResourceIsNotExposed() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.headForSearches(getResourceInformation(CreditCard.class)));
	}

	@Test // DATAREST-330
	void exposesHeadForSearchResourceIfResourceIsExposed() {
		controller.headForSearches(getResourceInformation(Person.class));
	}

	@Test // DATAREST-330
	void exposesHeadForExistingQueryMethodResource() {
		controller.headForSearch(getResourceInformation(Person.class), "findByCreatedUsingISO8601Date");
	}

	@Test // DATAREST-330
	void doesNotExposeHeadForInvalidQueryMethodResource() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.headForSearch(getResourceInformation(Person.class), "foobar"));
	}

	@Test // DATAREST-333
	void searchResourceSupportsGetOnly() {
		assertAllowHeaders(controller.optionsForSearches(getResourceInformation(Person.class)), HttpMethod.GET);
	}

	@Test // DATAREST-333
	void returns404ForOptionsForRepositoryWithoutSearches() {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.optionsForSearches(getResourceInformation(Address.class)));
	}

	@Test // DATAREST-333
	void queryMethodResourceSupportsGetOnly() {

		RootResourceInformation resourceInformation = getResourceInformation(Person.class);
		HttpEntity<Object> response = controller.optionsForSearch(resourceInformation, "firstname");

		assertAllowHeaders(response, HttpMethod.GET);
	}

	@Test // DATAREST-502
	void interpretsUriAsReferenceToRelatedEntity() {

		var parameters = new LinkedMultiValueMap<String, Object>(1);
		parameters.add("author", "/author/1");

		var resourceInformation = getResourceInformation(Book.class);

		when(assembler.toCollectionModel(any(), any()))
				.thenAnswer(new Answer<CollectionModel<?>>() {

					@Override
					public CollectionModel<?> answer(InvocationOnMock invocation) throws Throwable {
						return CollectionModel.of(invocation.getArgument(0));
					}
				});

		var result = controller.executeSearch(resourceInformation, parameters, "findByAuthorsContains", PAGEABLE,
				Sort.unsorted(), new HttpHeaders(), assembler);

		assertThat(result.getBody()).isInstanceOf(CollectionModel.class);
	}

	@Test // DATAREST-515
	void repositorySearchResourceExposesDomainType() {

		RepositorySearchesResource searches = controller.listSearches(getResourceInformation(Person.class));

		assertThat(searches.getDomainType()).isAssignableFrom(Person.class);
	}

	@Test // DATAREST-1121
	void returnsSimpleResponseEntityForQueryMethod() {

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.add("lastname", "Thornton");

		ResponseEntity<?> entity = controller.executeSearch(getResourceInformation(Person.class), parameters,
				"findCreatedDateByLastName", PAGEABLE, Sort.unsorted(), new HttpHeaders(), assembler);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders()).isEmpty();
	}
}
