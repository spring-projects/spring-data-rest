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
package org.springframework.data.rest.webmvc.solr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christoph Strobl
 */
@ContextConfiguration(classes = { SolrWebTests.MyConf.class })
public class SolrWebTests extends AbstractWebIntegrationTests {

	public static @ClassRule TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

	private static final Product PLAYSTATION = new Product("1", "playstation", "electronic", "game", "media");
	private static final Product GAMEBOY = new Product("2", "gameboy", "electronic");
	private static final Product AMIGA500 = new Product("3", "amiga500", "ancient");

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Configuration
	@EnableSolrRepositories
	@Import(value = { SolrInfrastructureConfig.class })
	static class MyConf {

		@Bean
		String solrHomeDir() {
			return TEMP_FOLDER.getRoot().getAbsolutePath();
		}
	}

	@Autowired ProductRepository repo;

	@Before
	public void setUp() {

		super.setUp();
		repo.save(Arrays.asList(PLAYSTATION, GAMEBOY, AMIGA500));
	}

	@After
	public void tearDown() {
		repo.deleteAll();
	}

	/**
	 * @see DATAREST-387
	 */
	@Test
	public void allowsPaginationThroughData() throws Exception {

		MockHttpServletResponse response = client.request("/products?page=0&size=1");

		Link nextLink = client.assertHasLinkWithRel(Link.REL_NEXT, response);
		assertDoesNotHaveLinkWithRel(Link.REL_PREVIOUS, response);

		response = client.request(nextLink);
		client.assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		nextLink = client.assertHasLinkWithRel(Link.REL_NEXT, response);

		response = client.request(nextLink);
		client.assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		assertDoesNotHaveLinkWithRel(Link.REL_NEXT, response);
	}

	/**
	 * @see DATAREST-387
	 */
	@Test
	public void allowsRetrievingDataById() throws Exception {
		requestAndCompare(PLAYSTATION);
	}

	/**
	 * @see DATAREST-387
	 */
	@Test
	public void createsEntitesCorrectly() throws Exception {

		Product product = new Product("4", "iWatch", "trends", "scary");

		mvc.perform(
				put("/products/{id}", 4).content(MAPPER.writeValueAsString(product)).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated()).andReturn().getResponse();

		assertJsonDocumentMatches(product);
	}

	/**
	 * @see DATAREST-387
	 */
	@Test
	public void deletesEntitiesCorrectly() throws Exception {
		deleteAndVerify(new Link("/products/1"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("products");
	}

	private void assertJsonDocumentMatches(Product reference) throws Exception {
		requestAndCompare(reference);
	}

	private MockHttpServletResponse requestAndCompare(Product reference) throws Exception {

		MockHttpServletResponse response = client.request("/products/" + reference.getId());

		assertJsonPathEquals("name", reference.getName(), response);
		assertJsonPathEquals("categories", MAPPER.writeValueAsString(reference.getCategories()), response);

		return response;
	}
}
