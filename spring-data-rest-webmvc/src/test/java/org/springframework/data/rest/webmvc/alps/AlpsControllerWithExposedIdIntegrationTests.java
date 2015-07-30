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
package org.springframework.data.rest.webmvc.alps;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.jpa.Item;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.core.JsonPathLinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Additional integration tests for {@link AlpsController} with a different {@link RepositoryRestConfiguration} via the
 * {@link RepositoryRestConfigurerAdapter}.
 *
 * @author Greg Turnquist
 */
@WebAppConfiguration
@ContextConfiguration(classes = { JpaRepositoryConfig.class, AlpsControllerWithExposedIdIntegrationTests.Config.class })
public class AlpsControllerWithExposedIdIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	@Configuration
	static class Config extends RepositoryRestConfigurerAdapter {

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

	protected MockMvc mvc;

	private TestMvcClient testMvcClient;

	@Before
	public void setUp() {

		this.mvc = MockMvcBuilders.webAppContextSetup(context).build();
		this.testMvcClient = new TestMvcClient(this.mvc, this.discoverers);
	}

	/**
	 * @see DATAREST-630
	 */
	@Test
	public void verifyThatIdAttributesAreOnlyShownWhenExposedInTheConfiguration() throws Exception {

		Link profileLink = discoverUnique("/", "profile");
		Link usersLink = discoverUnique(profileLink.getHref(), "users");
		Link itemsLink = discoverUnique(profileLink.getHref(), "items");

		assertThat(usersLink, is(nullValue()));

		mvc.perform(get(itemsLink.getHref()))
				.andExpect(jsonPath("$.descriptors[*].descriptors[*].name", hasItems("id", "name")))
				.andExpect(
						jsonPath("$.descriptors[*].descriptors[*].name", everyItem(not(isIn(new String[] { "owner", "manager",
								"curator" })))));
	}

	/**
	 * TODO: Switch to {@link TestMvcClient#discoverUnique(String)}
	 */
	private Link discoverUnique(String href, String rel) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(href)).//
				andExpect(status().is2xxSuccessful()).//
				andReturn().getResponse();

		LinkDiscoverer discoverer = discoverers.getLinkDiscovererFor(MediaType.valueOf(response.getContentType()));
		return discoverer.findLinkWithRel(rel, response.getContentAsString());
	}

}
