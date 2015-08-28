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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.core.JsonPathLinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {JpaRepositoryConfig.class, DuplicateLinkListingTests.ClassicConfiguration.class,
		DuplicateLinkListingTests.Config.class })
public class DuplicateLinkListingTests {

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;
	@Autowired PersonRepository personRepository;

	private static MediaType MEDIA_TYPE = MediaType.APPLICATION_JSON;

	protected TestMvcClient testMvcClient;
	protected MockMvc mvc;

	@Configuration
	static class Config {

		@Bean
		public LinkDiscoverer classicLinkDiscover() {
			return new JsonPathLinkDiscoverer("$.links[?(@.rel == '%s')].href", MEDIA_TYPE);
		}
	}

	@Configuration
	static class ClassicConfiguration extends RepositoryRestMvcConfiguration {

		@Override
		protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.setDefaultMediaType(MEDIA_TYPE).useHalAsDefaultJsonMediaType(false);
		}
	}

	@Before
	public void setUp() {

		mvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get("/")).build();
		testMvcClient = new TestMvcClient(mvc, discoverers);

		personRepository.save(new Person("Frodo", "Baggins"));
	}

	@Test
	public void testBasics() throws Exception {

		ResultActions frodoActions = testMvcClient.follow("/people/1");

		frodoActions.andExpect(jsonPath("$.links").value(hasSize(1)));
	}
}