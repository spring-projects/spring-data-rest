/*
 * Copyright 2016-2018 original author or authors.
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

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Web integration tests specific to Cross-origin resource sharing.
 *
 * @author Mark Paluch
 * @soundtrack 2 Unlimited - No Limit
 */
@ContextConfiguration
public class CorsIntegrationTests extends AbstractWebIntegrationTests {

	static class CorsConfig extends JpaRepositoryConfig {

		@Bean
		RepositoryRestConfigurer repositoryRestConfigurer() {

			return RepositoryRestConfigurer.withConfig(config -> {

				config.getCorsRegistry().addMapping("/books/**") //
						.allowedMethods("GET", "PUT", "POST") //
						.allowedOrigins("http://far.far.away");
			});
		}
	}

	@Test // DATAREST-573
	public void appliesSelectiveDefaultCorsConfiguration() throws Exception {

		Link findItems = client.discoverUnique("items");

		// Preflight request
		mvc.perform(options(findItems.expand().getHref()).header(HttpHeaders.ORIGIN, "http://far.far.away")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")) //
				.andExpect(status().isOk()) //
				.andExpect(
						header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS,TRACE"));
	}

	@Test // DATAREST-573
	public void appliesGlobalCorsConfiguration() throws Exception {

		Link findBooks = client.discoverUnique("books");

		// Preflight request
		mvc.perform(options(findBooks.expand().getHref()).header(HttpHeaders.ORIGIN, "http://far.far.away")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")) //
				.andExpect(status().isOk()) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://far.far.away")) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,PUT,POST"));

		// CORS request
		mvc.perform(get(findBooks.expand().getHref()).header(HttpHeaders.ORIGIN, "http://far.far.away")) //
				.andExpect(status().isOk()) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://far.far.away"));
	}

	/**
	 * @see BooksXmlController
	 */
	@Test // DATAREST-573
	public void appliesCorsConfigurationOnCustomControllers() throws Exception {

		// Preflight request
		mvc.perform(options("/books/xml/1234") //
				.header(HttpHeaders.ORIGIN, "http://far.far.away") //
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")) //
				.andExpect(status().isOk()) //
				.andExpect(header().longValue(HttpHeaders.ACCESS_CONTROL_MAX_AGE, 77123)) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://far.far.away")) //
				// See https://jira.spring.io/browse/SPR-14792
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET,PUT,POST")));

		// CORS request
		mvc.perform(get("/books/xml/1234") //
				.header(HttpHeaders.ORIGIN, "http://far.far.away") //
				.accept(MediaType.APPLICATION_XML)) //
				.andExpect(status().isOk()) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://far.far.away"));
	}

	/**
	 * @see BooksPdfController
	 */
	@Test // DATAREST-573
	public void appliesCorsConfigurationOnCustomControllerMethod() throws Exception {

		// Preflight request
		mvc.perform(options("/books/pdf/1234").header(HttpHeaders.ORIGIN, "http://far.far.away")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")) //
				.andExpect(status().isOk()) //
				.andExpect(header().longValue(HttpHeaders.ACCESS_CONTROL_MAX_AGE, 4711)) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://far.far.away")) //
				// See https://jira.spring.io/browse/SPR-14792
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET,PUT,POST")));
	}

	@Test // DATAREST-573
	public void appliesCorsConfigurationOnRepository() throws Exception {

		Link authorsLink = client.discoverUnique("authors");

		// Preflight request
		mvc.perform(options(authorsLink.expand().getHref()).header(HttpHeaders.ORIGIN, "http://not.so.far.away")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")) //
				.andExpect(status().isOk()) //
				.andExpect(header().longValue(HttpHeaders.ACCESS_CONTROL_MAX_AGE, 1234)) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://not.so.far.away")) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,PATCH,POST"));
	}

	@Test // DATAREST-573
	public void appliesCorsConfigurationOnRepositoryToCustomControllers() throws Exception {

		// Preflight request
		mvc.perform(options("/authors/pdf/1234").header(HttpHeaders.ORIGIN, "http://not.so.far.away")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")) //
				.andExpect(status().isOk()) //
				.andExpect(header().longValue(HttpHeaders.ACCESS_CONTROL_MAX_AGE, 1234)) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://not.so.far.away")) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")) //
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,PATCH,POST"));
	}

	@RepositoryRestController
	static class AuthorsPdfController {

		@RequestMapping(method = RequestMethod.GET, path = "/authors/pdf/1234", produces = MediaType.APPLICATION_PDF_VALUE)
		void authorToPdf() {}
	}

	@RepositoryRestController
	static class BooksPdfController {

		@RequestMapping(method = RequestMethod.GET, path = "/books/pdf/1234", produces = MediaType.APPLICATION_PDF_VALUE)
		@CrossOrigin(maxAge = 4711)
		void bookToPdf() {}
	}

	@BasePathAwareController
	static class BooksXmlController {

		@GetMapping(value = "/books/xml/{id}", produces = MediaType.APPLICATION_XML_VALUE)
		@CrossOrigin(maxAge = 77123)
		void bookToXml(@PathVariable String id) {}
	}
}
