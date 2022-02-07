/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.rest.webmvc.alps;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import net.minidev.json.JSONArray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.AbstractControllerIntegrationTests;
import org.springframework.data.rest.tests.TestMvcClient;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.jpa.Item;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.JsonPathLinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for {@link AlpsController}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@WebAppConfiguration
@ContextConfiguration(classes = { JpaRepositoryConfig.class, AlpsControllerIntegrationTests.Config.class })
class AlpsControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;
	@Autowired RepositoryRestConfiguration configuration;

	@Configuration
	static class Config {

		@Bean
		public LinkDiscoverer alpsLinkDiscoverer() {
			return new JsonPathLinkDiscoverer("$.descriptor[?(@.name == '%s')].href",
					MediaType.valueOf("application/alps+json"));
		}

		@Bean
		RepositoryRestConfigurer configurer() {
			return RepositoryRestConfigurer.withConfig(config -> config.exposeIdsFor(Item.class));
		}
	}

	TestMvcClient client;

	@BeforeEach
	void setUp() {

		MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
		this.client = new TestMvcClient(mvc, this.discoverers);
	}

	@AfterEach
	void tearDown() {
		configuration.setEnableEnumTranslation(false);
	}

	@Test // DATAREST-230
	void exposesAlpsCollectionResources() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link peopleLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		client.follow(peopleLink, MediaTypes.ALPS_JSON)//
				.andExpect(jsonPath("$.alps.version").value("1.0"))//
				.andExpect(jsonPath("$.alps.descriptor[*].name", hasItems("people", "person")));
	}

	@Test // DATAREST-638
	void verifyThatAlpsIsDefaultProfileFormat() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link peopleLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		client.follow(peopleLink)//
				.andExpect(jsonPath("$.alps.version").value("1.0"))//
				.andExpect(jsonPath("$.alps.descriptor[*].name", hasItems("people", "person")));

	}

	@Test // DATAREST-463
	void verifyThatAttributesIgnoredDontAppearInAlps() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);

		client.follow(itemsLink, MediaTypes.ALPS_JSON)//
				// Exposes standard property
				.andExpect(jsonPath("$.alps.descriptor[*].descriptor[*].name", hasItems("name")))
				// Does not expose explicitly @JsonIgnored property
				.andExpect(jsonPath("$.alps.descriptor[*].descriptor[*].name", not(hasItems("owner"))))
				// Does not expose properties pointing to non exposed types
				.andExpect(jsonPath("$.alps.descriptor[*].descriptor[*].name", not(hasItems("manager", "curator"))));
	}

	@Test // DATAREST-494
	void linksToJsonSchemaFromRepresentationDescriptor() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);

		assertThat(itemsLink).isNotNull();

		String result = client.follow(itemsLink, MediaTypes.ALPS_JSON).andReturn().getResponse().getContentAsString();
		String href = JsonPath.<JSONArray> read(result, "$.alps.descriptor[?(@.id == 'item-representation')].href").get(0)
				.toString();

		assertThat(href).endsWith("/profile/items");
	}

	@Test // DATAREST-516
	void referenceToAssociatedEntityDesciptorPointsToRepresentationDescriptor() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link usersLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		String jsonPath = "$.alps."; // Root
		jsonPath += "descriptor[?(@.id == 'person-representation')]."; // Representation descriptor
		jsonPath += "descriptor[?(@.name == 'father')]."; // First father descriptor
		jsonPath += "rt"; // Return type

		String result = client.follow(usersLink, MediaTypes.ALPS_JSON).andReturn().getResponse().getContentAsString();
		String rt = JsonPath.<JSONArray> read(result, jsonPath).get(0).toString();

		assertThat(rt).contains(ProfileController.PROFILE_ROOT_MAPPING).endsWith("-representation");
	}

	@Test // DATAREST-630
	void onlyExposesIdAttributesWhenExposedInTheConfiguration() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link itemsLink = client.discoverUnique(profileLink, "items", MediaType.ALL);

		client.follow(itemsLink, MediaTypes.ALPS_JSON)//
				// Exposes identifier if configured to
				.andExpect(jsonPath("$.alps.descriptor[*].descriptor[*].name", hasItems("id", "name")));
	}

	@Test // DATAREST-683
	void enumValueListingsAreTranslatedIfEnabled() throws Exception {

		configuration.setEnableEnumTranslation(true);

		Link profileLink = client.discoverUnique("profile");
		Link peopleLink = client.discoverUnique(profileLink, "people", MediaType.ALL);

		String result = client.follow(peopleLink).andReturn().getResponse().getContentAsString();

		String value = JsonPath
				.<JSONArray> read(result,
						"$.alps.descriptor[?(@.id == 'person-representation')].descriptor[?(@.name == 'gender')].doc.value")
				.get(0).toString();

		assertThat(value).isEqualTo("Male, Female, Undefined");
	}

	@Test // DATAREST-753
	void alpsCanHandleGroovyDomainObjects() throws Exception {

		Link profileLink = client.discoverUnique("profile");
		Link groovyDomainObjectLink = client.discoverUnique(profileLink, "simulatedGroovyDomainClasses");

		String result = client.follow(groovyDomainObjectLink).andReturn().getResponse().getContentAsString();

		String name = JsonPath
				.<JSONArray> read(result,
						"$.alps.descriptor[?(@.id == 'simulatedGroovyDomainClass-representation')].descriptor[0].name")
				.get(0).toString();

		assertThat(name).isEqualTo("name");
	}
}
