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
package org.springframework.data.rest.webmvc.json;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.rest.core.projection.ProjectionFactory;
import org.springframework.data.rest.core.projection.ProxyProjectionFactory;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.hal.Jackson2HalModule.HalHandlerInstantiator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for Jackson marshalling of projected objects.
 * 
 * @author Oliver Gierke
 */
public class ProjectionJacksonIntegrationTests {

	ObjectMapper mapper;
	ProjectionFactory factory = new ProxyProjectionFactory(null);

	@Before
	public void setUp() {

		this.mapper = new ObjectMapper();
		this.mapper.registerModule(new Jackson2HalModule());
		this.mapper.setHandlerInstantiator(new HalHandlerInstantiator(new EvoInflectorRelProvider(), null));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void considersJacksonAnnotationsOnProjectionInterfaces() throws Exception {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";
		customer.address = new Address();

		CustomerProjection projection = factory.createProjection(customer, CustomerProjection.class);

		String result = mapper.writeValueAsString(projection);
		assertThat(JsonPath.read(result, "$firstname"), is((Object) "Dave"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void rendersHalContentCorrectly() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2HalModule());
		mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(new EvoInflectorRelProvider(), null));

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";
		customer.address = new Address();

		CustomerProjection projection = factory.createProjection(customer, CustomerProjection.class);
		Resources<CustomerProjection> resources = new Resources<CustomerProjection>(Arrays.asList(projection));

		String result = mapper.writeValueAsString(resources);

		assertThat(JsonPath.read(result, "$_embedded.customers[0].firstname"), is((Object) "Dave"));
	}

	static class Customer {
		String firstname, lastname;
		Address address;
	}

	static class Address {

	}

	interface CustomerProjection {

		String getFirstname();

		@JsonIgnore
		String getLastname();
	}
}
