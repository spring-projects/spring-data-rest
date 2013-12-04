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
package org.springframework.data.rest.webmvc;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.core.DefaultLinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RepositoryRestMvcConfiguration.class)
public abstract class AbstractWebIntegrationTests {

	@Autowired WebApplicationContext context;

	protected MockMvc mvc;
	LinkDiscoverer links = new DefaultLinkDiscoverer();

	@Before
	public void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	protected MockHttpServletResponse request(String href, MediaType contentType) throws Exception {
		return mvc.perform(get(href).accept(contentType)). //
				andExpect(status().isOk()). //
				andExpect(content().contentType(MediaType.APPLICATION_JSON)). //
				andReturn().getResponse();
	}

	protected MockHttpServletResponse request(Link link) throws Exception {
		return request(link.getHref());
	}

	protected MockHttpServletResponse request(String href) throws Exception {
		return request(href, MediaType.APPLICATION_JSON);
	}

	protected ResultActions follow(Link link) throws Exception {
		return mvc.perform(get(link.getHref()));
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
		String s = mvc.perform(get(root.getHref())).andExpect(status().isOk()).andExpect(hasLinkWithRel(rel)).andReturn()
				.getResponse().getContentAsString();
		return links.findLinksWithRel(rel, s);
	}

	protected Link discoverUnique(Link root, String rel) throws Exception {
		String s = mvc.perform(get(root.getHref())).andExpect(status().isOk()).andExpect(hasLinkWithRel(rel)).andReturn()
				.getResponse().getContentAsString();
		return links.findLinkWithRel(rel, s);
	}

	protected Link assertHasLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Link link = links.findLinkWithRel(rel, content);

		assertThat("Expected to find link with rel " + rel + " but found none in " + content + "!", link,
				is(notNullValue()));

		return link;
	}

	protected Link assertHasContentLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {

		String href = JsonPath
				.read(response.getContentAsString(), String.format("$..links[?(@.rel == '%s')].href[0]", rel)).toString();
		assertThat("Expected to find a link with rel" + rel + " in the content section of the response!", href,
				is(notNullValue()));

		return new Link(href, rel);
	}

	protected void assertDoesNotHaveLinkWithRel(String rel, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Link link = links.findLinkWithRel(rel, content);

		assertThat("Expected not to find link with rel " + rel + " but found " + link + "!", link, is(nullValue()));
	}

	protected void assertHasJsonPathValue(String path, MockHttpServletResponse response) throws Exception {
		assertThat(JsonPath.read(response.getContentAsString(), path), is(notNullValue()));
	}

	protected ResultMatcher hasLinkWithRel(final String rel) {

		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String s = result.getResponse().getContentAsString();
				assertThat("Expected to find link with rel " + rel + " but found none in " + s, links.findLinkWithRel(rel, s),
						notNullValue());
			}
		};
	}

	protected ResultMatcher doesNotHaveLinkWithRel(final String rel) {

		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) throws Exception {
				String s = result.getResponse().getContentAsString();
				assertThat("Expected not to find link with rel " + rel + " but found one in " + s,
						links.findLinkWithRel(rel, s), nullValue());
			}
		};
	}

	@Test
	public void exposesRootResource() throws Exception {

		ResultActions actions = mvc.perform(get("/")).andExpect(status().isOk());

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
			mvc.perform(get(link.getHref() + "/schema").//
					accept(MediaType.parseMediaType("application/schema+json"))).//
					andExpect(status().isOk());
		}
	}

	protected abstract Iterable<String> expectedRootLinkRels();
}
