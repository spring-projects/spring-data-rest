/*
 * Copyright 2016-2018 original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Web integration tests specific to default {@link Pageable} handling.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = JpaDefaultPageableWebTests.Config.class)
public class JpaDefaultPageableWebTests extends AbstractWebIntegrationTests {

	@Configuration
	@Import({ RepositoryRestMvcConfiguration.class, JpaRepositoryConfig.class })
	@EnableJpaRepositories(considerNestedRepositories = true)
	static class Config implements RepositoryRestConfigurer {

		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.setDefaultPageSize(1);
		}
	}

	@RepositoryRestController
	public static class MyRestController {

		@RequestMapping(method = RequestMethod.GET, path = "books/default-pageable")
		@ResponseBody
		Page<Book> getDefaultPageable(Pageable pageable) {

			if (pageable != null) {
				return new PageImpl<Book>(Collections.singletonList(new Book()), pageable, 1);
			}

			return new PageImpl<Book>(Collections.emptyList(), pageable, 0);
		}
	}

	@Autowired TestDataPopulator loader;
	@Autowired ApplicationContext context;

	@Override
	@Before
	public void setUp() {
		loader.populateRepositories();
		super.setUp();
	}

	@Test // DATAREST-906
	public void executesSearchThatTakesAMappedSortProperty() throws Exception {

		Link findBySortedLink = client.discoverUnique(LinkRelation.of("books"), IanaLinkRelations.SEARCH,
				LinkRelation.of("find-spring-books-sorted"));

		// Assert sort options advertised
		assertThat(findBySortedLink.isTemplated()).isTrue();
		assertThat(findBySortedLink.getVariableNames()).contains("sort", "projection");

		// Assert results returned as specified
		client.follow(findBySortedLink.expand()).//
				andExpect(jsonPath("$._embedded.books[0].title").exists()).//
				andExpect(jsonPath("$._embedded.books[1].title").doesNotExist());

		client.follow(findBySortedLink.expand("sales,desc")).//
				andExpect(jsonPath("$._embedded.books[0].title").exists()).//
				andExpect(jsonPath("$._embedded.books[1].title").doesNotExist());
	}

	@Test // DATAREST-906
	public void shouldApplyDefaultPageable() throws Exception {

		mvc.perform(get("/books/default-pageable"))//
				.andExpect(jsonPath("$.content[0].sales").value(0)) //
				.andExpect(jsonPath("$.size").value(1));
	}

	@Test // DATAREST-906
	public void shouldOverrideDefaultPageable() throws Exception {

		mvc.perform(get("/books/default-pageable?size=10"))//
				.andExpect(jsonPath("$.content[0].sales").value(0)) //
				.andExpect(jsonPath("$.size").value(10));
	}
}
