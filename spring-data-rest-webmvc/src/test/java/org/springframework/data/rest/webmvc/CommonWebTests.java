/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.data.rest.webmvc;

import static org.hamcrest.Matchers.*;
import static org.junit.Assume.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;

import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * This class contains a common test suite used to verify multiple data stores with the same domain space. When
 * verifying support of a new data store, it's good to start with extending this suite of tests. However, if the data
 * store doesn't map well onto this, then a good alternative would be write a new test suite using
 * {@link org.springframework.data.rest.webmvc.AbstractWebIntegrationTests AbstractWebIntegrationTests} as the test
 * harness.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public abstract class CommonWebTests extends AbstractWebIntegrationTests {

	protected abstract Iterable<String> expectedRootLinkRels();

	// Root test cases

	@Test
	public void exposesRootResource() throws Exception {

		ResultActions actions = mvc.perform(get("/").accept(TestMvcClient.DEFAULT_MEDIA_TYPE)).andExpect(status().isOk());

		for (String rel : expectedRootLinkRels()) {
			actions.andExpect(client.hasLinkWithRel(rel));
		}
	}

	/**
	 * @see DATAREST-113
	 */
	@Test
	public void exposesSchemasForResourcesExposed() throws Exception {

		MockHttpServletResponse response = client.request("/");

		for (String rel : expectedRootLinkRels()) {

			Link link = client.assertHasLinkWithRel(rel, response);

			// Resource
			client.request(link);

			// Schema - TODO:Improve by using hypermedia
			mvc.perform(get(link.expand().getHref() + "/schema").//
					accept(MediaType.parseMediaType("application/schema+json"))).//
					andExpect(status().isOk());
		}
	}

	/**
	 * @see DATAREST-203
	 */
	@Test
	public void servesHalWhenRequested() throws Exception {

		mvc.perform(get("/")). //
				andExpect(content().contentType(MediaTypes.HAL_JSON)). //
				andExpect(jsonPath("$._links", notNullValue()));
	}

	/**
	 * @see DATAREST-203
	 */
	@Test
	public void servesHalWhenJsonIsRequested() throws Exception {

		mvc.perform(get("/").accept(MediaType.APPLICATION_JSON)). //
				andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)). //
				andExpect(jsonPath("$._links", notNullValue()));
	}

	/**
	 * @see DATAREST-203
	 */
	@Test
	public void exposesSearchesForRootResources() throws Exception {

		MockHttpServletResponse response = client.request("/");

		for (String rel : expectedRootLinkRels()) {

			Link link = client.assertHasLinkWithRel(rel, response);
			String rootResourceRepresentation = client.request(link).getContentAsString();
			Link searchLink = client.getDiscoverer(response).findLinkWithRel("search", rootResourceRepresentation);

			if (searchLink != null) {
				client.follow(searchLink).//
						andExpect(client.hasLinkWithRel("self")).//
						andExpect(jsonPath("$.domainType", is(nullValue()))); // DATAREST-549
			}
		}
	}

	@Test
	public void nic() throws Exception {

		Map<String, String> payloads = getPayloadToPost();
		assumeFalse(payloads.isEmpty());

		MockHttpServletResponse response = client.request("/");

		for (String rel : expectedRootLinkRels()) {

			String payload = payloads.get(rel);

			if (payload != null) {

				Link link = client.assertHasLinkWithRel(rel, response);
				String target = link.expand().getHref();

				MockHttpServletRequestBuilder request = post(target).//
						content(payload).//
						contentType(MediaType.APPLICATION_JSON);

				mvc.perform(request). //
						andExpect(status().isCreated());
			}
		}
	}

	/**
	 * @see DATAREST-198
	 */
	@Test
	public void accessLinkedResources() throws Exception {

		MockHttpServletResponse rootResource = client.request("/");

		for (Map.Entry<String, List<String>> linked : getRootAndLinkedResources().entrySet()) {

			Link resourceLink = client.assertHasLinkWithRel(linked.getKey(), rootResource);
			MockHttpServletResponse resource = client.request(resourceLink);

			for (String linkedRel : linked.getValue()) {

				// Find URIs pointing to linked resources
				String jsonPath = String.format("$..%s._links.%s.href", linked.getKey(), linkedRel);
				String representation = resource.getContentAsString();
				JSONArray uris = JsonPath.read(representation, jsonPath);

				for (Object href : uris) {

					client.follow(href.toString()). //
							andExpect(status().isOk());
				}
			}
		}
	}

	/**
	 * @see DATAREST-230
	 */
	@Test
	public void exposesDescriptionAsAlpsDocuments() throws Exception {

		MediaType ALPS_MEDIA_TYPE = MediaType.valueOf("application/alps+json");

		MockHttpServletResponse response = client.request("/");
		Link profileLink = client.assertHasLinkWithRel("profile", response);

		mvc.perform(//
				get(profileLink.expand().getHref()).//
						accept(ALPS_MEDIA_TYPE)).//
				andExpect(status().isOk()).//
				andExpect(content().contentType(ALPS_MEDIA_TYPE));
	}

	/**
	 * @see DATAREST-448
	 */
	@Test
	public void returnsNotFoundForUriNotBackedByARepository() throws Exception {

		mvc.perform(get("/index.html")).//
				andExpect(status().isNotFound());
	}
}
