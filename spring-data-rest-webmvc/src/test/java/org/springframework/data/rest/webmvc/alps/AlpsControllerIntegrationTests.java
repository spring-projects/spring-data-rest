/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.alps;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.jpa.Item;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.core.JsonPathLinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for {@link AlpsController}.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@WebAppConfiguration
@ContextConfiguration(classes = { JpaRepositoryConfig.class, AlpsControllerIntegrationTests.Config.class },
		inheritLocations = false)
public class AlpsControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	@Configuration
	static class Config extends RepositoryRestMvcConfiguration {

		@Bean
		public LinkDiscoverer alpsLinkDiscoverer() {
			return new JsonPathLinkDiscoverer("$.descriptors[?(@.name == '%s')].href",
					MediaType.valueOf("application/alps+json"));
		}

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.exposeIdsFor(Item.class);
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
	 */
	@Test
	public void exposesProfileLink() throws Exception {

		client.follow("/")//
				.andExpect(status().is2xxSuccessful())//
				.andExpect(jsonPath("$._links.profile.href", endsWith(AlpsController.ALPS_ROOT_MAPPING)));
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void alpsResourceExposesResourcePerCollectionResource() throws Exception {

		Link profileLink = client.discoverUnique("profile");

		assertThat(client.discoverUnique(profileLink, "orders", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "people", MediaType.ALL), is(notNullValue()));
		assertThat(client.discoverUnique(profileLink, "items", MediaType.ALL), is(notNullValue()));
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void exposesAlpsCollectionResources() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link peopleLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		client.follow(peopleLink)//
				.andExpect(jsonPath("$.version").value("1.0"))//
				.andExpect(jsonPath("$.descriptors[*].name", hasItems("people", "person")));
	}

	/**
	 * @see DATAREST-463
	 */
	@Test
	public void verifyThatAttributesIgnoredDontAppearInAlps() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);
		client.follow(itemsLink)//

				// Exposes standard property
				.andExpect(jsonPath("$.descriptors[*].descriptors[*].name", hasItems("name")))
				// Does not expose explicitly @JsonIgnored property
				.andExpect(jsonPath("$.descriptors[*].descriptors[*].name", not(hasItems("owner"))))
				// Does not expose properties pointing to non exposed types
				.andExpect(jsonPath("$.descriptors[*].descriptors[*].name", not(hasItems("manager", "curator"))));
	}

	/**
	 * @see DATAREST-494
	 */
	@Test
	public void linksToJsonSchemaFromRepresentationDescriptor() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);

		assertThat(itemsLink, is(notNullValue()));

		client.follow(itemsLink)//
				.andExpect(jsonPath("$.descriptors[?(@.id == 'item-representation')].href", is(notNullValue())));
	}

	/**
	 * @see DATAREST-516
	 */
	@Test
	public void referenceToAssociatedEntityDesciptorPointsToRepresentationDescriptor() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link usersLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		String jsonPath = "$."; // Root
		jsonPath += "descriptors[?(@.id == 'person-representation')]."; // Representation descriptor
		jsonPath += "descriptors[?(@.name == 'father')][0]."; // First father descriptor
		jsonPath += "rt"; // Return type

		client.follow(usersLink)//
				.andExpect(jsonPath(jsonPath, allOf(containsString("alps"), endsWith("-representation"))));
	}

	/**
	 * @see DATAREST-630
	 */
	@Test
	public void onlyExposesIdAttributesWhenExposedInTheConfiguration() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);

		client.follow(itemsLink)//
				// Exposes identifier if configured to
				.andExpect(jsonPath("$.descriptors[*].descriptors[*].name", hasItems("id", "name")));
	}
}
