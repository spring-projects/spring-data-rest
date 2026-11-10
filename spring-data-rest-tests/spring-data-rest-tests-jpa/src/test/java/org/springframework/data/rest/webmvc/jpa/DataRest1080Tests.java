/*
 * Copyright 2017-present the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for DATAREST-1080 (GitHub issue #1445): Spring Data REST must generate correct self-links and
 * expose subclass-specific fields when the repository domain type is an abstract JPA entity with inheritance and the
 * actual instances returned are concrete subclasses.
 *
 * @author Steve Rutherford
 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/1445">GitHub issue #1445</a>
 */
@Transactional
@ContextConfiguration(classes = JpaRepositoryConfig.class)
class DataRest1080Tests extends AbstractWebIntegrationTests {

	@Autowired MealRepository mealRepository;

	private Long savedDinnerId;

	@BeforeEach
	@Override
	public void setUp() {
		super.setUp();

		Dinner dinner = new Dinner();
		dinner.setDinnerCode("HOLIDAY_DINNER");
		dinner = mealRepository.save(dinner);
		savedDinnerId = dinner.getId();
	}

	/**
	 * Verifies that the self-link of a concrete subclass instance returned from a repository typed to an abstract base
	 * class points to the base-class resource path (e.g. {@code /meals/1}), not to a non-existent subclass path (e.g.
	 * {@code /dinners/1}).
	 */
	@Test // DATAREST-1080 / GH-1445
	void selfLinkOfSubclassInstancePointsToBaseClassResourcePath() throws Exception {

		MockHttpServletResponse response = mockMvc
				.perform(get("/meals/{id}", savedDinnerId))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		String selfHref = JsonPath.read(response.getContentAsString(), "$._links.self.href");

		assertThat(selfHref).as("Self-link should point to the /meals path, not a subclass-specific path")
				.contains("/meals/")
				.doesNotContain("/dinners/");
	}

	/**
	 * Verifies that following the self-link of a subclass instance (which must point to the base-class resource path)
	 * returns HTTP 200 OK, i.e. the link is actually resolvable.
	 */
	@Test // DATAREST-1080 / GH-1445
	void selfLinkOfSubclassInstanceIsResolvable() throws Exception {

		MockHttpServletResponse collectionResponse = mockMvc
				.perform(get("/meals"))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		String body = collectionResponse.getContentAsString();

		// The embedded key may be "meals" (correct) or "dinners" (buggy subclass key).
		// We accept either and just verify the self-link is resolvable.
		String embeddedKey = body.contains("\"meals\"") ? "meals" : "dinners";
		String selfHref = JsonPath.read(body, "$._embedded." + embeddedKey + "[0]._links.self.href");

		mockMvc.perform(get(selfHref))
				.andExpect(status().isOk());
	}

	/**
	 * Verifies that the item resource response for a concrete subclass includes the subclass-specific field
	 * ({@code dinnerCode}) in the serialized JSON, not just the fields declared on the abstract base class.
	 */
	@Test // DATAREST-1080 / GH-1445
	void itemResourceIncludesSubclassSpecificFields() throws Exception {

		MockHttpServletResponse response = mockMvc
				.perform(get("/meals/{id}", savedDinnerId))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		String body = response.getContentAsString();

		assertThat(JsonPath.<String> read(body, "$.dinnerCode"))
				.as("Subclass-specific field 'dinnerCode' must be present in the serialized response")
				.isEqualTo("HOLIDAY_DINNER");
	}

	/**
	 * Verifies that the collection resource for the abstract base class repository lists items with self-links that
	 * point to the base-class resource path.
	 */
	@Test // DATAREST-1080 / GH-1445
	void collectionResourceItemsHaveCorrectSelfLinks() throws Exception {

		MockHttpServletResponse response = mockMvc
				.perform(get("/meals"))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		String body = response.getContentAsString();

		// The embedded key may be "meals" (correct) or "dinners" (buggy subclass key).
		// Regardless of the embedded key, the self-link of each item must point to /meals/.
		String embeddedKey = body.contains("\"meals\"") ? "meals" : "dinners";
		String embeddedSelfHref = JsonPath.read(body, "$._embedded." + embeddedKey + "[0]._links.self.href");

		assertThat(embeddedSelfHref)
				.as("Embedded item self-link should point to /meals/, not a subclass path")
				.contains("/meals/")
				.doesNotContain("/dinners/");
	}
}
