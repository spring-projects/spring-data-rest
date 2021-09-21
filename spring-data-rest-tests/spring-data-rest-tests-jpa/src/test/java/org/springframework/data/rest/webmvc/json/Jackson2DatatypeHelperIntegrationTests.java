/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link Jackson2DatatypeHelper}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { JpaRepositoryConfig.class, RepositoryRestMvcConfiguration.class })
@Transactional
class Jackson2DatatypeHelperIntegrationTests {

	@Autowired PersistentEntities entities;
	@Autowired ApplicationContext context;

	@Autowired PersonRepository people;
	@Autowired OrderRepository orders;
	@Autowired EntityManager em;

	ObjectMapper objectMapper;

	Order order;

	@BeforeEach
	void setUp() {

		this.order = orders.save(new Order(people.save(new Person("Dave", "Matthews"))));
		this.objectMapper = context.getBean("halJacksonHttpMessageConverter", AbstractJackson2HttpMessageConverter.class)
				.getObjectMapper();

		// Reset JPA to make sure the query returns a result with proxy references
		em.flush();
		em.clear();
	}

	@Test // DATAREST-500
	void configuresHibernate4ModuleToLoadLazyLoadingProxies() throws Exception {

		PersistentEntity<?, ?> entity = entities.getRequiredPersistentEntity(Order.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("creator");
		PersistentPropertyAccessor<?> accessor = entity
				.getPropertyAccessor(orders.findById(this.order.getId()).orElse(null));

		assertThat(objectMapper.writeValueAsString(accessor.getProperty(property))).isNotEqualTo("null");
	}
}
