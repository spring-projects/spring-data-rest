/*
 * Copyright 2013-2019 the original author or authors.
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
import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import net.minidev.json.JSONArray;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

/**
 * A test harness for hypermedia unit/integration testing. Provides chained operations (like postAndGet) to create a new
 * entity and then retrieve it with a single method call. It also provides often-used assertions (like
 * assertJsonPathEquals).
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Ľubomír Varga
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RepositoryRestMvcConfiguration.class, DelegatingWebMvcConfiguration.class })
public abstract class AbstractWebIntegrationTests {

	private static final String CONTENT_LINK_JSONPATH = "$._embedded.._links.%s.href";

	@Autowired WebApplicationContext context;
	@Autowired LinkDiscoverers discoverers;

	protected TestMvcClient client;
	protected MockMvc mvc;

	@Before
	public void setUp() {
		setupMockMvc();
		this.client = new TestMvcClient(mvc, discoverers);
	}

	protected void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(context)//
				.defaultRequest(get("/").accept(TestMvcClient.DEFAULT_MEDIA_TYPE)).build();
	}

	protected MockHttpServletResponse postAndGet(Link link, Object payload, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(post(href).content(payload.toString()).contentType(mediaType))//
				.andExpect(status().isCreated())//
				.andExpect(header().string("Location", is(notNullValue())))//
				.andReturn().getResponse();

		String content = response.getContentAsString();

		if (StringUtils.hasText(content)) {
			return response;
		}

		return client.request(response.getHeader("Location"));
	}

	protected MockHttpServletResponse putAndGet(Link link, Object payload, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(put(href).content(payload.toString()).contentType(mediaType))//
				.andExpect(status().is2xxSuccessful())//
				.andReturn().getResponse();

		return StringUtils.hasText(response.getContentAsString()) ? response : client.request(link);
	}

	protected MockHttpServletResponse putOnlyExpect5XXStatus(Link link, Object payload, MediaType mediaType)
			throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(put(href).content(payload.toString()).contentType(mediaType))//
				.andExpect(status().is5xxServerError()).andReturn().getResponse();

		return StringUtils.hasText(response.getContentAsString()) ? response : client.request(link);
	}

	protected MockHttpServletResponse patchAndGet(Link link, Object payload, MediaType mediaType) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		MockHttpServletResponse response = mvc.perform(MockMvcRequestBuilders.request(HttpMethod.PATCH, href).//
				content(payload.toString()).contentType(mediaType)).andExpect(status().is2xxSuccessful())//
				.andReturn().getResponse();

		return StringUtils.hasText(response.getContentAsString()) ? response : client.request(href);
	}

	protected void deleteAndVerify(Link link) throws Exception {

		String href = link.isTemplated() ? link.expand().getHref() : link.getHref();

		mvc.perform(delete(href))//
				.andExpect(status().isNoContent())//
				.andReturn().getResponse();

		// Check that the resource is unavailable after a DELETE
		mvc.perform(get(href))//
				.andExpect(status().isNotFound());
	}

	protected Link assertHasContentLinkWithRel(LinkRelation relation, MockHttpServletResponse response) throws Exception {
		return assertContentLinkWithRel(relation, response, true);
	}

	protected void assertDoesNotHaveContentLinkWithRel(LinkRelation rel, MockHttpServletResponse response)
			throws Exception {
		assertContentLinkWithRel(rel, response, false);
	}

	protected Link assertContentLinkWithRel(LinkRelation rel, MockHttpServletResponse response, boolean expected)
			throws Exception {

		String content = response.getContentAsString();

		try {

			String href = JsonPath.<JSONArray> read(content, String.format(CONTENT_LINK_JSONPATH, rel)).get(0).toString();

			String message = "Expected to%s find a link with rel %s in the content section of the response!";

			if (expected) {
				assertThat(href).as(message, "", rel).isNotNull();
			} else {
				assertThat(href).as(message, " not", rel).isNull();
			}

			return new Link(href, rel);

		} catch (InvalidPathException o_O) {

			if (expected) {
				fail("Didn't find any content in the given response!", o_O);
			}

			return null;
		}
	}

	protected void assertDoesNotHaveLinkWithRel(LinkRelation rel, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Optional<Link> link = client.getDiscoverer(response).findLinkWithRel(rel, content);

		assertThat(link).as("Expected not to find link with rel %s but found %s!", rel, link).isEmpty();
	}

	@SuppressWarnings("unchecked")
	protected <T> T assertHasJsonPathValue(String path, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Object jsonPathResult = JsonPath.read(content, path);

		assertThat(jsonPathResult).as("JSONPath lookup for %s did return null in %s.", path, content).isNotNull();

		if (jsonPathResult instanceof JSONArray) {
			JSONArray array = (JSONArray) jsonPathResult;
			assertThat(array.size()).isGreaterThan(0);
		}

		return (T) jsonPathResult;
	}

	protected void assertJsonPathDoesntExist(String path, MockHttpServletResponse response) throws Exception {

		try {

			Object result = JsonPath.read(response.getContentAsString(), path);

			if (result != null) {
				fail("Was expecting to find no value for path " + path + " but got " + result.toString());
			}

		} catch (InvalidPathException e) {}
	}

	protected String assertJsonPathEquals(String path, String expected, MockHttpServletResponse response)
			throws Exception {

		Object jsonQueryResults = assertHasJsonPathValue(path, response);

		String jsonString = "";

		if (jsonQueryResults instanceof JSONArray) {
			jsonString = ((JSONArray) jsonQueryResults).toJSONString();
		} else {
			jsonString = jsonQueryResults != null ? jsonQueryResults.toString() : null;
		}

		assertThat(jsonString).isEqualTo(expected);
		return jsonString;
	}

	protected ResultMatcher doesNotHaveLinkWithRel(final LinkRelation relation) {

		return result -> {

			MockHttpServletResponse response = result.getResponse();
			String s = response.getContentAsString();

			assertThat(client.getDiscoverer(response).findLinkWithRel(relation, s))//
					.as("Expected not to find link with rel %s but found one in %s!", relation, s)//
					.isEmpty();
		};
	}

	protected Map<LinkRelation, String> getPayloadToPost() throws Exception {
		return Collections.emptyMap();
	}

	protected MultiValueMap<LinkRelation, String> getRootAndLinkedResources() {
		return new LinkedMultiValueMap<LinkRelation, String>(0);
	}
}
