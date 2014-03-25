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
package org.springframework.data.rest.webmvc.alps;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.AbstractControllerIntegrationTests;
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
 * Integration tests for {@link AlpsController}.
 * 
 * @author Oliver Gierke
 */
@WebAppConfiguration
@ContextConfiguration(classes = { JpaRepositoryConfig.class, AlpsControllerIntegrationTests.Config.class })
public class AlpsControllerIntegrationTests extends AbstractControllerIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	@Configuration
	static class Config {

		@Bean
		public LinkDiscoverer alpsLinkDiscoverer() {
			return new JsonPathLinkDiscoverer("$.descriptors[?(@.name == '%s')].href",
					MediaType.valueOf("application/alps+json"));
		}
	}

	protected MockMvc mvc;

	@Before
	public void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void exposesProfileLink() throws Exception {

		mvc.perform(get("/")).//
				andExpect(status().is2xxSuccessful()).//
				andExpect(jsonPath("$._links.profile.href", endsWith(AlpsController.ALPS_ROOT_MAPPING)));
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void alpsResourceExposesResourcePerCollectionResource() throws Exception {

		Link profileLink = discoverUnique("/", "profile");

		assertThat(discoverUnique(profileLink.getHref(), "orders"), is(notNullValue()));
		assertThat(discoverUnique(profileLink.getHref(), "people"), is(notNullValue()));
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void exposesAlpsCollectionResources() throws Exception {

		Link profileLink = discoverUnique("/", "profile");
		Link peopleLink = discoverUnique(profileLink.getHref(), "people");

		mvc.perform(get(peopleLink.getHref())).//
				andDo(print()).//
				andExpect(jsonPath("$.version").value("1.0")).//
				andExpect(jsonPath("$.descriptors[*].name", hasItems("people", "person")));
	}

	private Link discoverUnique(String href, String rel) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(href)).//
				andExpect(status().is2xxSuccessful()).//
				andReturn().getResponse();

		LinkDiscoverer discoverer = discoverers.getLinkDiscovererFor(MediaType.valueOf(response.getContentType()));
		return discoverer.findLinkWithRel(rel, response.getContentAsString());
	}
}
