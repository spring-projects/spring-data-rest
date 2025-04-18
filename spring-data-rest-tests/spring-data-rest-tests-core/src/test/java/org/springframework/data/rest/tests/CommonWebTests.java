/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.rest.tests;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import net.minidev.json.JSONArray;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * This class contains a common test suite used to verify multiple data stores with the same domain space. When
 * verifying support of a new data store, it's good to start with extending this suite of tests. However, if the data
 * store doesn't map well onto this, then a good alternative would be write a new test suite using
 * {@link AbstractWebIntegrationTests AbstractWebIntegrationTests} as the test harness.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public abstract class CommonWebTests extends AbstractWebIntegrationTests {

	protected abstract Iterable<LinkRelation> expectedRootLinkRels();

	// Root test cases

	@Test
	void exposesRootResource() throws Exception {

		ResultActions actions = mvc.perform(get("/").accept(TestMvcClient.DEFAULT_MEDIA_TYPE)).andExpect(status().isOk());

		for (LinkRelation rel : expectedRootLinkRels()) {
			actions.andExpect(client.hasLinkWithRel(rel));
		}
	}

	@Test // DATAREST-113, DATAREST-638
	void exposesSchemasForResourcesExposed() throws Exception {

		MockHttpServletResponse response = client.request("/");

		for (LinkRelation rel : expectedRootLinkRels()) {

			Link link = client.assertHasLinkWithRel(rel, response);

			// Resource
			client.follow(link).andExpect(status().is2xxSuccessful());

			Link profileLink = client.discoverUnique(link, LinkRelation.of("profile"));

			// Default metadata
			client.follow(profileLink).andExpect(status().is2xxSuccessful());

			// JSON Schema
			client.follow(profileLink, RestMediaTypes.SCHEMA_JSON).andExpect(status().is2xxSuccessful());

			// ALPS
			client.follow(profileLink, MediaTypes.ALPS_JSON).andExpect(status().is2xxSuccessful());
		}
	}

	@Test // DATAREST-203
	void servesHalWhenRequested() throws Exception {

		mvc.perform(get("/")). //
				andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON)). //
				andExpect(jsonPath("$._links", notNullValue()));
	}

	@Test // DATAREST-203
	void servesHalWhenJsonIsRequested() throws Exception {

		mvc.perform(get("/").accept(MediaType.APPLICATION_JSON)). //
				andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)). //
				andExpect(jsonPath("$._links", notNullValue()));
	}

	@Test // DATAREST-203
	void exposesSearchesForRootResources() throws Exception {

		MockHttpServletResponse response = client.request("/");

		for (LinkRelation rel : expectedRootLinkRels()) {

			Link link = client.assertHasLinkWithRel(rel, response);
			String rootResourceRepresentation = client.request(link).getContentAsString();

			client.getDiscoverer(response) //
					.findLinkWithRel("search", rootResourceRepresentation) //
					.ifPresent(it -> {

						try {

							client.follow(it).//
							andExpect(client.hasLinkWithRel(IanaLinkRelations.SELF)).//
							andExpect(jsonPath("$.domainType").doesNotExist());

						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});
		}
	}

	@Test
	void nic() throws Exception {

		Map<LinkRelation, String> payloads = getPayloadToPost();
		assumeFalse(payloads.isEmpty());

		MockHttpServletResponse response = client.request("/");

		for (LinkRelation rel : expectedRootLinkRels()) {

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

	@Test // DATAREST-198
	void accessLinkedResources() throws Exception {

		MockHttpServletResponse rootResource = client.request("/");

		for (Map.Entry<LinkRelation, List<String>> linked : getRootAndLinkedResources().entrySet()) {

			Link resourceLink = client.assertHasLinkWithRel(linked.getKey(), rootResource);
			MockHttpServletResponse resource = client.request(resourceLink);

			for (String linkedRel : linked.getValue()) {

				// Find URIs pointing to linked resources
				String jsonPath = String.format("$._embedded.%s[*]._links.%s.href", linked.getKey(), linkedRel);
				String representation = resource.getContentAsString();
				JSONArray uris = JsonPath.read(representation, jsonPath);

				for (Object href : uris) {

					client.follow(Link.of(href.toString())) //
							.andExpect(status().isOk());
				}
			}
		}
	}

	@Test // DATAREST-230
	void exposesDescriptionAsAlpsDocuments() throws Exception {

		MediaType ALPS_MEDIA_TYPE = MediaType.valueOf("application/alps+json");

		MockHttpServletResponse response = client.request("/");
		Link profileLink = client.assertHasLinkWithRel(LinkRelation.of("profile"), response);

		mvc.perform(//
				get(profileLink.expand().getHref()).//
						accept(ALPS_MEDIA_TYPE))
				.//
				andExpect(status().isOk()).//
				andExpect(content().contentTypeCompatibleWith(ALPS_MEDIA_TYPE));
	}

	@Test // DATAREST-448
	void returnsNotFoundForUriNotBackedByARepository() throws Exception {

		mvc.perform(get("/index.html")).//
				andExpect(status().isNotFound());
	}

	@Test // DATAREST-658
	void collectionResourcesExposeLinksAsHeadersForHeadRequest() throws Exception {

		for (LinkRelation rel : expectedRootLinkRels()) {

			Link link = client.discoverUnique(rel);

			MockHttpServletResponse response = mvc.perform(head(link.expand().getHref()))//
					.andExpect(status().isNoContent())//
					.andReturn().getResponse();

			Links links = Links.parse(response.getHeader("Link"));

			assertThat(links.hasLink(IanaLinkRelations.SELF)).isTrue();
			assertThat(links.hasLink("profile")).isTrue();
		}
	}

	@Test // DATAREST-661
	void patchToNonExistingResourceReturnsNotFound() throws Exception {

		LinkRelation rel = expectedRootLinkRels().iterator().next();
		String uri = client.discoverUnique(rel).expand().getHref().concat("/");
		String id = "4711";
		Integer status = null;

		do {

			// Try to find non existing resource
			uri = uri.concat(id);
			status = mvc.perform(get(URI.create(uri))).andReturn().getResponse().getStatus();

		} while (status != HttpStatus.NOT_FOUND.value());

		// PATCH to non-existing resource
		mvc.perform(patch(URI.create(uri))).andExpect(status().isNotFound());
	}

	@Test // DATAREST-1003
	void rejectsUnsupportedAcceptTypeForResources() throws Exception {

		for (LinkRelation string : expectedRootLinkRels()) {

			Link link = client.discoverUnique(string);

			mvc.perform(get(link.expand().getHref())//
					.accept(MediaType.valueOf("application/schema+json")))//
					.andExpect(status().isNotAcceptable());
		}
	}
}
