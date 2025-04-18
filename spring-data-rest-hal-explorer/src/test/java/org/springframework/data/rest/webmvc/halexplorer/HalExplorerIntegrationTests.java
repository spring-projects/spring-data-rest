/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.halexplorer;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Integration tests for {@link HalExplorer}.
 *
 * @author Oliver Gierke
 * @soundtrack Miles Davis - Blue in green (Kind of blue)
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
class HalExplorerIntegrationTests {

	static final String BASE_PATH = "/api";
	static final String EXPLORER_INDEX = "/explorer/index.html";
	static final String TARGET = BASE_PATH.concat(EXPLORER_INDEX).concat("#uri=").concat(BASE_PATH);

	@Configuration
	@EnableWebMvc
	@Import(RepositoryRestMvcConfiguration.class)
	static class TestConfiguration {

		@Bean
		RepositoryRestConfigurer configExtension() {
			return RepositoryRestConfigurer.withConfig(config -> config.setBasePath(BASE_PATH));
		}
	}

	@Autowired WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get(BASE_PATH).accept(MediaType.TEXT_HTML)).build();
	}

	@Test // DATAREST-293
	void exposesJsonUnderApiRootByDefault() throws Exception {

		mvc.perform(get(BASE_PATH).accept(MediaType.ALL)).//
				andExpect(status().isOk()).//
				andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaTypes.HAL_JSON.toString())));
	}

	@Test // DATAREST-293
	void redirectsToBrowserForApiRootAndHtml() throws Exception {

		mvc.perform(get(BASE_PATH).accept(MediaType.TEXT_HTML)).//
				andExpect(status().isFound()).//
				andExpect(header().string(HttpHeaders.LOCATION, endsWith(TARGET)));
	}

	@Test // DATAREST-293
	void forwardsBrowserToIndexHtml() throws Exception {

		mvc.perform(get(BASE_PATH.concat("/explorer"))).//
				andExpect(status().isFound()).//
				andExpect(header().string(HttpHeaders.LOCATION, endsWith(TARGET)));
	}

	@Test // DATAREST-293
	void exposesHalBrowser() throws Exception {

		mvc.perform(get(BASE_PATH.concat("/explorer/index.html"))).//
				andExpect(status().isOk()).//
				andExpect(content().string(containsString("HAL Explorer")));
	}

	@Test // DATAREST-293
	void retrunsApiIfHtmlIsNotExplicitlyListed() throws Exception {

		mvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON, MediaType.ALL)).//
				andExpect(status().isOk()).//
				andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.APPLICATION_JSON_VALUE)));
	}
}
