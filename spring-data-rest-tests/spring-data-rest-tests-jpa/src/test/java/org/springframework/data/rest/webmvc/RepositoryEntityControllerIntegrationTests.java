/*
 * Copyright 2014-2025 the original author or authors.
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
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.tests.TestMvcClient.*;
import static org.springframework.http.HttpMethod.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.RepositoryEntityControllerIntegrationTests.ConfigurationCustomizer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.jpa.Address;
import org.springframework.data.rest.webmvc.jpa.AddressRepository;
import org.springframework.data.rest.webmvc.jpa.CreditCard;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
 * @author Jeremy Rickard
 */
@ContextConfiguration(classes = { ConfigurationCustomizer.class, JpaRepositoryConfig.class })
@Transactional
class RepositoryEntityControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryEntityController controller;
	@Autowired AddressRepository repository;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntityResourceAssembler assembler;
	@Autowired PersistentEntities entities;

	@Configuration
	static class ConfigurationCustomizer {

		@Bean
		RepositoryRestConfigurer configurer() {

			return RepositoryRestConfigurer.withConfig(config -> {
				config.getExposureConfiguration().forDomainType(Address.class).disablePutForCreation();
			});
		}
	}

	@Test // DATAREST-217
	void returnsNotFoundForListingEntitiesIfFindAllNotExported() throws Exception {

		repository.save(new Address());

		RootResourceInformation request = getResourceInformation(Address.class);

		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class) //
				.isThrownBy(() -> controller.getCollectionResource(request, null, null, null));
	}

	@Test // DATAREST-217
	void rejectsEntityCreationIfSaveIsNotExported() throws Exception {

		RootResourceInformation request = getResourceInformation(Address.class);

		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class) //
				.isThrownBy(() -> controller.postCollectionResource(request, null, null, MediaType.APPLICATION_JSON_VALUE));
	}

	@Test // DATAREST-301
	void setsExpandedSelfUriInLocationHeader() throws Exception {

		RootResourceInformation information = getResourceInformation(Order.class);

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getRequiredPersistentEntity(Order.class)).build();

		ResponseEntity<?> entity = controller.putItemResource(information, persistentEntityResource, 1L, assembler,
				ETag.NO_ETAG, MediaType.APPLICATION_JSON_VALUE);

		assertThat(entity.getHeaders().getLocation().toString()).doesNotEndWith("{?projection}");
	}

	@Test // DATAREST-330
	void exposesHeadForCollectionResourceIfExported() throws Exception {
		ResponseEntity<?> entity = controller.headCollectionResource(getResourceInformation(Person.class),
				new DefaultedPageable(Pageable.unpaged(), false));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test // DATAREST-330
	void doesNotExposeHeadForCollectionResourceIfNotExported() throws Exception {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> {

					controller.headCollectionResource(getResourceInformation(CreditCard.class),
							new DefaultedPageable(Pageable.unpaged(), false));
				});
	}

	@Test // DATAREST-330
	void exposesHeadForItemResourceIfExported() throws Exception {

		Address address = repository.save(new Address());

		ResponseEntity<?> entity = controller.headForItemResource(getResourceInformation(Address.class), address.id,
				assembler);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test // DATAREST-330
	void doesNotExposeHeadForItemResourceIfNotExisting() throws Exception {

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> controller.headForItemResource(getResourceInformation(CreditCard.class), 1L, assembler));
	}

	@Test // DATAREST-333
	void doesNotExposeMethodsForOptionsIfNotHttpMethodsSupportedForCollectionResource() {

		HttpEntity<?> response = controller.optionsForCollectionResource(getResourceInformation(Address.class));
		assertAllowHeaders(response, OPTIONS);
	}

	@Test // DATAREST-333
	void exposesSupportedHttpMethodsInAllowHeaderForOptionsRequestToCollectionResource() {

		HttpEntity<?> response = controller.optionsForCollectionResource(getResourceInformation(Person.class));
		assertAllowHeaders(response, GET, POST, HEAD, OPTIONS);
	}

	@Test // DATAREST-333
	void exposesSupportedHttpMethodsInAllowHeaderForOptionsRequestToItemResource() {

		HttpEntity<?> response = controller.optionsForItemResource(getResourceInformation(Person.class));
		assertAllowHeaders(response, GET, PUT, PATCH, DELETE, HEAD, OPTIONS);
	}

	@Test // DATAREST-333, DATAREST-348
	void optionsForItermResourceSetsAllowPatchHeader() {

		ResponseEntity<?> entity = controller.optionsForItemResource(getResourceInformation(Person.class));

		List<String> value = entity.getHeaders().get("Accept-Patch");

		assertThat(value).hasSize(3);
		assertThat(value).contains(RestMediaTypes.JSON_PATCH_JSON.toString(), RestMediaTypes.MERGE_PATCH_JSON.toString(),
				MediaType.APPLICATION_JSON_VALUE);
	}

	@Test // DATAREST-34
	void returnsBodyOnPutForUpdateIfAcceptHeaderPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		Order order = request.getInvoker().invokeSave(new Order(new Person()));

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getRequiredPersistentEntity(Order.class)).build();

		assertThat(controller.putItemResource(request, persistentEntityResource, order.getId(), assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody()).isTrue();
	}

	@Test // DATAREST-34
	void returnsBodyForCreatingPutIfAcceptHeaderPresentByDefault() throws HttpRequestMethodNotSupportedException {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getRequiredPersistentEntity(Order.class)).forCreation();

		assertThat(controller.putItemResource(request, persistentEntityResource, 1L, assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody()).isTrue();
	}

	@Test // DATAREST-34
	void returnsBodyForPostIfAcceptHeaderIsPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getRequiredPersistentEntity(Order.class)).build();

		assertThat(controller
				.postCollectionResource(request, persistentEntityResource, assembler, MediaType.APPLICATION_JSON_VALUE)
				.hasBody()).isTrue();
	}

	@Test // DATAREST-34
	void doesNotReturnBodyForPostIfNoAcceptHeaderPresentByDefault() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getRequiredPersistentEntity(Order.class)).build();

		assertThat(controller.postCollectionResource(request, persistentEntityResource, assembler, null).hasBody())
				.isFalse();
		assertThat(controller.postCollectionResource(request, persistentEntityResource, assembler, "").hasBody()).isFalse();
	}

	@Test // DATAREST-581
	void createsEtagForProjectedEntityCorrectly() throws Exception {

		Address address = repository.save(new Address());

		PersistentEntityResourceAssembler assembler = Mockito.mock(PersistentEntityResourceAssembler.class);
		AddressProjection addressProjection = new SpelAwareProxyProjectionFactory()
				.createProjection(AddressProjection.class);

		PersistentEntityResource resource = PersistentEntityResource
				.build(addressProjection, entities.getRequiredPersistentEntity(Address.class)).build();

		Mockito.when(assembler.toFullResource(Mockito.any(Object.class))).thenReturn(resource);

		ResponseEntity<EntityModel<?>> entity = controller.getItemResource(getResourceInformation(Address.class),
				address.id, assembler, new HttpHeaders());

		assertThat(entity.getHeaders().getETag()).isNotNull();
	}

	@Test // DATAREST-724
	void deletesEntityWithCustomLookupCorrectly() throws Exception {

		Address address = repository.save(new Address());
		assertThat(repository.findById(address.id)).isNotNull();

		RootResourceInformation resourceInformation = getResourceInformation(Address.class);
		RepositoryInvoker invoker = spy(resourceInformation.getInvoker());
		doReturn(Optional.of(address)).when(invoker).invokeFindById("foo");

		RootResourceInformation informationSpy = Mockito.spy(resourceInformation);
		doReturn(invoker).when(informationSpy).getInvoker();

		controller.deleteItemResource(informationSpy, "foo", ETag.from("0"), assembler, MediaType.ALL_VALUE);

		assertThat(repository.findById(address.id)).isEmpty();
	}

	@Test // DATAREST-948
	void rejectsPutForCreationIfConfigured() throws HttpRequestMethodNotSupportedException {

		RootResourceInformation request = getResourceInformation(Address.class);
		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Address(), entities.getRequiredPersistentEntity(Address.class)).forCreation();

		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class) //
				.isThrownBy(() -> controller.putItemResource(request, persistentEntityResource, 1L, assembler, ETag.NO_ETAG,
						MediaType.APPLICATION_JSON_VALUE));
	}

	@TestFactory // #2225
	Stream<DynamicTest> returnsResponseBodyForDeleteForAcceptHeaderOrConfig() throws Exception {

		record Fixture(String description, Boolean activateReturnBodyOnDelete, String acceptHeader,
				HttpStatus expectedStatusCode) {
			public String description() {
				return description.formatted(expectedStatusCode);
			}
		}

		var fixtures = Stream.of(
				new Fixture("No config, no header -> %s", null, null, HttpStatus.NO_CONTENT),
				new Fixture("No config, but header -> %s", null, MediaType.ALL_VALUE, HttpStatus.OK),
				new Fixture("Enabled, no header -> %s", true, null, HttpStatus.OK),
				new Fixture("Enabled, and header -> %s", true, MediaType.ALL_VALUE, HttpStatus.OK),
				new Fixture("Disabled, no header -> %s", false, null, HttpStatus.NO_CONTENT),
				new Fixture("Disabled, and header -> %s", false, MediaType.ALL_VALUE, HttpStatus.NO_CONTENT));

		return DynamicTest.stream(fixtures, Fixture::description, it -> {

			try {

				// Apply configuration
				configuration.setReturnBodyOnDelete(it.activateReturnBodyOnDelete());

				var address = repository.save(new Address());
				var response = controller.deleteItemResource(getResourceInformation(Address.class), address.id,
						ETag.NO_ETAG, assembler, it.acceptHeader());

				assertThat(response.getStatusCode()).isEqualTo(it.expectedStatusCode());

			} finally {

				// Reset configuration
				configuration.setReturnBodyOnDelete(null);
			}
		});
	}

	interface AddressProjection {}
}
