/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Series of tests to verify {@link ProfileController} serves ALPS and JSON Schema metadata from the root level and
 * at collection resource levels.
 *
 * @author Greg Turnquist
 * @since 2.4
 */
@WebAppConfiguration
@ContextConfiguration(classes = { JpaRepositoryConfig.class, ProfileIntegrationTests.Config.class})
public class ProfileIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	private static final String ROOT_URI = "/api";

	@Configuration
	static class Config extends RepositoryRestConfigurerAdapter {

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.setBasePath(ROOT_URI);
		}
	}

	TestMvcClient client;

	@Before
	public void setUp() {

		MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
		this.client = new TestMvcClient(mvc, this.discoverers);
	}

	/**
	 * @see DATAREST-230
	 * @see DATAREST-638
	 */
	@Test
	public void exposesProfileLink() throws Exception {

		client.follow(ROOT_URI)//
				.andExpect(status().is2xxSuccessful())//
				.andExpect(jsonPath("$._links.profile.href", endsWith(ProfileController.PROFILE_ROOT_MAPPING)));
	}

	/**
	 * @see DATAREST-230
	 * @see DATAREST-638
	 */
	@Test
	public void profileRootLinkContainsMetadataForEachRepo() throws Exception {

		Link profileLink = client.discoverUnique(new Link(ROOT_URI), ProfileResourceProcessor.PROFILE_REL);

		assertThat(client.discoverUnique(profileLink, "self", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "people", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "items", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "authors", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "books", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "orders", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "receipts", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "addresses", MediaType.ALL), is(notNullValue()));
	}



	/**
	 * @see DATAREST-638
	 */
	@Test
	public void profileLinkOnCollectionResourceLeadsToRepositorySpecificMetadata() throws Exception {

		Link peopleLink = client.discoverUnique(new Link(ROOT_URI), "people");
		Link profileLink = client.discoverUnique(peopleLink, ProfileResourceProcessor.PROFILE_REL);

		client.follow(profileLink, RestMediaTypes.ALPS_JSON)
			.andExpect(status().is2xxSuccessful())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, RestMediaTypes.ALPS_JSON_VALUE));

		client.follow(profileLink, RestMediaTypes.SCHEMA_JSON)
			.andExpect(status().is2xxSuccessful())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, RestMediaTypes.SCHEMA_JSON_VALUE));
	}

}
