/*
 * Copyright 2013-2014 the original author or authors.
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
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minidev.json.JSONArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RepositoryRestMvcConfiguration.class)
public abstract class AbstractWebIntegrationTests {

	private static final String CONTENT_LINK_JSONPATH = "$._embedded.._links.%s.href[0]";

	protected static MediaType DEFAULT_MEDIA_TYPE = org.springframework.hateoas.MediaTypes.HAL_JSON;

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	protected MockMvc mvc;

	@Before
	public void setUp() {

		mvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get("/").accept(DEFAULT_MEDIA_TYPE)).build();
	}

	protected MockHttpServletResponse request(String href, MediaType contentType) throws Exception {
		return mvc.perform(get(href).accept(contentType)). //
				andExpect(status().isOk()). //
				andExpect(content().contentType(contentType)). //
				andReturn().getResponse();
	}

	protected MockHttpServletResponse request(Link link) throws Exception {
		return request(link.expand().getHref());
	}

	protected MockHttpServletResponse request(String href) throws Exception {
		return request(href, DEFAULT_MEDIA_TYPE);
	}

	protected ResultActions follow(Link link) throws Exception {
		return follow(link.expand().getHref());
	}

	protected ResultActions follow(String href) throws Exception {
		return mvc.perform(get(href));
	}

	protected List<Link> discover(String rel) throws Exception {
		return discover(new Link("/"), rel);
	}

	protected Link discoverUnique(String rel) throws Exception {

		List<Link> discover = discover(rel);
		assertThat(discover, hasSize(1));
		return discover.get(0);
	}

	protected List<Link> discover(Link root, String rel) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(root.getHref()).accept(DEFAULT_MEDIA_TYPE)).//
				andExpect(status().isOk()).//
				andExpect(hasLinkWithRel(rel)).//
				andReturn().getResponse();

		String s = response.getContentAsString();
		return getDiscoverer(response).findLinksWithRel(rel, s);
	}

	protected Link discoverUnique(Link root, String rel) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(root.getHref()).accept(DEFAULT_MEDIA_TYPE)).//
				andExpect(status().isOk()).//
				andExpect(hasLinkWithRel(rel)).//
				andReturn().getResponse();

		return assertHasLinkWithRel(rel, response);
	}

	protected MockHttpServletResponse postAndGet(Link link, Object payload, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(post(href).content(payload.toString()).contentType(mediaType)).//
				andExpect(status().isCreated()).//
				andExpect(header().string("Location", is(notNullValue()))).//
				andReturn().getResponse();

		String content = response.getContentAsString();

		if (StringUtils.hasText(content)) {
			return response;
		}

		return request(response.getHeader("Location"));
	}

	protected MockHttpServletResponse putAndGet(Link link, Object payload, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(put(href).content(payload.toString()).contentType(mediaType)).//
				andExpect(status().is(both(greaterThanOrEqualTo(200)).and(lessThan(300)))).//
				andExpect(header().string("Location", is(notNullValue()))).//
				andReturn().getResponse();

		String content = response.getContentAsString();

		if (StringUtils.hasText(content)) {
			return response;
		}

		return request(response.getHeader("Location"));
	}

	protected MockHttpServletResponse deleteAndGet(Link link, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(delete(href).contentType(mediaType)).//
				andExpect(status().isNoContent()).//
				andReturn().getResponse();

		String content = response.getContentAsString();

		if (StringUtils.hasText(content)) {
			return response;
		}

		return request(response.getHeader("Location"));
	}

	protected Link assertHasLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Link link = getDiscoverer(response).findLinkWithRel(rel, content);

		assertThat("Expected to find link with rel " + rel + " but found none in " + content + "!", link,
				is(notNullValue()));

		return link;
	}

	protected Link assertHasContentLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {
		return assertContentLinkWithRel(rel, response, true);
	}

	protected void assertDoesNotHaveContentLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {
		assertContentLinkWithRel(rel, response, false);
	}

	protected Link assertContentLinkWithRel(String rel, MockHttpServletResponse response, boolean expected)
			throws Exception {

		String content = response.getContentAsString();

		try {

			String href = JsonPath.read(content, String.format(CONTENT_LINK_JSONPATH, rel)).toString();
			assertThat("Expected to find a link with rel" + rel + " in the content section of the response!", href,
					is(expected ? notNullValue() : nullValue()));

			return new Link(href, rel);

		} catch (InvalidPathException o_O) {

			if (expected) {
				fail("Didn't find any content in the given response!");
			}

			return null;
		}
	}

	protected void assertDoesNotHaveLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Link link = getDiscoverer(response).findLinkWithRel(rel, content);

		assertThat("Expected not to find link with rel " + rel + " but found " + link + "!", link, is(nullValue()));
	}

	private LinkDiscoverer getDiscoverer(MockHttpServletResponse response) {

		String contentType = response.getContentType();
		LinkDiscoverer linkDiscovererFor = discoverers.getLinkDiscovererFor(contentType);

		assertThat("Did not find a LinkDiscoverer for returned media type " + contentType + "!", linkDiscovererFor,
				is(notNullValue()));

		return linkDiscovererFor;
	}

	@SuppressWarnings("unchecked")
	protected <T> T assertHasJsonPathValue(String path, MockHttpServletResponse response) throws Exception {

		Object jsonPathResult = JsonPath.read(response.getContentAsString(), path);
		assertThat(jsonPathResult, is(notNullValue()));

		return (T) jsonPathResult;
	}

	protected String assertJsonPathEquals(String path, MockHttpServletResponse response, String expected)
			throws Exception {

		String jsonQueryResults = assertHasJsonPathValue(path, response);
		assertThat(jsonQueryResults, is(expected));

		return jsonQueryResults;
	}

	protected ResultMatcher hasLinkWithRel(final String rel) {

		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) throws Exception {

				MockHttpServletResponse response = result.getResponse();
				String s = response.getContentAsString();

				assertThat("Expected to find link with rel " + rel + " but found none in " + s, //
						getDiscoverer(response).findLinkWithRel(rel, s), notNullValue());
			}
		};
	}

	protected ResultMatcher doesNotHaveLinkWithRel(final String rel) {

		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) throws Exception {

				MockHttpServletResponse response = result.getResponse();
				String s = response.getContentAsString();

				assertThat("Expected not to find link with rel " + rel + " but found one in " + s, //
						getDiscoverer(response).findLinkWithRel(rel, s), nullValue());
			}
		};
	}

	// Root test cases

	@Test
	public void exposesRootResource() throws Exception {

		ResultActions actions = mvc.perform(get("/").accept(DEFAULT_MEDIA_TYPE)).andExpect(status().isOk());

		for (String rel : expectedRootLinkRels()) {
			actions.andExpect(hasLinkWithRel(rel));
		}
	}

	/**
	 * @see DATAREST-113
	 */
	@Test
	public void exposesSchemasForResourcesExposed() throws Exception {

		MockHttpServletResponse response = request("/");

		for (String rel : expectedRootLinkRels()) {

			Link link = assertHasLinkWithRel(rel, response);

			// Resource
			request(link);

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
				andExpect(content().contentType(MediaType.APPLICATION_JSON)). //
				andExpect(jsonPath("$._links", notNullValue()));
	}

	/**
	 * @see DATAREST-203
	 */
	@Test
	public void exposesSearchesForRootResources() throws Exception {

		MockHttpServletResponse response = request("/");

		for (String rel : expectedRootLinkRels()) {

			Link link = assertHasLinkWithRel(rel, response);
			String rootResourceRepresentation = request(link).getContentAsString();
			Link searchLink = getDiscoverer(response).findLinkWithRel("search", rootResourceRepresentation);

			if (searchLink != null) {
				request(searchLink);
			}
		}
	}

	@Test
	public void nic() throws Exception {

		Map<String, String> payloads = getPayloadToPost();
		assumeFalse(payloads.isEmpty());

		MockHttpServletResponse response = request("/");

		for (String rel : expectedRootLinkRels()) {

			String payload = payloads.get(rel);

			if (payload != null) {

				Link link = assertHasLinkWithRel(rel, response);
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

		MockHttpServletResponse rootResource = request("/");

		for (Entry<String, List<String>> linked : getRootAndLinkedResources().entrySet()) {

			Link resourceLink = assertHasLinkWithRel(linked.getKey(), rootResource);
			MockHttpServletResponse resource = request(resourceLink);

			for (String linkedRel : linked.getValue()) {

				// Find URIs pointing to linked resources
				String jsonPath = String.format("$..%s._links.%s.href", linked.getKey(), linkedRel);
				String representation = resource.getContentAsString();
				JSONArray uris = JsonPath.read(representation, jsonPath);

				for (Object href : uris) {

					follow(href.toString()). //
							andExpect(status().isOk());
				}
			}
		}
	}

	protected abstract Iterable<String> expectedRootLinkRels();

	protected Map<String, String> getPayloadToPost() throws Exception {
		return Collections.emptyMap();
	}

	protected MultiValueMap<String, String> getRootAndLinkedResources() {
		return new LinkedMultiValueMap<String, String>(0);
	}
}
