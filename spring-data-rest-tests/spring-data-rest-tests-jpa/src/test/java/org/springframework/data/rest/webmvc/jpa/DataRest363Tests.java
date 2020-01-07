/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.tests.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
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
 * Integration tests for DATAREST-363.
 *
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(
		classes = { JpaRepositoryConfig.class, RepositoryRestMvcConfiguration.class, DataRest363Tests.Config.class })
public class DataRest363Tests {

	private static MediaType MEDIA_TYPE = MediaType.APPLICATION_JSON;

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;
	@Autowired PersonRepository personRepository;

	TestMvcClient testMvcClient;

	Person frodo;

	@Configuration
	static class Config {

		@Bean
		LinkDiscoverer classicLinkDiscover() {
			return new JsonPathLinkDiscoverer("$.links[?(@.rel == '%s')].href", MEDIA_TYPE);
		}

		@Bean
		RepositoryRestConfigurer configurer() {
			return RepositoryRestConfigurer.withConfig( //
					it -> it.setDefaultMediaType(MEDIA_TYPE).useHalAsDefaultJsonMediaType(false));
		}
	}

	@Before
	public void setUp() {

		MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get("/")).build();

		this.testMvcClient = new TestMvcClient(mvc, discoverers);
		this.frodo = personRepository.save(new Person("Frodo", "Baggins"));
	}

	@Test // DATAREST-363
	public void testBasics() throws Exception {

		ResultActions frodoActions = testMvcClient.follow("/people/".concat(frodo.getId().toString()));

		frodoActions.andExpect(jsonPath("$.links").value(hasSize(4)));
	}
}
