/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;
import org.springframework.data.rest.webmvc.jpa.AddressRepository;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.Type;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link RepositoryRestExceptionHandler}.
 * 
 * @author Oliver Gierke
 * @author Jeremy Rickard
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@Transactional
@ContextConfiguration(classes = { JpaRepositoryConfig.class, RepositoryRestExceptionHandlerIntegrationTests.class })
public class RepositoryRestExceptionHandlerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired RepositoryEntityController controller;
	@Autowired AddressRepository repository;
	@Autowired RepositoryRestConfiguration configuration;
	@Autowired PersistentEntityResourceAssembler assembler;
	@Autowired PersistentEntities entities;

	@Bean
	OrderEventHandler personEventHandler() {
		return new OrderEventHandler();
	}

	@Bean
	OrderEventListener beforeSaveEventListener() {
		return new OrderEventListener();
	}
	
	/**
	 * @see DATAREST-755
	 */
	@Test(expected = ResourceForbiddenException.class)
	public void rejectOrdersTakeAway() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		Order order = request.getInvoker().invokeSave(new Order(new Person()));
		order.setType(Type.TAKE_AWAY);

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller.putItemResource(request, persistentEntityResource, order.getId(), assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody(), is(true));
	}
	
	/**
	 * @see DATAREST-755
	 */
	@Test(expected = ResourceForbiddenException.class)
	public void rejectOrdersInStore() throws Exception {

		RootResourceInformation request = getResourceInformation(Order.class);
		Order order = request.getInvoker().invokeSave(new Order(new Person()));
		order.setType(Type.IN_STORE);

		PersistentEntityResource persistentEntityResource = PersistentEntityResource
				.build(new Order(new Person()), entities.getPersistentEntity(Order.class)).build();

		assertThat(controller.putItemResource(request, persistentEntityResource, order.getId(), assembler, ETag.NO_ETAG,
				MediaType.APPLICATION_JSON_VALUE).hasBody(), is(true));
	}
	
	/**
	 * An <code>EventHandler</code> that rejects <code>Order</code>s of type TAKE_AWAY.
	 */
	@RepositoryEventHandler
	public class OrderEventHandler {

		@HandleBeforeSave
		public void handlePersonSave(Order order) {

			if (Type.TAKE_AWAY.equals(order.getType())) {
				throw new ResourceForbiddenException();
			}

		}

	}
	
	/**
	 * An <code>AbstractRepositoryEventListener</code> that rejects <code>Order</code>s of type IN_STORE.
	 *
	 */
	public class OrderEventListener extends AbstractRepositoryEventListener<Order> {

		@Override
		public void onBeforeSave(Order order) {

			if (Type.IN_STORE.equals(order.getType())) {
				throw new ResourceForbiddenException();
			}

		}

	}
	
}