/*
 * Copyright 2013-2022 the original author or authors.
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper methods for web integration testing.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class TestMvcClient {

	public static MediaType DEFAULT_MEDIA_TYPE = org.springframework.hateoas.MediaTypes.HAL_JSON;

	private final MockMvc mvc;
	private final LinkDiscoverers discoverers;

	/**
	 * Creates a new {@link TestMvcClient} for the given {@link MockMvc} and {@link LinkDiscoverers}.
	 *
	 * @param mvc must not be {@literal null}.
	 * @param discoverers must not be {@literal null}.
	 */
	public TestMvcClient(MockMvc mvc, LinkDiscoverers discoverers) {

		Assert.notNull(mvc, "MockMvc must not be null");
		Assert.notNull(discoverers, "LinkDiscoverers must not be null");

		this.mvc = mvc;
		this.discoverers = discoverers;
	}

	/**
	 * Initializes web tests. Will register a {@link MockHttpServletRequest} for the current thread.
	 */
	public static void initWebTest() {

		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	public static void assertAllowHeaders(HttpEntity<?> response, HttpMethod... methods) {

		HttpHeaders headers = response.getHeaders();

		assertThat(headers.getAllow()).hasSize(methods.length);
		assertThat(headers.getAllow()).contains(methods);
	}

	/**
	 * Perform GET [href] with an explicit Accept media type using MockMvc. Verify the requests succeeded and also came
	 * back as the Accept type.
	 *
	 * @param href
	 * @param contentType
	 * @return a mocked servlet response with results from GET [href]
	 * @throws Exception
	 */
	public MockHttpServletResponse request(String href, MediaType contentType) throws Exception {
		return mvc.perform(get(href).accept(contentType)). //
				andExpect(status().isOk()). //
				andExpect(content().contentTypeCompatibleWith(contentType)). //
				andReturn().getResponse();
	}

	/**
	 * Perform GET [href] with an explicit Accept media type using MockMvc. Verify the requests succeeded and also came
	 * back as the Accept type.
	 *
	 * @param href
	 * @param contentType
	 * @return a mocked servlet response with results from GET [href]
	 * @throws Exception
	 */
	public MockHttpServletResponse request(String href, MediaType contentType, HttpHeaders httpHeaders) throws Exception {
		return mvc.perform(get(href).accept(contentType).headers(httpHeaders)). //
				andExpect(status().isOk()). //
				andExpect(content().contentType(contentType)). //
				andReturn().getResponse();
	}

	/**
	 * Convenience wrapper that first expands the link using URI substitution before requesting with the default media
	 * type.
	 *
	 * @param link
	 * @return
	 * @throws Exception
	 */
	public MockHttpServletResponse request(Link link) throws Exception {
		return request(link.expand().getHref());
	}

	/**
	 * Convenience wrapper that first expands the link using URI substitution and then GET [href] using an explicit media
	 * type
	 *
	 * @param link
	 * @param mediaType
	 * @return
	 * @throws Exception
	 */
	public MockHttpServletResponse request(Link link, MediaType mediaType) throws Exception {
		return request(link.expand().getHref(), mediaType);
	}

	/**
	 * Convenience wrapper to GET [href] using the default media type.
	 *
	 * @param href
	 * @return
	 * @throws Exception
	 */
	public MockHttpServletResponse request(String href) throws Exception {
		return request(href, DEFAULT_MEDIA_TYPE);
	}

	/**
	 * For a given link, expand the href using URI substitution and then do a simple GET.
	 *
	 * @param link
	 * @return
	 * @throws Exception
	 */
	public ResultActions follow(Link link) throws Exception {
		return follow(link.expand().getHref());
	}

	/**
	 * Follow URL supplied as a string. NOTE: Assumes no URI templates.
	 *
	 * @param href
	 * @return
	 * @throws Exception
	 */
	public ResultActions follow(String href) throws Exception {
		return follow(href, MediaType.ALL);
	}

	/**
	 * Folow Link with a specific Accept header (media type).
	 *
	 * @param link
	 * @param accept
	 * @return
	 * @throws Exception
	 */
	public ResultActions follow(Link link, MediaType accept) throws Exception {
		return follow(link.expand().getHref(), accept);
	}

	/**
	 * Follow URL supplied as a string with a specific Accept header.
	 *
	 * @param href
	 * @param accept
	 * @return
	 * @throws Exception
	 */
	public ResultActions follow(String href, MediaType accept) throws Exception {
		return mvc.perform(get(href).header(HttpHeaders.ACCEPT, accept.toString()));
	}

	/**
	 * Discover list of URIs associated with a rel, starting at the root node ("/")
	 *
	 * @param rel
	 * @return
	 * @throws Exception
	 */
	public Links discover(LinkRelation rel) throws Exception {
		return discover(Link.of("/"), rel);
	}

	public Link discoverUnique(String rel) throws Exception {
		return discoverUnique(LinkRelation.of(rel));
	}

	/**
	 * Discover single URI associated with a rel, starting at the root node ("/")
	 *
	 * @param rel
	 * @return
	 * @throws Exception
	 */
	public Link discoverUnique(LinkRelation rel) throws Exception {

		Links discover = discover(rel);
		assertThat(discover).hasSize(1);
		return discover.toList().get(0);
	}

	public Link discoverUnique(String... rels) throws Exception {
		return discoverUnique(Arrays.stream(rels).map(LinkRelation::of).toArray(LinkRelation[]::new));
	}

	/**
	 * Traverses the given link relations from the root.
	 *
	 * @param rels
	 * @return
	 * @throws Exception
	 */
	public Link discoverUnique(LinkRelation... rels) throws Exception {

		Iterator<LinkRelation> toTraverse = Arrays.asList(rels).iterator();
		Link lastLink = null;

		while (toTraverse.hasNext()) {

			LinkRelation relation = toTraverse.next();

			lastLink = lastLink == null //
					? discoverUnique(relation) //
					: discoverUnique(lastLink, relation);
		}

		return lastLink;
	}

	/**
	 * Given a URI (root), discover the URIs for a given rel.
	 *
	 * @param root - URI to start from
	 * @param relation - name of the relationship to seek links
	 * @return list of {@link org.springframework.hateoas.Link Link} objects associated with the rel
	 * @throws Exception
	 */
	public Links discover(Link root, LinkRelation relation) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(root.expand().getHref()).accept(DEFAULT_MEDIA_TYPE)).//
				andExpect(status().isOk()).//
				andExpect(hasLinkWithRel(relation)).//
				andReturn().getResponse();

		String content = response.getContentAsString();
		return getDiscoverer(response).findLinksWithRel(relation, content);
	}

	public Link discoverUnique(Link root, String relation) throws Exception {
		return discoverUnique(root, LinkRelation.of(relation));
	}

	/**
	 * Given a URI (root), discover the unique URI for a given rel. NOTE: Assumes there is only one URI
	 *
	 * @param root
	 * @param relation
	 * @return {@link org.springframework.hateoas.Link Link} tied to a given rel
	 * @throws Exception
	 */
	public Link discoverUnique(Link root, LinkRelation relation) throws Exception {
		return discoverUnique(root, relation, DEFAULT_MEDIA_TYPE);
	}

	public Link discoverUnique(Link root, String rel, MediaType mediaType) throws Exception {
		return discoverUnique(root, LinkRelation.of(rel), mediaType);
	}

	/**
	 * Given a URI (root), discover the unique URI for a given rel. NOTE: Assumes there is only one URI
	 *
	 * @param root the link to the resource to access.
	 * @param rel the link relation to discover in the response.
	 * @param mediaType the {@link MediaType} to request.
	 * @return {@link org.springframework.hateoas.Link Link} tied to a given rel
	 * @throws Exception
	 */
	public Link discoverUnique(Link root, LinkRelation rel, MediaType mediaType) throws Exception {

		MockHttpServletResponse response = mvc.perform(get(root.expand().getHref())//
				.accept(mediaType)).andExpect(status().isOk())//
				.andExpect(hasLinkWithRel(rel))//
				.andReturn().getResponse();

		return assertHasLinkWithRel(rel, response);
	}

	public Link assertHasLinkWithRel(String relation, MockHttpServletResponse response) throws Exception {
		return assertHasLinkWithRel(LinkRelation.of(relation), response);
	}

	/**
	 * For a given servlet response, verify that the provided rel exists in its hypermedia. If so, return the URI link.
	 *
	 * @param relation
	 * @param response
	 * @return {@link org.springframework.hateoas.Link} of the rel found in the response
	 * @throws Exception
	 */
	public Link assertHasLinkWithRel(LinkRelation relation, MockHttpServletResponse response) throws Exception {

		String content = response.getContentAsString();
		Optional<Link> link = getDiscoverer(response).findLinkWithRel(relation, content);

		return link.orElseThrow(() -> new IllegalStateException(
				"Expected to find link with rel " + relation + " but found none in " + content));
	}

	public ResultMatcher hasLinkWithRel(String rel) {
		return hasLinkWithRel(LinkRelation.of(rel));
	}

	/**
	 * MockMvc matcher used to verify existence of rel with URI link
	 *
	 * @param rel
	 * @return
	 */
	public ResultMatcher hasLinkWithRel(LinkRelation rel) {

		return result -> {

			MockHttpServletResponse response = result.getResponse();
			String s = response.getContentAsString();

			assertThat(getDiscoverer(response).findLinkWithRel(rel, s)) //
					.as(() -> "Expected to find link with rel " + rel + " but found none in " + s) //
					.isNotNull();

		};
	}

	/**
	 * Using the servlet response's content type, find the corresponding link discoverer.
	 *
	 * @param response
	 * @return {@link org.springframework.hateoas.client.LinkDiscoverer}
	 */
	public LinkDiscoverer getDiscoverer(MockHttpServletResponse response) {

		String contentType = response.getContentType();

		return discoverers.getRequiredLinkDiscovererFor(contentType);
	}
}
