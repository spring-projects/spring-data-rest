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

package org.springframework.data.rest.tests.neo4j;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.util.Arrays;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import net.minidev.json.JSONArray;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.CommonWebTests;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = NeoConfig.class)
public class Neo4jWebTests extends CommonWebTests {

	@Autowired TestDataPopulator populator;
	@Before
	@Override
	public void setUp() {
		this.populator.populate();
		super.setUp();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("customers");
	}

	/**
	 * @see DATAREST-184
	 */
	@Test
	public void deletesCustomer() throws Exception {

		// Lookup customer
		Link customers = client.discoverUnique("customers");
		Link customerLink = assertHasContentLinkWithRel("self", client.request(customers));

		// Delete customer
		mvc.perform(delete(customerLink.getHref()));

		// Assert no customers anymore
		assertDoesNotHaveContentLinkWithRel("self", client.request(customers));
	}

	protected Link assertContentLinkWithRel(String rel, MockHttpServletResponse response, boolean expected) throws Exception {
		String content = response.getContentAsString();

		try {
			String string = String.format("$._embedded.._links.%s.href", new Object[]{rel});
			JSONArray read = JsonPath.read(content, string, new Predicate[0]);
			String o_O = read.get(0).toString();
			Assert.assertThat("Expected to find a link with rel" + rel + " in the content section of the response!", o_O, Matchers.is(expected?Matchers.notNullValue():Matchers.nullValue()));
			return new Link(o_O, rel);
		} catch (InvalidPathException | IndexOutOfBoundsException e) {
			if(expected) {
				Assert.fail("Didn\'t find any content in the given response!");
			}

			return null;
		}
	}
}
