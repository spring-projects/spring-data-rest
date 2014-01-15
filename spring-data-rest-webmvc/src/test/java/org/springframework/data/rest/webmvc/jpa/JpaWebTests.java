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
package org.springframework.data.rest.webmvc.jpa;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;

import net.minidev.json.JSONArray;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriTemplate;

/**
 * Web integration tests specific to JPA.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@Transactional
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class JpaWebTests extends AbstractWebIntegrationTests {

	private static final MediaType TEXT_URI_LIST = MediaType.valueOf("text/uri-list");
	static final String LINK_TO_SIBLINGS_OF = "$._embedded..[?(@.firstName == '%s')]._links.siblings.href[0]";

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
		return Arrays.asList("people", "authors", "books");
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#getPayloadToPost()
	 */
	@Override
	protected Map<String, String> getPayloadToPost() throws Exception {
		return Collections.singletonMap("people", readFile("person.json"));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#getRootAndLinkedResources()
	 */
	@Override
	protected MultiValueMap<String, String> getRootAndLinkedResources() {

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("authors", "books");
		map.add("books", "authors");

		return map;
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

	/**
	 * @see DATAREST-199
	 */
	@Test
	public void createsOrderUsingPut() throws Exception {

		mvc.perform(//
				put("/orders/{id}", 4711).//
						content(readFile("order.json")).contentType(MediaType.APPLICATION_JSON)//
		).andExpect(status().isCreated());
	}

	@Test
	public void listsSiblingsWithContentCorrectly() throws Exception {

		MockHttpServletResponse response = mvc.perform(get("/people")).andReturn().getResponse();
		String href = assertHasJsonPathValue(String.format(LINK_TO_SIBLINGS_OF, "John"), response);

		mvc.perform(get(href)).andExpect(status().isOk());
	}

	@Test
	public void listsEmptySiblingsCorrectly() throws Exception {

		MockHttpServletResponse response = mvc.perform(get("/people")).andReturn().getResponse();
		String href = assertHasJsonPathValue(String.format(LINK_TO_SIBLINGS_OF, "Billy Bob"), response);

		mvc.perform(get(href)).andExpect(status().isOk());
	}


	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithMultiplePosts() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		String bilbo = mapper.writeValueAsString(new Person("Bilbo", "Baggins"));
		String frodo = mapper.writeValueAsString(new Person("Frodo", "Baggins"));
		String merry = mapper.writeValueAsString(new Person("Merry", "Baggins"));
		String pippin = mapper.writeValueAsString(new Person("Pippin", "Baggins"));

		Link peopleLink = discoverUnique("people");

		MockHttpServletResponse bilboResponse = postAndGet(peopleLink, bilbo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse frodoResponse = postAndGet(peopleLink, frodo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse merryResponse = postAndGet(peopleLink, merry, MediaType.APPLICATION_JSON);
		MockHttpServletResponse pippinResponse = postAndGet(peopleLink, pippin, MediaType.APPLICATION_JSON);

		Link bilboSelfLink = assertHasLinkWithRel(Link.REL_SELF, bilboResponse);
		Link merrySelfLink = assertHasLinkWithRel(Link.REL_SELF, merryResponse);
		Link pippinSelfLink = assertHasLinkWithRel(Link.REL_SELF, pippinResponse);
		Link frodosSiblingsLink = assertHasLinkWithRel("siblings", frodoResponse);

		postAndGet(frodosSiblingsLink, bilboSelfLink.getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, merrySelfLink.getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, pippinSelfLink.getHref(), TEXT_URI_LIST);

		MockHttpServletResponse frodosLatestSiblings = request(frodosSiblingsLink);
		String[] persons = ((JSONArray) JsonPath.read(frodosLatestSiblings.getContentAsString(), "$._embedded.persons[*].firstName")).toArray(new String[]{});
		assertThat(persons.length, equalTo(3));
		assertThat(Arrays.asList(persons), hasItems("Bilbo", "Merry", "Pippin"));
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePost() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		String bilbo = mapper.writeValueAsString(new Person("Bilbo", "Baggins"));
		String frodo = mapper.writeValueAsString(new Person("Frodo", "Baggins"));
		String merry = mapper.writeValueAsString(new Person("Merry", "Baggins"));
		String pippin = mapper.writeValueAsString(new Person("Pippin", "Baggins"));

		Link peopleLink = discoverUnique("people");

		MockHttpServletResponse bilboResponse = postAndGet(peopleLink, bilbo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse frodoResponse = postAndGet(peopleLink, frodo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse merryResponse = postAndGet(peopleLink, merry, MediaType.APPLICATION_JSON);
		MockHttpServletResponse pippinResponse = postAndGet(peopleLink, pippin, MediaType.APPLICATION_JSON);

		final Link bilboSelfLink = assertHasLinkWithRel(Link.REL_SELF, bilboResponse);
		final Link merrySelfLink = assertHasLinkWithRel(Link.REL_SELF, merryResponse);
		final Link pippinSelfLink = assertHasLinkWithRel(Link.REL_SELF, pippinResponse);
		Link frodosSiblingsLink = assertHasLinkWithRel("siblings", frodoResponse);

		postAndGet(frodosSiblingsLink,
				StringUtils.arrayToDelimitedString(new Object[]{bilboSelfLink.getHref(),
						merrySelfLink.getHref(), pippinSelfLink.getHref()}, "\n"),
				TEXT_URI_LIST);

		MockHttpServletResponse frodosLatestSiblings = request(frodosSiblingsLink);
		String[] persons = ((JSONArray) JsonPath.read(frodosLatestSiblings.getContentAsString(), "$._embedded.persons[*].firstName")).toArray(new String[]{});
		assertThat(persons.length, equalTo(3));
		assertThat(Arrays.asList(persons), hasItems("Bilbo", "Merry", "Pippin"));
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithMultiplePuts() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		String bilbo = mapper.writeValueAsString(new Person("Bilbo", "Baggins"));
		String frodo = mapper.writeValueAsString(new Person("Frodo", "Baggins"));
		String merry = mapper.writeValueAsString(new Person("Merry", "Baggins"));
		String pippin = mapper.writeValueAsString(new Person("Pippin", "Baggins"));

		Link peopleLink = discoverUnique("people");

		MockHttpServletResponse bilboResponse = postAndGet(peopleLink, bilbo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse frodoResponse = postAndGet(peopleLink, frodo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse merryResponse = postAndGet(peopleLink, merry, MediaType.APPLICATION_JSON);
		MockHttpServletResponse pippinResponse = postAndGet(peopleLink, pippin, MediaType.APPLICATION_JSON);

		Link bilboSelfLink = assertHasLinkWithRel(Link.REL_SELF, bilboResponse);
		Link merrySelfLink = assertHasLinkWithRel(Link.REL_SELF, merryResponse);
		Link pippinSelfLink = assertHasLinkWithRel(Link.REL_SELF, pippinResponse);
		Link frodosSiblingsLink = assertHasLinkWithRel("siblings", frodoResponse);

		putAndGet(frodosSiblingsLink, bilboSelfLink.getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, merrySelfLink.getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, pippinSelfLink.getHref(), TEXT_URI_LIST);

		MockHttpServletResponse frodosLatestSiblings = request(frodosSiblingsLink);
		String firstName = JsonPath.read(frodosLatestSiblings.getContentAsString(), "$._embedded.person.firstName");
		assertThat(firstName, equalTo("Pippin"));

		postAndGet(frodosSiblingsLink, merrySelfLink.getHref(), TEXT_URI_LIST);

		frodosLatestSiblings = request(frodosSiblingsLink);
		String[] persons = ((JSONArray) JsonPath.read(frodosLatestSiblings.getContentAsString(),
				"$._embedded.persons[*].firstName")).toArray(new String[]{});
		assertThat(persons.length, equalTo(2));
		assertThat(Arrays.asList(persons), hasItems("Merry", "Pippin"));
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePut() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		String bilbo = mapper.writeValueAsString(new Person("Bilbo", "Baggins"));
		String frodo = mapper.writeValueAsString(new Person("Frodo", "Baggins"));
		String merry = mapper.writeValueAsString(new Person("Merry", "Baggins"));
		String pippin = mapper.writeValueAsString(new Person("Pippin", "Baggins"));

		Link peopleLink = discoverUnique("people");

		MockHttpServletResponse bilboResponse = postAndGet(peopleLink, bilbo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse frodoResponse = postAndGet(peopleLink, frodo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse merryResponse = postAndGet(peopleLink, merry, MediaType.APPLICATION_JSON);
		MockHttpServletResponse pippinResponse = postAndGet(peopleLink, pippin, MediaType.APPLICATION_JSON);

		Link bilboSelfLink = assertHasLinkWithRel(Link.REL_SELF, bilboResponse);
		Link merrySelfLink = assertHasLinkWithRel(Link.REL_SELF, merryResponse);
		Link pippinSelfLink = assertHasLinkWithRel(Link.REL_SELF, pippinResponse);
		Link frodosSiblingsLink = assertHasLinkWithRel("siblings", frodoResponse);

		putAndGet(frodosSiblingsLink,
				StringUtils.arrayToDelimitedString(new Object[]{bilboSelfLink.getHref(),
						merrySelfLink.getHref()}, "\n"),
				TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, pippinSelfLink.getHref(), TEXT_URI_LIST);

		MockHttpServletResponse frodosLatestSiblings = request(frodosSiblingsLink);
		String firstName = JsonPath.read(frodosLatestSiblings.getContentAsString(), "$._embedded.person.firstName");
		assertThat(firstName, equalTo("Pippin"));

		postAndGet(frodosSiblingsLink, merrySelfLink.getHref(), TEXT_URI_LIST);

		frodosLatestSiblings = request(frodosSiblingsLink);
		String[] persons = ((JSONArray) JsonPath.read(frodosLatestSiblings.getContentAsString(),
				"$._embedded.persons[*].firstName")).toArray(new String[]{});
		assertThat(persons.length, equalTo(2));
		assertThat(Arrays.asList(persons), hasItems("Merry", "Pippin"));
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithDelete() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		String bilbo = mapper.writeValueAsString(new Person("Bilbo", "Baggins"));
		String frodo = mapper.writeValueAsString(new Person("Frodo", "Baggins"));
		String merry = mapper.writeValueAsString(new Person("Merry", "Baggins"));
		String pippin = mapper.writeValueAsString(new Person("Pippin", "Baggins"));

		Link peopleLink = discoverUnique("people");

		MockHttpServletResponse bilboResponse = postAndGet(peopleLink, bilbo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse frodoResponse = postAndGet(peopleLink, frodo, MediaType.APPLICATION_JSON);
		MockHttpServletResponse merryResponse = postAndGet(peopleLink, merry, MediaType.APPLICATION_JSON);
		MockHttpServletResponse pippinResponse = postAndGet(peopleLink, pippin, MediaType.APPLICATION_JSON);

		Link bilboSelfLink = assertHasLinkWithRel(Link.REL_SELF, bilboResponse);
		Link merrySelfLink = assertHasLinkWithRel(Link.REL_SELF, merryResponse);
		Link pippinSelfLink = assertHasLinkWithRel(Link.REL_SELF, pippinResponse);
		Link frodosSiblingsLink = assertHasLinkWithRel("siblings", frodoResponse);

		postAndGet(frodosSiblingsLink, bilboSelfLink.getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, merrySelfLink.getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, pippinSelfLink.getHref(), TEXT_URI_LIST);

		String pippinId = new UriTemplate("/people/{id}").match(pippinSelfLink.getHref()).get("id");
		deleteAndGet(new Link(frodosSiblingsLink.getHref() + "/" + pippinId), TEXT_URI_LIST);

		MockHttpServletResponse frodosLatestSiblings = request(frodosSiblingsLink);
		String[] persons = ((JSONArray) JsonPath.read(frodosLatestSiblings.getContentAsString(), "$._embedded.persons[*].firstName")).toArray(new String[]{});
		assertThat(persons.length, equalTo(2));
		assertThat(Arrays.asList(persons), hasItems("Bilbo", "Merry"));
	}


	private String readFile(String name) throws Exception {

		ClassPathResource file = new ClassPathResource(name, getClass());
		StringBuilder builder = new StringBuilder();
		Scanner scanner = new Scanner(file.getFile(), "UTF-8");

		while (scanner.hasNextLine()) {
			builder.append(scanner.nextLine());
		}

		return builder.toString();
	}
}
