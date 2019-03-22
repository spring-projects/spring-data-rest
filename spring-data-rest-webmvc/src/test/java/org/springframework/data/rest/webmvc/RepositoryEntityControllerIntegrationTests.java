/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.webmvc.TestMvcClient.*;
import static org.springframework.http.HttpMethod.*;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.AddressRepository;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Integration tests for {@link RepositoryEntityController}.
 * 
 * @author Oliver Gierke
 * @author Jeremy Rickard
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

		controller.postCollectionResource(request, null, null, MediaType.APPLICATION_JSON_VALUE);
	}

	/**
	 * @see DATAREST-301
	 */
	@Test
	public void setsExpandedSelfUriInLocationHeader() throws Exception {

		RootResourceInformation information = getResourceInformation(Order.class);

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		ResponseEntity<?> entity = controller.putItemResource(information, persistentEntityResource, 1L, assembler,
				ETag.NO_ETAG, MediaType.APPLICATION_JSON_VALUE);

		assertThat(entity.getHeaders().getLocation().toString(), not(endsWith("{?projection}")));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForCollectionResourceIfExported() throws Exception {
		ResponseEntity<?> entity = controller.headCollectionResource(getResourceInformation(Person.class),
				new DefaultedPageable(null, false));
		assertThat(entity.getStatusCode(), is(HttpStatus.NO_CONTENT));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForCollectionResourceIfNotExported() throws Exception {
		controller.headCollectionResource(getResourceInformation(CreditCard.class), new DefaultedPageable(null, false));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test
	public void exposesHeadForItemResourceIfExported() throws Exception {

		Address address = repository.save(new Address());

		ResponseEntity<?> entity = controller.headForItemResource(getResourceInformation(Address.class), address.id,
				assembler);

		assertThat(entity.getStatusCode(), is(HttpStatus.NO_CONTENT));
	}

	/**
	 * @see DATAREST-330
	 */
	@Test(expected = ResourceNotFoundException.class)
	public void doesNotExposeHeadForItemResourceIfNotExisting() throws Exception {
		controller.headForItemResource(getResourceInformation(CreditCard.class), 1L, assembler);
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
		assertThat(value,
				hasItems(//
						RestMediaTypes.JSON_PATCH_JSON.toString(), //
						RestMediaTypes.MERGE_PATCH_JSON.toString(), //
						MediaType.APPLICATION_JSON_VALUE));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodyOnPutForUpdateIfAcceptHeaderPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		Order order = request.getInvoker().invokeSave(new Order(new Person()));

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller.putItemResource(request, persistentEntityResource, order.getId(), assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody(), is(true));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodyForCreatingPutIfAcceptHeaderPresentByDefault() throws HttpRequestMethodNotSupportedException {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller.putItemResource(request, persistentEntityResource, 1L, assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody(), is(true));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodyForPostIfAcceptHeaderIsPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller
				.postCollectionResource(request, persistentEntityResource, assembler, MediaType.APPLICATION_JSON_VALUE)
				.hasBody(), is(true));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void doesNotReturnBodyForPostIfNoAcceptHeaderPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller.postCollectionResource(request, persistentEntityResource, assembler, null).hasBody(),
				is(false));
		assertThat(controller.postCollectionResource(request, persistentEntityResource, assembler, "").hasBody(),
				is(false));
	}

	/**
	 * @see DATAREST-581
	 */
	@Test
	public void createsEtagForProjectedEntityCorrectly() throws Exception {

		Address address = repository.save(new Address());

		PersistentEntityResourceAssembler assembler = Mockito.mock(PersistentEntityResourceAssembler.class);
		AddressProjection addressProjection = new SpelAwareProxyProjectionFactory()
				.createProjection(AddressProjection.class);

		PersistentEntityResource resource = PersistentEntityResource
				.build(addressProjection, entities.getPersistentEntity(Address.class)).build();

		Mockito.when(assembler.toFullResource(Mockito.any(Object.class))).thenReturn(resource);

		ResponseEntity<Resource<?>> entity = controller.getItemResource(getResourceInformation(Address.class), address.id,
				assembler, new LinkedMultiValueMap<String, String>());

		assertThat(entity.getHeaders().getETag(), is(notNullValue()));
	}

	interface AddressProjection {}
}
