/*
 * Copyright 2012-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticMessageSource;
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
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.hal.HalLinkDiscoverer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for entity (de)serialization.
 *
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MongoDbRepositoryConfig.class, RepositoryTestsConfig.class,
		PersistentEntitySerializationTests.TestConfig.class })
public class PersistentEntitySerializationTests {

	@Autowired ObjectMapper mapper;
	@Autowired Repositories repositories;
	@Autowired UserRepository users;

	@Configuration
	static class TestConfig extends RepositoryTestsConfig {

		@Bean
		@Override
		public ObjectMapper objectMapper() {

			ObjectMapper objectMapper = super.objectMapper();
			objectMapper.registerModule(
					new JacksonSerializers(new EnumTranslator(new MessageSourceAccessor(new StaticMessageSource()))));
			return objectMapper;
		}
	}

	LinkDiscoverer linkDiscoverer;
	ProjectionFactory projectionFactory;

	@Before
	public void setUp() {

		RequestContextHolder.setRequestAttributes(new ServletWebRequest(new MockHttpServletRequest()));

		this.linkDiscoverer = new HalLinkDiscoverer();
		this.projectionFactory = new SpelAwareProxyProjectionFactory();
	}

	@Test // DATAREST-250
	public void serializesEmbeddedReferencesCorrectly() throws Exception {

		User user = new User();
		user.address = new Address();
		user.address.street = "Street";

		PersistentEntityResource userResource = PersistentEntityResource.//
				build(user, repositories.getPersistentEntity(User.class)).//
				withLink(new Link("/users/1")).//
				build();

		PagedResources<PersistentEntityResource> persistentEntityResource = new PagedResources<PersistentEntityResource>(
				Arrays.asList(userResource), new PageMetadata(1, 0, 10));

		String result = mapper.writeValueAsString(persistentEntityResource);

		assertThat(JsonPath.<Object> read(result, "$._embedded.users[*].address")).isNotNull();
	}

	@Test // DATAREST-654
	public void deserializesTranslatedEnumProperty() throws Exception {
		assertThat(mapper.readValue("{ \"gender\" : \"Male\" }", User.class).gender).isEqualTo(Gender.MALE);
	}

	@Test // DATAREST-864
	public void createsNestedResourceForMap() throws Exception {

		User dave = users.save(new User());
		dave.colleaguesMap = new HashMap<String, Nested>();
		dave.colleaguesMap.put("carter", new Nested(users.save(new User())));

		PersistentEntityResource resource = PersistentEntityResource
				.build(dave, repositories.getPersistentEntity(User.class)).build();

		assertThat(JsonPath.parse(mapper.writeValueAsString(resource)).read("$.colleaguesMap.carter._links.user.href",
				String.class), is(notNullValue()));
	}
}
