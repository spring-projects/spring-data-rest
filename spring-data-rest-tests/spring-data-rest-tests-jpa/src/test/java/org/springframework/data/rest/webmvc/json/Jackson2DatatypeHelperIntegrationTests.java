/*
 * Copyright 2015-2017 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Order;
import org.springframework.data.rest.webmvc.jpa.OrderRepository;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link Jackson2DatatypeHelper}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JpaRepositoryConfig.class, RepositoryRestMvcConfiguration.class })
@Transactional
public class Jackson2DatatypeHelperIntegrationTests {

	@Autowired PersistentEntities entities;
	@Autowired ObjectMapper objectMapper;

	@Autowired PersonRepository people;
	@Autowired OrderRepository orders;
	@Autowired EntityManager em;

	Order order;

	@Before
	public void setUp() {

		this.order = orders.save(new Order(people.save(new Person("Dave", "Matthews"))));

		// Reset JPA to make sure the query returns a result with proxy references
		em.flush();
		em.clear();
	}

	@Test // DATAREST-500
	public void configuresHIbernate4ModuleToLoadLazyLoadingProxies() throws Exception {

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(Order.class);
		PersistentProperty<?> property = entity.getPersistentProperty("creator");
		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(orders.findOne(this.order.getId()));

		assertThat(objectMapper.writeValueAsString(accessor.getProperty(property)), is(not("null")));
	}
}
