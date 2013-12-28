/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests to check the legacy representation is rendered if HAL is not the default media type.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration
public class LegacyRepresentationConfigIntegrationTests extends AbstractRepositoryRestMvcConfigurationIntegrationTests {

	@Configuration
	static class Config extends RepositoryRestMvcConfiguration {

		@Override
		protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.setDefaultMediaType(MediaType.APPLICATION_JSON);
		}
	}

	/**
	 * @see DATAREST-213
	 */
	@Test
	public void returnsJsonIfConfiguredAndRequested() throws Exception {

		mvc.perform(get("/").accept(MediaType.APPLICATION_JSON)). //
				andExpect(jsonPath("content", is(notNullValue())));
	}

	/**
	 * @see DATAREST-213
	 */
	@Test
	public void returnsJsonIfConfigured() throws Exception {

		mvc.perform(get("/").accept(MediaType.ALL)). //
				andExpect(jsonPath("content", is(notNullValue())));
	}
}
