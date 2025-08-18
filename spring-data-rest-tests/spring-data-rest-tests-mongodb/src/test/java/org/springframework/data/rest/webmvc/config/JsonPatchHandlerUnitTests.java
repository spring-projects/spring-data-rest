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
package org.springframework.data.rest.webmvc.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.tests.mongodb.TestUtils.*;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.tests.mongodb.Address;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.json.BindContextFactory;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.json.PersistentEntitiesBindContextFactory;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Unit tests for {@link JsonPatchHandler}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class JsonPatchHandlerUnitTests {

	JsonPatchHandler handler;
	ObjectMapper mapper = new ObjectMapper();
	User user;

	@Mock ResourceMappings mappings;

	@BeforeEach
	void setUp() {

		MongoCustomConversions conversions = new MongoCustomConversions(Collections.emptyList());

		MongoMappingContext context = new MongoMappingContext();
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		context.getPersistentEntity(User.class);
		context.getPersistentEntity(WithIgnoredProperties.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(context));
		Associations associations = new Associations(mappings, mock(RepositoryRestConfiguration.class));
		BindContextFactory factory = new PersistentEntitiesBindContextFactory(entities,
				DefaultConversionService.getSharedInstance());

		this.handler = new JsonPatchHandler(factory, new DomainObjectReader(entities, associations));

		Address address = new Address();
		address.street = "Foo";
		address.zipCode = "Bar";

		this.user = new User();
		this.user.firstname = "Oliver";
		this.user.lastname = "Gierke";
		this.user.address = address;
	}

	@Test // DATAREST-348
	void appliesRemoveOperationCorrectly() throws Exception {

		String input = "[{ \"op\": \"replace\", \"path\": \"/address/zipCode\", \"value\": \"ZIP\" },"
				+ "{ \"op\": \"remove\", \"path\": \"/lastname\" }]";

		User result = handler.applyPatch(asStream(input), user, mapper);

		assertThat(result.lastname).isNull();
		assertThat(result.address.zipCode).isEqualTo("ZIP");
	}

	@Test // DATAREST-348
	void appliesMergePatchCorrectly() throws Exception {

		String input = "{ \"address\" : { \"zipCode\" : \"ZIP\"}, \"lastname\" : null }";

		User result = handler.applyMergePatch(asStream(input), user, mapper);

		assertThat(result.lastname).isNull();
		assertThat(result.address.zipCode).isEqualTo("ZIP");
	}

	/**
	 * DATAREST-537
	 */
	@Test
	void removesArrayItemCorrectly() throws Exception {

		User thomas = new User();
		thomas.firstname = "Thomas";

		User christoph = new User();
		christoph.firstname = "Christoph";

		this.user.colleagues = new ArrayList<User>(Arrays.asList(thomas, christoph));

		String input = "[{ \"op\": \"remove\", \"path\": \"/colleagues/0\" }]";

		handler.applyPatch(asStream(input), user, mapper);

		assertThat(user.colleagues).hasSize(1);
		assertThat(user.colleagues.get(0).firstname).isEqualTo(christoph.firstname);
	}

	@Test // DATAREST-609
	void hintsToMediaTypeIfBodyCantBeRead() throws Exception {

		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> handler.applyPatch(asStream("{ \"foo\" : \"bar\" }"), new User(), mapper))
				.withMessageContaining(RestMediaTypes.JSON_PATCH_JSON.toString());
	}

	@Test
	void skipsReplaceConditionally() throws Exception {

		WithIgnoredProperties object = new WithIgnoredProperties();
		assertThatExceptionOfType(PatchException.class).isThrownBy(() -> {
			handler.applyPatch(asStream("[{ \"op\": \"replace\", \"path\": \"/password\", \"value\": \"hello\" }]"), object,
					mapper);
		});

		WithIgnoredProperties result = handler
				.applyPatch(asStream("[{ \"op\": \"replace\", \"path\": \"/name\", \"value\": \"hello\" }]"), object, mapper);

		assertThat(result.name).isEqualTo("hello");
	}

	@Test
	void skipsCopyConditionally() throws Exception {

		WithIgnoredProperties object = new WithIgnoredProperties();
		object.setName("hello");

		assertThatExceptionOfType(PatchException.class).isThrownBy(() -> {
			handler.applyPatch(asStream("[{ \"op\": \"copy\", \"path\": \"/password\", \"from\": \"/name\" }]"), object,
					mapper);
		});

		WithIgnoredProperties result = handler
				.applyPatch(asStream("[{ \"op\": \"copy\", \"path\": \"/lastname\", \"from\": \"/name\" }]"), object, mapper);

		assertThat(result.lastname).isEqualTo("hello");
	}

	@JsonIgnoreProperties("password")
	static class WithIgnoredProperties {

		String name, lastname, password;

		@JsonIgnore String ssn;

		public String getName() {
			return this.name;
		}

		public String getLastname() {
			return this.lastname;
		}

		public String getPassword() {
			return this.password;
		}

		public String getSsn() {
			return this.ssn;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@JsonIgnore
		public void setSsn(String ssn) {
			this.ssn = ssn;
		}
	}
}
