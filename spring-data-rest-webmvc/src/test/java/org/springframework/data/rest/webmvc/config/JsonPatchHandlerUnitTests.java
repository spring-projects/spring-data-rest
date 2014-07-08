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
package org.springframework.data.rest.webmvc.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.webmvc.util.TestUtils.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.mongodb.Address;
import org.springframework.data.rest.webmvc.mongodb.User;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonPatchHandler}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonPatchHandlerUnitTests {

	JsonPatchHandler handler;
	User user;

	@Mock ResourceMappings mappings;

	@Before
	public void setUp() {

		MongoMappingContext context = new MongoMappingContext();
		context.getPersistentEntity(User.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(context));

		this.handler = new JsonPatchHandler(new ObjectMapper(), new DomainObjectReader(entities, mappings));

		Address address = new Address();
		address.street = "Foo";
		address.zipCode = "Bar";

		this.user = new User();
		this.user.firstname = "Oliver";
		this.user.lastname = "Gierke";
		this.user.address = address;
	}

	/**
	 * @see DATAREST-348
	 */
	@Test
	public void appliesRemoveOperationCorrectly() throws Exception {

		String input = "[{ \"op\": \"replace\", \"path\": \"/address/zipCode\", \"value\": \"ZIP\" },"
				+ "{ \"op\": \"remove\", \"path\": \"/lastname\" }]";

		User result = handler.applyPatch(asStream(input), user);

		assertThat(result.lastname, is(nullValue()));
		assertThat(result.address.zipCode, is("ZIP"));
	}

	/**
	 * @see DATAREST-348
	 */
	@Test
	public void appliesMergePatchCorrectly() throws Exception {

		String input = "{ \"address\" : { \"zipCode\" : \"ZIP\"}, \"lastname\" : null }";

		User result = handler.applyMergePatch(asStream(input), user);

		assertThat(result.lastname, is(nullValue()));
		assertThat(result.address.zipCode, is("ZIP"));
	}
}
