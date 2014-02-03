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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

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

	ObjectMapper mapper = new ObjectMapper();

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

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingLink = links.get(0);

		postAndGet(frodosSiblingLink, links.get(1).getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingLink, links.get(2).getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingLink, links.get(3).getHref(), TEXT_URI_LIST);

		assertSiblingNames(frodosSiblingLink, "Bilbo", "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePost() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingLink = links.get(0);

		postAndGet(frodosSiblingLink, toUriList(links.get(1), links.get(2), links.get(3)), TEXT_URI_LIST);

		assertSiblingNames(frodosSiblingLink, "Bilbo", "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithMultiplePuts() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingsLink = links.get(0);

		putAndGet(frodosSiblingsLink, links.get(1).getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, links.get(2).getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, links.get(3).getHref(), TEXT_URI_LIST);
		assertSiblingNames(frodosSiblingsLink, "Pippin");

		postAndGet(frodosSiblingsLink, links.get(2).getHref(), TEXT_URI_LIST);
		assertSiblingNames(frodosSiblingsLink, "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePut() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodoSiblingLink = links.get(0);

		putAndGet(frodoSiblingLink, toUriList(links.get(1), links.get(2), links.get(3)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Bilbo", "Merry", "Pippin");

		putAndGet(frodoSiblingLink, toUriList(links.get(3)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Pippin");

		postAndGet(frodoSiblingLink, toUriList(links.get(2)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithDelete() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingsLink = links.get(0);

		postAndGet(frodosSiblingsLink, links.get(1).getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, links.get(2).getHref(), TEXT_URI_LIST);
		postAndGet(frodosSiblingsLink, links.get(3).getHref(), TEXT_URI_LIST);

		String pippinId = new UriTemplate("/people/{id}").match(links.get(3).getHref()).get("id");
		deleteAndGet(new Link(frodosSiblingsLink.getHref() + "/" + pippinId), TEXT_URI_LIST);

		assertSiblingNames(frodosSiblingsLink, "Bilbo", "Merry");
	}

	/**
	 * @see DATAREST-50
	 */
	@Test
	public void propertiesCanHaveNulls() throws Exception {

		Link peopleLink = discoverUnique("people");

		Person frodo = new Person();
		frodo.setFirstName("Frodo");
		frodo.setLastName(null);

		MockHttpServletResponse response = postAndGet(peopleLink, mapper.writeValueAsString(frodo),
				MediaType.APPLICATION_JSON);
		String responseBody = response.getContentAsString();

		assertEquals(JsonPath.read(responseBody, "$.firstName"), "Frodo");
		assertNull(JsonPath.read(responseBody, "$.lastName"));
	}

	/**
	 * @see DATAREST-238
	 */
	@Test
	public void putShouldWorkDespiteExistingLinks() throws Exception {

		Link peopleLink = discoverUnique("people");

		Person frodo = new Person("Frodo", "Baggins");
		String frodoString = mapper.writeValueAsString(frodo);

		MockHttpServletResponse createdPerson = postAndGet(peopleLink, frodoString, MediaType.APPLICATION_JSON);

		Link frodoLink = assertHasLinkWithRel("self", createdPerson);
		assertJsonPathEquals("$.firstName", createdPerson, "Frodo");

		String bilboWithFrodosLinks = createdPerson.getContentAsString().replace("Frodo", "Bilbo");

		MockHttpServletResponse overwrittenResponse = putAndGet(frodoLink, bilboWithFrodosLinks, MediaType.APPLICATION_JSON);

		assertHasLinkWithRel("self", overwrittenResponse);
		assertJsonPathEquals("$.firstName", overwrittenResponse, "Bilbo");
	}

	private List<Link> preparePersonResources(Person primary, Person... persons) throws Exception {

		Link peopleLink = discoverUnique("people");
		List<Link> links = new ArrayList<Link>();

		MockHttpServletResponse primaryResponse = postAndGet(peopleLink, mapper.writeValueAsString(primary),
				MediaType.APPLICATION_JSON);
		links.add(assertHasLinkWithRel("siblings", primaryResponse));

		for (Person person : persons) {

			String payload = mapper.writeValueAsString(person);
			MockHttpServletResponse response = postAndGet(peopleLink, payload, MediaType.APPLICATION_JSON);

			links.add(assertHasLinkWithRel(Link.REL_SELF, response));
		}

		return links;
	}

	/**
	 * Asserts the {@link Person} resource the given link points to contains siblings with the given names.
	 * 
	 * @param link
	 * @param siblingNames
	 * @throws Exception
	 */
	private void assertSiblingNames(Link link, String... siblingNames) throws Exception {

		String responseBody = request(link).getContentAsString();
		List<String> persons = JsonPath.read(responseBody, "$._embedded.persons[*].firstName");

		assertThat(persons, hasSize(siblingNames.length));
		assertThat(persons, hasItems(siblingNames));
	}

	private static String readFile(String name) throws Exception {

		ClassPathResource file = new ClassPathResource(name, JpaWebTests.class);
		StringBuilder builder = new StringBuilder();

		Scanner scanner = new Scanner(file.getFile(), "UTF-8");

		try {

			while (scanner.hasNextLine()) {
				builder.append(scanner.nextLine());
			}

		} finally {
			scanner.close();
		}

		return builder.toString();
	}

	private static String toUriList(Link... links) {

		List<String> uris = new ArrayList<String>(links.length);

		for (Link link : links) {
			uris.add(link.expand().getHref());
		}

		return StringUtils.collectionToDelimitedString(uris, "\n");
	}
}
