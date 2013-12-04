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
package org.springframework.data.rest.webmvc.jpa;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Web integration tests specific to JPA.
 * 
 * @author Oliver Gierke
 */
@Transactional
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class JpaWebTests extends AbstractWebIntegrationTests {

	@Autowired TestDataPopulator loader;
	@Autowired ResourceMappings mappings;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#setUp()
	 */
	@Override
	@Before
	public void setUp() {
		loader.populateRepositories();
		super.setUp();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("people");
	}

	/**
	 * @see DATAREST-99
	 */
	@Test
	public void doesNotExposeCreditCardRepository() throws Exception {

		mvc.perform(get("/")). //
				andExpect(status().isOk()). //
				andExpect(doesNotHaveLinkWithRel(mappings.getMappingFor(CreditCard.class).getRel()));
	}

	@Test
	public void accessPersons() throws Exception {

		MockHttpServletResponse response = request("/people?page=0&size=1");

		Link nextLink = assertHasLinkWithRel(Link.REL_NEXT, response);
		assertDoesNotHaveLinkWithRel(Link.REL_PREVIOUS, response);

		response = request(nextLink);
		assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		nextLink = assertHasLinkWithRel(Link.REL_NEXT, response);

		response = request(nextLink);
		assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		assertDoesNotHaveLinkWithRel(Link.REL_NEXT, response);
	}

	/**
	 * @see DATAREST-169
	 */
	@Test
	public void exposesCreatorOfAnOrder() throws Exception {

		MockHttpServletResponse response = request("/");
		Link ordersLink = assertHasLinkWithRel("orders", response);

		MockHttpServletResponse orders = request(ordersLink);

		Link creatorLink = assertHasContentLinkWithRel("creator", orders);

		assertThat(request(creatorLink), is(notNullValue()));
	}

	/**
	 * @see DATAREST-200
	 */
	@Test
	public void exposesInlinedEntities() throws Exception {

		MockHttpServletResponse response = request("/");
		Link ordersLink = assertHasLinkWithRel("orders", response);

		MockHttpServletResponse orders = request(ordersLink);
		assertHasJsonPathValue("$..lineItems", orders);
	}
}
