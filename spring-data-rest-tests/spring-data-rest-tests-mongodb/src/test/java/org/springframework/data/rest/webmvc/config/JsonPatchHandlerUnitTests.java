/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.tests.mongodb.TestUtils.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.tests.mongodb.Address;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.http.converter.HttpMessageNotReadableException;

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
	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {

		MongoMappingContext context = new MongoMappingContext();
		context.getPersistentEntity(User.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(context));

		Associations associations = new Associations(mappings, mock(RepositoryRestConfiguration.class));

		this.handler = new JsonPatchHandler(new ObjectMapper(), new DomainObjectReader(entities, associations));

		Address address = new Address();
		address.street = "Foo";
		address.zipCode = "Bar";

		this.user = new User();
		this.user.firstname = "Oliver";
		this.user.lastname = "Gierke";
		this.user.address = address;
	}

	@Test // DATAREST-348
	public void appliesRemoveOperationCorrectly() throws Exception {

		String input = "[{ \"op\": \"replace\", \"path\": \"/address/zipCode\", \"value\": \"ZIP\" },"
				+ "{ \"op\": \"remove\", \"path\": \"/lastname\" }]";

		User result = handler.applyPatch(asStream(input), user);

		assertThat(result.lastname).isNull();
		assertThat(result.address.zipCode).isEqualTo("ZIP");
	}

	@Test // DATAREST-348
	public void appliesMergePatchCorrectly() throws Exception {

		String input = "{ \"address\" : { \"zipCode\" : \"ZIP\"}, \"lastname\" : null }";

		User result = handler.applyMergePatch(asStream(input), user);

		assertThat(result.lastname).isNull();
		assertThat(result.address.zipCode).isEqualTo("ZIP");
	}

	/**
	 * DATAREST-537
	 */
	@Test
	public void removesArrayItemCorrectly() throws Exception {

		User thomas = new User();
		thomas.firstname = "Thomas";

		User christoph = new User();
		christoph.firstname = "Christoph";

		this.user.colleagues = new ArrayList<User>(Arrays.asList(thomas, christoph));

		String input = "[{ \"op\": \"remove\", \"path\": \"/colleagues/0\" }]";

		handler.applyPatch(asStream(input), user);

		assertThat(user.colleagues).hasSize(1);
		assertThat(user.colleagues.get(0).firstname).isEqualTo(christoph.firstname);
	}

	@Test // DATAREST-609
	public void hintsToMediaTypeIfBodyCantBeRead() throws Exception {

		exception.expect(HttpMessageNotReadableException.class);
		exception.expectMessage(RestMediaTypes.JSON_PATCH_JSON.toString());

		handler.applyPatch(asStream("{ \"foo\" : \"bar\" }"), new User());
	}
}
