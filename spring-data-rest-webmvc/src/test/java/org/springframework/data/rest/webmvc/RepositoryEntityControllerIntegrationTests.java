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
import static org.springframework.http.HttpMethod.*;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.AddressRepository;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Integration tests for {@link RepositoryEntityController}.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = JpaRepositoryConfig.class)
@Transactional
public class RepositoryEntityControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryEntityController controller;
	@Autowired AddressRepository repository;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntityResourceAssembler assembler;
	@Autowired PersistentEntities entities;

	/**
	 * @see DATAREST-217
	 */
	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void returnsNotFoundForListingEntitiesIfFindAllNotExported() throws Exception {

		repository.save(new Address());

		RootResourceInformation request = getResourceInformation(Address.class);
		controller.getCollectionResource(request, null, null, null);
	}

	/**
	 * @see DATAREST-217
	 */
	@Test(expected = HttpRequestMethodNotSupportedException.class)
	public void rejectsEntityCreationIfSaveIsNotExported() throws Exception {

		RootResourceInformation request = getResourceInformation(Address.class);

		controller.postCollectionResource(request, null, null);
	}

	/**
	 * @see DATAREST-301
	 */
	@Test
	public void setsExpandedSelfUriInLocationHeader() throws Exception {

		RootResourceInformation information = getResourceInformation(Order.class);

		PersistentEntityResource persistentEntityResource = PersistentEntityResource.build(new Order(new Person()),
				entities.getPersistentEntity(Order.class)).build();

		ResponseEntity<?> entity = controller.putItemResource(information, persistentEntityResource, 1L, assembler);

		assertThat(entity.getHeaders().getLocation().toString(), not(endsWith("{?projection}")));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForCollectionResourceIfExported() throws Exception {
		ResponseEntity<?> entity = controller.headCollectionResource(getResourceInformation(Person.class));
		assertThat(entity.getStatusCode(), is(HttpStatus.NO_CONTENT));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForCollectionResourceIfNotExported() throws Exception {
		controller.headCollectionResource(getResourceInformation(CreditCard.class));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForItemResourceIfExported() throws Exception {

		Address address = repository.save(new Address());

		ResponseEntity<?> entity = controller.headForItemResource(getResourceInformation(Address.class), address.id);
		assertThat(entity.getStatusCode(), is(HttpStatus.NO_CONTENT));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForItemResourceIfNotExisting() throws Exception {
		controller.headForItemResource(getResourceInformation(CreditCard.class), 1L);
	}

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void doesNotExposeMethodsForOptionsIfNotHttpMethodsSupportedForCollectionResource() {

		HttpEntity<?> response = controller.optionsForCollectionResource(getResourceInformation(Address.class));
		assertAllowHeaders(response, OPTIONS);
	}

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void exposesSupportedHttpMethodsInAllowHeaderForOptionsRequestToCollectionResource() {

		HttpEntity<?> response = controller.optionsForCollectionResource(getResourceInformation(Person.class));
		assertAllowHeaders(response, GET, POST, HEAD, OPTIONS);
	}

	/**
	 * @see DATAREST-333
	 */
	@Test
	public void exposesSupportedHttpMethodsInAllowHeaderForOptionsRequestToItemResource() {

		HttpEntity<?> response = controller.optionsForItemResource(getResourceInformation(Person.class));
		assertAllowHeaders(response, GET, PUT, PATCH, DELETE, HEAD, OPTIONS);
	}

	/**
	 * @see DATAREST-333, DATAREST-348
	 */
	@Test
	public void optionsForItermResourceSetsAllowPatchHeader() {

		ResponseEntity<?> entity = controller.optionsForItemResource(getResourceInformation(Person.class));

		List<String> value = entity.getHeaders().get("Accept-Patch");

		assertThat(value, hasSize(3));
		assertThat(value, hasItems(//
				RestMediaTypes.JSON_PATCH_JSON.toString(), //
				RestMediaTypes.MERGE_PATCH_JSON.toString(), //
				MediaType.APPLICATION_JSON_VALUE));
	}
}
