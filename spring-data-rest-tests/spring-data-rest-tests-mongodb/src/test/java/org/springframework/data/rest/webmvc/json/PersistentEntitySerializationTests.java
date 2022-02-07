/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.tests.RepositoryTestsConfig;
import org.springframework.data.rest.tests.mongodb.Address;
import org.springframework.data.rest.tests.mongodb.MongoDbRepositoryConfig;
import org.springframework.data.rest.tests.mongodb.User;
import org.springframework.data.rest.tests.mongodb.User.Gender;
import org.springframework.data.rest.tests.mongodb.User.Nested;
import org.springframework.data.rest.tests.mongodb.UserRepository;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

/**
 * Integration tests for entity (de)serialization.
 *
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MongoDbRepositoryConfig.class, RepositoryTestsConfig.class,
		PersistentEntitySerializationTests.TestConfig.class })
class PersistentEntitySerializationTests {

	@Autowired ObjectMapper mapper;
	@Autowired Repositories repositories;
	@Autowired UserRepository users;

	@Configuration
	static class TestConfig extends RepositoryTestsConfig {

		@Bean
		@Override
		public ObjectMapper objectMapper() {

			ObjectMapper objectMapper = super.objectMapper();
			objectMapper.registerModule(new JacksonSerializers(new EnumTranslator(MessageResolver.DEFAULTS_ONLY)));

			return objectMapper;
		}
	}

	LinkDiscoverer linkDiscoverer;
	ProjectionFactory projectionFactory;

	@BeforeEach
	void setUp() {

		RequestContextHolder.setRequestAttributes(new ServletWebRequest(new MockHttpServletRequest()));

		this.linkDiscoverer = new HalLinkDiscoverer();
		this.projectionFactory = new SpelAwareProxyProjectionFactory();
	}

	@Test // DATAREST-250
	void serializesEmbeddedReferencesCorrectly() throws Exception {

		User user = new User();
		user.address = new Address();
		user.address.street = "Street";

		PersistentEntityResource userResource = PersistentEntityResource.//
				build(user, repositories.getPersistentEntity(User.class)).//
				withLink(Link.of("/users/1")).//
				build();

		PagedModel<PersistentEntityResource> persistentEntityResource = PagedModel.of(
				Arrays.asList(userResource), new PageMetadata(1, 0, 10));

		String result = mapper.writeValueAsString(persistentEntityResource);

		assertThat(JsonPath.<Object> read(result, "$._embedded.users[*].address")).isNotNull();
	}

	@Test // DATAREST-654
	void deserializesTranslatedEnumProperty() throws Exception {
		assertThat(mapper.readValue("{ \"gender\" : \"Male\" }", User.class).gender).isEqualTo(Gender.MALE);
	}

	@Test // DATAREST-864
	void createsNestedResourceForMap() throws Exception {

		User dave = users.save(new User());
		dave.colleaguesMap = new HashMap<String, Nested>();
		dave.colleaguesMap.put("carter", new Nested(users.save(new User())));

		PersistentEntityResource resource = PersistentEntityResource
				.build(dave, repositories.getPersistentEntity(User.class)).build();

		String result = mapper.writeValueAsString(resource);
		ReadContext document = JsonPath.parse(result);

		assertThat(document.read("$.colleaguesMap.carter._links.user.href", String.class)).isNotNull();
	}
}
