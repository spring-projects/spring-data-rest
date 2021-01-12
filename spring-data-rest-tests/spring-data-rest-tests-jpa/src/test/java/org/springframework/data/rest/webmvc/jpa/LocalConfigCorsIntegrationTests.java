/*
 * Copyright 2019-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

/**
 * Web integration tests specific to Cross-origin resource sharing using repository interface CORS configuration.
 *
 * @author Mark Paluch
 * @soundtrack RFLKTD - Liquid Crystals
 */
@ContextConfiguration
public class LocalConfigCorsIntegrationTests extends AbstractWebIntegrationTests {

	static class CorsConfig extends JpaRepositoryConfig {

		@Bean
		RepositoryRestConfigurer repositoryRestConfigurer() {
			return RepositoryRestConfigurer.withConfig(c -> {});
		}
	}

	/**
	 * @see ItemRepository
	 */
	@Test // DATAREST-1397
	public void appliesRepositoryCorsConfiguration() throws Exception {

		Link findItems = client.discoverUnique(LinkRelation.of("items"));

		// Preflight request
		String header = mvc
				.perform(options(findItems.expand().getHref()).header(HttpHeaders.ORIGIN, "http://far.far.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")) //
				.andExpect(status().isOk()) //
				.andReturn().getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);

		assertThat(header.split(","))
				.containsExactlyInAnyOrderElementsOf(
						RepositoryRestHandlerMapping.DEFAULT_ALLOWED_METHODS.map(HttpMethod::name));
	}
}
